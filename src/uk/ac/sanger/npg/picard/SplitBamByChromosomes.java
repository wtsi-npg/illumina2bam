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
 * Class to to split a BAM file based on chromosome, and output one BAM file for each chromosome subset.  More precisely:  Define subsets of the @SQ reference sequences in the BAM header.  The RNAME for each alignment record should refer to a member of @SQ.  Output each alignment record to the appropriate file for its subset.
 * Specify chromosome subsets on the command line.  Members of a subset are separated by commas; subsets are separated by colons.  Example:   1,3,5,7:Y,MT
 * Members of @SQ which are not in any of the subsets defined on the command line will be directed to a "default" output file.  Output one file for each subset specified on the command line, *including* empty subsets.
 * Appends a @PG entry to the BAM header to record use of this class, and chromosome subsets used for split.
 */

/*

TODO:  Option of sending output to a FIFO instead of a file?
  
 */


public class SplitBamByChromosomes extends PicardCommandLine {

    private final Log log = Log.getInstance(SplitBamByChromosomes.class);

    private final String programName = "SplitBamByChromosomes";

    private final String programDS = "Split a BAM (or SAM) file into multiple files, "+
	"based on chromosome subsets. Original headers are preserved, with additional @PG entry.";
   
    @Option(shortName=StandardOptionDefinitions.INPUT_SHORT_NAME, doc="Input SAM or BAM file")
	public File INPUT;
    
    @Option(shortName="O", doc="Prefix for output SAM/BAM filenames.")
	public String OUTPUT_PREFIX;

    @Option(shortName="S", doc="Subsets of @SQ values (eg. chromosomes) in BAM header to split away. Members of a subset are separated by commas; subsets are separated by colons (example: 1,3,5:X,Y). @SQ values not specified in the subset string are written to their own file. Default split: Y,MT", optional=true)
	public String SUBSETS;

    @Usage(programVersion= version)
	public final String USAGE = getStandardUsagePreamble() + programDS + " "; 

    @Override protected int doWork() {
	/*
	 * 'Main' method inherited from net.sf.picard.cmdline.CommandLineProgram
	 */
	log.info("Starting SplitBamByChromosomes");
	if (SUBSETS == null) {
	    SUBSETS = "Y,MT"; // default if not specified on command line
	}
	log.info("Parsing subset argument");
	HashMap<String, Integer> subsetMap = parseSubsetArg(SUBSETS); 

        log.info("Checking input file");
        IoUtil.assertFileIsReadable(INPUT);
        final SAMFileReader in = new SAMFileReader(INPUT);
        final SAMFileHeader header = in.getFileHeader();
	subsetMap = updateSubsetMap(subsetMap, header.getSequenceDictionary());

	log.info("Opening output files");
	final HashMap<Integer, SAMFileWriter> writers = getWriters(subsetMap, header, OUTPUT_PREFIX, 
								   in.isBinary());

	log.info("Writing output split by @SQ subset");
	for (SAMRecord rec: in){
	    String rName = rec.getReferenceName();
	    Integer index = 0;
	    if (!(rName.equals("*"))) { index = subsetMap.get(rName); }
	    if (index == null) {
		log.warn("Reference name "+rName+" does not appear in header @SQ fields");
		index = 0;
	    }
	    SAMFileWriter out = writers.get(index);
	    if ( out == null ) {
		String msg = "SAM output writer not found for subset index "+index.toString();
		log.error(msg);
		throw new RuntimeException(msg);
	    }
	    writers.get(index).addAlignment(rec);
	}
	for (SAMFileWriter writer: writers.values()) {
	    writer.close();
	}
	log.info("Output written to files");
	return 0;
    }

    private HashMap<String, Integer> updateSubsetMap(HashMap<String, Integer> subsetMap, 
						     SAMSequenceDictionary seqDict) throws RuntimeException {
	/*
	 * Cross-reference mapping from parseSubsetArg() with @SQ dictionary from SAM header
	 * create a mapping from items in @SQ dictionary to subset index
	 * sequences not specified in command-line argument are mapped to subset 0
	 */
	if (seqDict.isEmpty()) { // eg. testdata/bam/6210_8.sam
	    String msg = "SAM header @SQ dictionary is empty.";
	    log.warn(msg); 
	} else {
	    Integer seqSize = seqDict.size();
	    log.info(seqSize.toString()+" items found in SAM header @SQ dictionary.");
	}
	// check for input subsets which do not correspond to anything in @SQ dictionary; warn if found
	for (String inputSubset : subsetMap.keySet() ) {
	    Boolean empty = true;
	    for (SAMSequenceRecord rec : seqDict.getSequences()) {
		if (rec.getSequenceName().equals(inputSubset)) {
		    empty = false;
		    break;
		}
	    }
	    if (empty) {
		String msg = "Subset "+inputSubset+" from input argument does not match any item in @SQ dictionary."; 
		log.warn(msg);
	    }
	}
	// check items in @SQ dictionary; if any do not have subsets, assign to (default) subset 0
	Boolean empty = true;
	for (SAMSequenceRecord rec : seqDict.getSequences()) {
	    String name = rec.getSequenceName();
	    if (!(subsetMap.containsKey(name))) {
		subsetMap.put(name, 0);
	    } else {
		empty = false;
	    }
	}
	if (empty) {
	    log.warn("No valid subsets supplied for @SQ values; all sequences will be output to default file.");
	}
	return subsetMap;
    }

