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
        "files; one for records matching given chromosome subsets (target),"+
        " and one for all other records (excluded). Original headers are "+
        "preserved, with additional @PG entry.";
   
    @Option(shortName=StandardOptionDefinitions.INPUT_SHORT_NAME, 
            doc="Input SAM or BAM file")
    public File INPUT;
    
    @Option(shortName="S", doc="@SQ reference sequence values (eg. "+
            "chromosomes) in BAM header to target. Option may be used "+
            "multiple times to select multiple chromosomes. Default subset: "+
            "Y, MT.", optional=true)
    public ArrayList<String> SUBSET;

    @Option(shortName="T", doc="Output SAM/BAM path for target sequences.")
        public String TARGET_PATH;

    @Option(shortName="X", doc="Output SAM/BAM path for excluded sequences.")
        public String EXCLUDED_PATH;

    @Option(shortName="U", doc="Exclude read groups in which all reads are unaligned. (Groups with at least one read aligned to the target, and others unaligned, will be written to the target file.)", optional=true)
        public boolean EXCLUDE_UNALIGNED = false;

    @Option(shortName="V", doc="Treat the S option as a list to EXCLUDE rather than TARGET, so that chimeric/unmapped read pairs remain excluded.  If S option is not provided, this option is set back to false to allow the default to continue to work)", optional=true)
        public boolean INVERT_TARGET = false;
    
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
        final HashMap<Integer, SAMFileWriter> writers = getWriters(header);
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

    private HashMap<Integer, SAMFileWriter> getWriters(SAMFileHeader header) {
        /* 
         * Get map from subset indices to SAMFileWriter objects, for output
         * Updates SAM/BAM headers
         * Also generates filenames using namePrefix
         */
        HashMap<Integer, SAMFileWriter> writers = 
            new HashMap<Integer, SAMFileWriter>();
        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        String[] paths = {EXCLUDED_PATH, TARGET_PATH};
        int[] indices = {EXCLUDED_INDEX, TARGET_INDEX};
        for (int i=0; i<2; i++) {
            // generate updated SAM/BAM header
            SAMFileHeader newHeader = header.clone();
            SAMProgramRecord rec = getThisProgramRecord(programName, programDS);
            String outputType;
            if (indices[i] == TARGET_INDEX) {
                outputType = "TARGET";
            } else {
                outputType = "EXCLUDED";
            }
            rec.setAttribute("OT", outputType);
            newHeader.addProgramRecord(rec);
            // create output writer with new header and appropriate path
            File outputFile = new File(paths[i]);
            IoUtil.assertFileIsWritable(outputFile);
            SAMFileWriter out = 
                factory.makeSAMOrBAMWriter(newHeader, false, outputFile);
            writers.put(indices[i], out);
        }
        return writers;
    }

    private void readWriteRecords(SAMFileReader in, 
                                  HashMap<Integer, SAMFileWriter> writers) {
        /* iterate over records from input, write to appropriate output
           assume paired reads are adjacent and have same QNAME/read_name
         */
        String lastq = null;
        ArrayList<SAMRecord> groupOfReads = new ArrayList<SAMRecord>();
        for (SAMRecord rec: in){
            String qname = rec.getReadName();
            if (!(qname.equals(lastq)) && (lastq != null)) {
                // start of new read group; write previous group to file
                writeGroup(groupOfReads, writers);
                groupOfReads.clear();
            }
            groupOfReads.add(rec);
            lastq = qname;
        }
        writeGroup(groupOfReads, writers); // write final read group
    }

    private void writeGroup(ArrayList<SAMRecord> groupOfReads,
                            HashMap<Integer, SAMFileWriter> writers) {
        /*
         * Write a group of reads from SAM/BAM file
         * Reads have same query name, but may align to different chromosomes
         * If any read aligns to a chromosome not in subset, entire group is 
         * written to excluded file
         *
         * Unaligned reads go into target file, unless EXCLUDE_UNALIGNED==true
         *
         * Underlying principles:
         * 1. Paired reads go to the same file, AND
         * 2. Reads which align to "unconsented" references go to excluded file
         */
        int destination = TARGET_INDEX;
                
        boolean unaligned = true; // are all reads unaligned?
        for (SAMRecord rec: groupOfReads) {
            // first pass -- check for reads not in SUBSET
            if  (!(rec.getReadUnmappedFlag())) {
                if (( INVERT_TARGET) ^
                    (!(SUBSET.contains(rec.getReferenceName()))))
                {
                    destination = EXCLUDED_INDEX;
                    break;
                } else {
                    unaligned = false;
                }
            } else { // one or both reads are unmapped
                if (EXCLUDE_UNALIGNED) {
                    // FixMate has reset * Y or Y * to Y Y
                    destination = EXCLUDED_INDEX;
                }
            }
        }
        if (unaligned && EXCLUDE_UNALIGNED && destination==TARGET_INDEX) {
            destination = EXCLUDED_INDEX;
        }
        
        for (SAMRecord rec: groupOfReads) {
            // second pass -- write all reads to appropriate file
            writers.get(destination).addAlignment(rec);
        }
    }
    
    public static void main(final String[] args) {        
        System.exit(new SplitBamByChromosomes().instanceMain(args));
    } 
      
}
