/*
 * Copyright (C) 2012 GRL
 *
 * This library is free software. You can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */
package uk.ac.sanger.npg.picard;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.*;

/**
 * @author ib5@sanger.ac.uk
 * Class to to split a BAM file based on given chromosomes.  More precisely:
 *  Define subsets of the @SQ reference sequences in the BAM header by their SN fields.
 *  Filter pairs of reads based on alignment records referring to a member of this @SQ subset.
 *  Specify chromosome subset on the command line or use the default of MT and Y
 *  Send pairs of reads for which either read aligns to a chromosome/@SQ not specified in the given set to an "excluded" output.
 *  Send other pairs, i.e. both unaligned, both aligned to the given subset, or one aligned to the given subset and the other unaligned to a "target" output.
 *  Also cope with unpaired read data.
 * Rationale: we have human samples for which we only have consent to analyse Mitochondrial and Y data
 */

/*
TODO:  Option of sending output to a FIFO instead of a file?
 */

public class SplitBamByChromosomes extends PicardCommandLine {

    private final Log log = Log.getInstance(SplitBamByChromosomes.class);

    private final String programName = "SplitBamByChromosomes";

    private final String programDS = "Split a BAM (or SAM) file into two "+
	    "files; one for records matching given chromosome subsets, and one "+
	    "for all other records. Original headers are preserved, with "+
	    "additional @PG entry.";
   
    @Option(shortName=StandardOptionDefinitions.INPUT_SHORT_NAME, 
            doc="Input SAM or BAM file")
    public File INPUT;
    
    @Option(shortName="O", doc="Prefix for output SAM/BAM filenames.")
        public String OUTPUT_PREFIX;

    @Option(shortName="S", doc="@SQ reference sequence values (eg. "+
            "chromosomes) in BAM header to split away. Option may be used "+
            "multiple times to select multiple chromosomes. Default subset: "+
            "Y, MT.", optional=true)
    public ArrayList<String> SUBSET;

    @Usage(programVersion= version)
	    public final String USAGE = getStandardUsagePreamble()+programDS + " "; 
	private final int EXCLUDED_INDEX = 0;
	private final int TARGET_INDEX = 1;

    @Override protected int doWork() {
	    /*
	     * 'Main' method inherited from net.sf.picard.cmdline.CommandLineProgram
	     */
	    log.info("Starting SplitBamByChromosomes");
	    if (SUBSET.isEmpty()) { // default if not specified on command line
		    SUBSET.add("Y");
		    SUBSET.add("MT"); 
	    }
	    for (String member: SUBSET) {
		    log.info("Subset contains: "+member);
	    }
        log.info("Checking input file");
        IoUtil.assertFileIsReadable(INPUT);
        final SAMFileReader in = new SAMFileReader(INPUT);
        final SAMFileHeader header = in.getFileHeader();
        checkSequenceDictionary(header.getSequenceDictionary());
        log.info("Opening output files");
        final HashMap<Integer, SAMFileWriter> writers = 
	        getWriters(header, OUTPUT_PREFIX, in.isBinary());
        log.info("Writing output split by @SQ subset");
        readWriteRecords(in, writers);
        for (SAMFileWriter writer : writers.values()) {
	        writer.close();
        }
        return 0;
    }

	private void checkSequenceDictionary(SAMSequenceDictionary seqDict) {
		/* basic sanity checks on @SQ dictionary from SAM header
		 * write warnings/info to log
		 */
		if (seqDict.isEmpty()) { // eg. testdata/bam/6210_8.sam
			String msg = "SAM header @SQ dictionary is empty.";
			log.warn(msg); 
		} else {
			Integer seqSize = seqDict.size();
			log.info(seqSize.toString()+" items read from SAM header @SQ.");
		}
		// check for input subsets not in @SQ dictionary; warn if found
		for (String member : SUBSET) {
			Boolean empty = true;
			for (SAMSequenceRecord rec : seqDict.getSequences()) {
				if (rec.getSequenceName().equals(member)) {
					empty = false;
					break;
				}
			}
			if (empty) {
				String msg = "Subset member "+member+" from input argument "+
					"does not match any item in @SQ dictionary."; 
				log.warn(msg);
			}
		}
	}