    private SAMFileHeader getUpdatedSamHeader(SAMFileHeader inputHeader, HashMap<String, Integer> subsetMap, 
					      Integer index) {
	/*
	 * Update SAM/BAM file header; add a new @PG program record for this class
	 */
	SAMFileHeader header = inputHeader.clone();
	SAMProgramRecord rec = getThisProgramRecord(programName, programDS);  // method from PicardCommandLine
	// now set @RS attribute, indicating the reference subset being used
	ArrayList<String> refs = new ArrayList<String>();
	for (String ref: subsetMap.keySet()) {
	    if (subsetMap.get(ref) == index) {
		refs.add(ref);
	    }
	}
	Collections.sort(refs);
	StringBuilder sb = new StringBuilder("REFERENCE_SUBSET");
	String sep = ",";
	for (String ref: refs) {
	    sb.append(sep);
	    sb.append(ref);
	}
	rec.setAttribute("RS", sb.toString());
	rec.setAttribute("SA", "SPLIT_ARG "+SUBSETS); // record string used to define split
	String isDefault = "N";
	if (index == 0) { isDefault = "Y"; }
	rec.setAttribute("DF", "IS_DEFAULT:"+isDefault); // is this file the default output?
	header.addProgramRecord(rec);
	return header;
    }

    private HashMap<Integer, SAMFileWriter> getWriters(HashMap<String, Integer> subsetMap, 
						       SAMFileHeader headerBase, String namePrefix,
						       boolean isBinary) {
	/* 
	 * Generate a mapping from subset indices to SAMFileWriter objects, for output
	 * Also generates filenames using namePrefix
	 */
	HashSet<Integer> valueSet = new HashSet<Integer>(subsetMap.values());
	Integer valueTotal = valueSet.size();
	log.info(valueTotal.toString()+" @SQ subsets found.");
	HashMap<Integer, SAMFileWriter> writers = new HashMap<Integer, SAMFileWriter>();
	SAMFileWriterFactory factory = new SAMFileWriterFactory();
	for (Integer i: valueSet) {
	    String fileName = namePrefix+"_"+String.format("%03d", i);
	    if (isBinary) { fileName = fileName + ".bam"; }
	    else { fileName = fileName + ".sam"; }
            File outputFile = new File(fileName);
            IoUtil.assertFileIsWritable( outputFile );
	    SAMFileHeader newHeader = getUpdatedSamHeader(headerBase, subsetMap, i);
	    SAMFileWriter out = factory.makeSAMOrBAMWriter(newHeader, false, outputFile);
	    writers.put(i, out);
	}
	return writers;
    }

    private HashMap<String, Integer> parseSubsetArg(String subsetArg) throws RuntimeException {
	/* 
	 * parse command-line argument which specifies subsets of @SQ values (with basic sanity checking)
	 * assign an integer to each subset; return a mapping from @SQ value to subset number
	 * 'zeroth' subset is always the default (for @SQ not specified on the command line, if any)
	 * doWork() in base class net.sf.picard.cmdline.CommandLineProgram throws RuntimeException
	 */
	HashMap<String, Integer> subsetMap = new HashMap<String, Integer>();
	String[] subsets = subsetArg.split(":");
	Integer i = 1; // index 0 is for @SQ values not in subset argument, so counting starts from 1
	for (String subset : subsets) {
	    String[] members = subset.split(",");
	    for (String member : members) {
		boolean error = false;
		String msg = "";
		if (member.length() == 0) {
		    msg = "Subset parsing error: Subset name of zero length found";
		    error = true;
		}
		if (subsetMap.containsKey(member)) {
		    msg = "Subset parsing error: Same @SQ value in multiple subsets";
		    error = true;
		}
		if (error) {
		    log.error(msg);
		    throw new RuntimeException(msg);
		}
		subsetMap.put(member, i);
	    }
	    i++;
	}
	return subsetMap;
    }
    
    public static void main(final String[] args) {        
        System.exit(new SplitBamByChromosomes().instanceMain(args));
    } 
      
}