    private SAMFileHeader 
	    getUpdatedSamHeader(SAMFileHeader inputHeader, Integer index) {
	    /*
	     * Update SAM/BAM file header; add a new @PG program record
	     */
	    SAMFileHeader header = inputHeader.clone();
	    SAMProgramRecord rec = getThisProgramRecord(programName, programDS);
	    // getThisProgramRecord() is from PicardCommandLine
	    String isTarget = "IS_TARGET:";
	    if (index.equals(TARGET_INDEX)) {
		    isTarget = isTarget+"Y";
	    } else {
		    isTarget = isTarget+"N";
	    }
	    rec.setAttribute("IT", isTarget);
	    header.addProgramRecord(rec);
	    return header;
    }

    private HashMap<Integer, SAMFileWriter>
	    getWriters(SAMFileHeader headerBase, String namePrefix,
	               boolean isBinary) {
	    /* 
	     * Get map from subset indices to SAMFileWriter objects, for output
	     * Updates SAM/BAM headers
	     * Also generates filenames using namePrefix
	     */
	    HashMap<Integer, SAMFileWriter> writers = 
		    new HashMap<Integer, SAMFileWriter>();
	    SAMFileWriterFactory factory = new SAMFileWriterFactory();
	    int[] outputIndices = {EXCLUDED_INDEX, TARGET_INDEX};
	    for (Integer i: outputIndices) {
		    String fileName = new String();
		    if (i == EXCLUDED_INDEX) {
			    fileName = namePrefix+"_excluded";
		    } else if (i == TARGET_INDEX) {
			    fileName = namePrefix+"_target";
		    } 
		    if (isBinary) { fileName = fileName + ".bam"; }
		    else { fileName = fileName + ".sam"; }
		    File outputFile = new File(fileName);
            IoUtil.assertFileIsWritable( outputFile );
            SAMFileHeader newHeader = getUpdatedSamHeader(headerBase, i);
            SAMFileWriter out = 
	            factory.makeSAMOrBAMWriter(newHeader, false, outputFile);
            writers.put(i, out);
	    }
	    return writers;
    }

	private void readWriteRecords(SAMFileReader in, 
	                              HashMap<Integer, SAMFileWriter> writers) {
		/* iterate over records from input, write to appropriate output
		   assume paired reads are adjacent and have same QNAME/read_name
		 */
		String lastq = null;
		ArrayList<SAMRecord> readGroup = new ArrayList<SAMRecord>();
		for (SAMRecord rec: in){
			String qname = rec.getReadName();
			if (!(qname.equals(lastq)) && (lastq != null)) {
				// start of new read group; write previous group to file
				writeGroup(readGroup, writers);
				readGroup.clear();
			}
			readGroup.add(rec);
			lastq = qname;
		}
		writeGroup(readGroup, writers); // write final read group
	}

	private void writeGroup(ArrayList<SAMRecord> readGroup,
	                        HashMap<Integer, SAMFileWriter> writers) {
		/*
		 * Write a group of reads from SAM/BAM file
		 * Reads have same query name, but may align to different chromosomes
		 * If *any* alignment is *not* in SUBSET, write group to default file
		 * EXCEPTION: Unaligned reads paired with SUBSET reads go to SUBSET file
		 * Excluded file corresponds to "unconsented data"
		 * Underlying principles:
		 * 1. Paired reads go to the same file, AND
		 * 2. Reads which align to "unconsented" references go to excluded file
		 */
		int destination = TARGET_INDEX;
		for (SAMRecord rec: readGroup) {
			// first pass -- find destination index for all reads
			String rname = rec.getReferenceName();
			if (!(SUBSET.contains(rname)) && !(rname.equals("*"))) {
				destination = EXCLUDED_INDEX;
				break;
			}
		}
		for (SAMRecord rec: readGroup) {
			// second pass -- write all reads to appropriate file
			writers.get(destination).addAlignment(rec);
		}
	}

    public static void main(final String[] args) {        
        System.exit(new SplitBamByChromosomes().instanceMain(args));
    } 
      
}
