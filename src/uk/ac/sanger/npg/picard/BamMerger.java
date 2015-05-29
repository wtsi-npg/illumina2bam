/*
 * Copyright (C) 2011 GRL
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
 */

package uk.ac.sanger.npg.picard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.*;


/**
 * A command-line tool to merge BAM/SAM alignment info in a bam
 * with the data in an unmapped BAM file,
 * producing a third BAM file that has alignment data and all the additional data from the unmapped BAM.
 * 
 * All the fields in each read will be added if not already in alignment.
 * 
 * The failed quality check flag in unmapped reads, and paired, first and second read flags will be added to alignment.
 * 
 * Two bam files must be in the same order but unmapped bam may have more records than the aligned bam.
 * There is an option to add these extra reads into final bam.
 * 
 * Only SQ records and alignment PG in the aligned bam file will be added to the output by default.
 * Default alignment PG ID is bwa, you need specify this PG ID from command line if not.
 * 
 * There is an option you can merge all other PGs in aligned bam file into the output.
 * 
 * All header information in the unmapped bam header will be kept, except SQ records.
 * 
 * @author gq1@sanger.ac.uk
 */

public class BamMerger extends PicardCommandLine {
    
    private final Log log = Log.getInstance(BamMerger.class);
    
    private final String programName = "BamMerger";
    
    private final String programDS = "A command-line tool to merge BAM/SAM alignment info in the first input file"
            + " with the data in an unmapped BAM file,"
            + " producing a third BAM file that has alignment data"
            + " and all the additional data from the unmapped BAM";
   
    @Usage(programVersion= version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". "; 
 
    @Option(shortName= "ALIGNED", doc="The input SAM or BAM file with alignment.")
    public File ALIGNED_BAM;
    
    @Option(shortName= "PG", doc="The alignment program ID in the header of the SAM or BAM file with alignment.")
    public String ALIGNMENT_PROGRAM_ID = "bwa";
    
    @Option(shortName= "KEEP_PG", doc="keep all other PGs in aligned bam into output")
    public Boolean KEEP_ALL_PG = false;
    
    @Option(shortName= StandardOptionDefinitions.INPUT_SHORT_NAME, doc="The input unmapped SAM or BAM file to merge.")
    public File INPUT;

    @Option(shortName=StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc="The output file after merging.")
    public File OUTPUT;

    @Option(shortName= "KEEP", doc="KEEP extra unmapped reads in unmapped bam file to the final output if true.")
    public Boolean KEEP_EXTRA_UNMAPPED_READS = false;

    @Option(shortName= "REPLACE_QUAL", doc="Replace base qualities in aligned bam wtih the ones in unaligned bam if true.")
    public Boolean REPLACE_ALIGNED_BASE_QUALITY = false;
    
    private void mergealign_pairedsupp(SAMRecordIterator iteratorIn, SAMRecordIterator iteratorAlignments, SAMFileWriter out) {
        // We are assuming the primary alignment record always comes first
        SAMRecord alignment = null;
        SAMRecord nextAlignment = null;
        Stack<SAMRecord> secondary_supp = new Stack<SAMRecord>();
        if(iteratorAlignments.hasNext()){
            nextAlignment = iteratorAlignments.next();
        }

        while( iteratorIn.hasNext() ){
            SAMRecord record = iteratorIn.next();
            // Unaligned data shouldn't have these but meh
            if (record.isSecondaryOrSupplementary())
                continue;
            // Check if we are out of alignments
            if(alignment == null && nextAlignment != null) {
                // Hunt for the primary
                SAMRecord searchAlignment = nextAlignment;
                while (alignment == null) {
                    if (nextAlignment.getReadName().equals(searchAlignment.getReadName()) &&
                        nextAlignment.getReadPairedFlag() == searchAlignment.getReadPairedFlag() &&
                        (
                         (
                          nextAlignment.getReadPairedFlag() &&
                          nextAlignment.getFirstOfPairFlag() == searchAlignment.getFirstOfPairFlag() &&
                          nextAlignment.getSecondOfPairFlag() == searchAlignment.getSecondOfPairFlag()
                          )
                         ||
                         !searchAlignment.getReadPairedFlag()
                         )
                        )
                    {
                        if (nextAlignment.isSecondaryOrSupplementary()) {
                            secondary_supp.push(nextAlignment);
                            if (iteratorAlignments.hasNext()) {
                                nextAlignment = iteratorAlignments.next();
                            } else {
                                log.error( nextAlignment.getReadName() );
                                throw new RuntimeException("The mapped bam file has a read where there's a supplementary/secondary but no primary.");
                            }
                        } else {
                            alignment = nextAlignment;
                            nextAlignment = null;
                        }
                    } else {
                        log.error( searchAlignment.getReadName() );
                        throw new RuntimeException("The mapped bam file has a read where there's a supplementary/secondary but no primary.");
                    }
                }
                // Mop up any secondaries
                while (iteratorAlignments.hasNext()) {
                    nextAlignment = iteratorAlignments.next();
                    // is it from the same mapping as the current alignment?
                    if (nextAlignment.getReadName().equals(alignment.getReadName()) &&
                        nextAlignment.isSecondaryOrSupplementary() &&
                        nextAlignment.getReadPairedFlag() == alignment.getReadPairedFlag() &&
                            (
                             (
                              nextAlignment.getReadPairedFlag() &&
                              nextAlignment.getFirstOfPairFlag() == alignment.getFirstOfPairFlag() &&
                              nextAlignment.getSecondOfPairFlag() == alignment.getSecondOfPairFlag()
                             )
                             ||
                             !nextAlignment.getReadPairedFlag()
                            )
                        ) {
                        // it is a secondary for current alignment
                        secondary_supp.push(nextAlignment);
                        nextAlignment = null;
                    } else {
                        // new record so we move on
                        break;
                    }
                }
                
            } else if ( this.KEEP_EXTRA_UNMAPPED_READS ) { // no more piggies
                out.addAlignment(record);
                continue;
            } else {
                break;
            }
            String readName1 = record.getReadName();
            String readName2 = alignment.getReadName();
            
            boolean pairedRead1 = record.getReadPairedFlag();
            boolean pairedRead2 = alignment.getReadPairedFlag();
            
            boolean firstOfPair1 = false;
            if(pairedRead1){
                firstOfPair1 = record.getFirstOfPairFlag();
            }
            boolean firstOfPair2 = false;
            if(pairedRead2){
                firstOfPair2= alignment.getFirstOfPairFlag();
            }
            
            // Try and read in from unaligned BAM until read name matches align'd bam's read
            while( ( !readName1.equals(readName2)
                    || pairedRead1 != pairedRead2
                    || firstOfPair1 != firstOfPair2 )
                  ){
                // Output
                if( this.KEEP_EXTRA_UNMAPPED_READS ){
                    out.addAlignment(record);
                }
                
                if (iteratorIn.hasNext()) {
                    record = iteratorIn.next();
                    readName1 = record.getReadName();
                    pairedRead1 = record.getReadPairedFlag();
                    firstOfPair1 = false;
                    if (pairedRead1) {
                        firstOfPair1 = record.getFirstOfPairFlag();
                    }
                }else{
                    break; // No more unaligned input data
                }
                
            }
            // If we've found a matching record merge them
            if(  readName1.equals(readName2)
               && pairedRead1 == pairedRead2
               && firstOfPair1 == firstOfPair2
               ){
                this.mergeRecords(alignment, record);
                out.addAlignment(alignment); // Write merged record
                String parent_rg = (String) alignment.getAttribute("RG");
                while ( !secondary_supp.empty() ) {
                    SAMRecord s = secondary_supp.pop();
                    s.setAttribute("RG", parent_rg);
                    out.addAlignment(s); // Write merged supp record
                }
                alignment = null;
            } else if ( this.KEEP_EXTRA_UNMAPPED_READS ) {
                out.addAlignment(record);
            }
        }
        // If we have left over records in our aligned BAM something went wrong
        if( alignment != null || nextAlignment != null || iteratorAlignments.hasNext() ){
            SAMRecord firstRecordLeft = iteratorAlignments.next();
            log.error( firstRecordLeft.getReadName() + " " + firstRecordLeft.getFlags() );
            throw new RuntimeException("The mapped bam file has more reads than the unmapped"
                                       + " after reading their common reads in their begining.");
        }
    }
    
    @Override
    protected int doWork() {
      
        log.info("Checking input and output file");
        IoUtil.assertFileIsReadable(ALIGNED_BAM);
        IoUtil.assertFileIsReadable(INPUT);
        IoUtil.assertFileIsWritable(OUTPUT);
        
        log.info("Open aligned bam file: " + ALIGNED_BAM.getName());
        final SAMFileReader alignments  = new SAMFileReader(ALIGNED_BAM);
        SAMFileHeader headerAlignments = alignments.getFileHeader();

        //keep sequence dictionary from aligned bam
        SAMSequenceDictionary sequenceDictionary = headerAlignments.getSequenceDictionary();
        
        List<SAMProgramRecord> pgList = new ArrayList<SAMProgramRecord>();
        if(this.KEEP_ALL_PG){

            pgList = headerAlignments.getProgramRecords();
        }else{

            SAMProgramRecord alignmentProgram = headerAlignments.getProgramRecord(this.ALIGNMENT_PROGRAM_ID);
            pgList.add(alignmentProgram);
        }
          
        log.info("Open input file to merge: " + INPUT.getName());
        final SAMFileReader in  = new SAMFileReader(INPUT);        
        final SAMFileHeader header = in.getFileHeader();

        
        log.info("Generate new bam/sam output header");
        final SAMFileHeader outputHeader = header.clone();
        outputHeader.setSequenceDictionary(sequenceDictionary);
        outputHeader.setSortOrder(SAMFileHeader.SortOrder.unsorted);

        for(SAMProgramRecord pg : pgList){
          if(pg != null ){
             this.addProgramRecordToHead(outputHeader, pg);
          }
        }
        this.addProgramRecordToHead(outputHeader, this.getThisProgramRecord(programName, programDS));

        
        log.info("Open output file with header: " + OUTPUT.getName());
        final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader,  true, OUTPUT);
    
        log.info("Starting to merge");

        SAMRecordIterator iteratorAlignments = alignments.iterator();
        SAMRecordIterator iteratorIn = in.iterator();

        //mergealign_legacy(iteratorIn, iteratorAlignments, out);
        mergealign_pairedsupp(iteratorIn, iteratorAlignments, out);

        out.close();
        in.close();
        alignments.close();


        log.info("Merging finished, merged file: " + this.OUTPUT);
        
        return 0;
    }
    
    /**
     * 
     * @param alignment
     * @param record 
     */    
    public void mergeRecords(SAMRecord alignment, SAMRecord record){
        
        boolean isNegativeStrand1 = alignment.getReadNegativeStrandFlag();
        boolean isNegativeStrand2 = record.getReadNegativeStrandFlag();
        if( isNegativeStrand1 != isNegativeStrand2 ){
            SAMRecordUtil.reverseComplement(record);
        }


        if( ! Arrays.equals( alignment.getReadBases(), record.getReadBases() ) ){
            throw new RuntimeException( "Bases are different for read " + record.getReadName() );
        }
        
        if(this.REPLACE_ALIGNED_BASE_QUALITY){
            alignment.setBaseQualities(record.getBaseQualities());
        }else if( ! Arrays.equals( alignment.getBaseQualities(), record.getBaseQualities() ) ){
            throw new RuntimeException( "Qualities are different for read " + record.getReadName() );
        }
        
        alignment.setReadFailsVendorQualityCheckFlag(record.getReadFailsVendorQualityCheckFlag());
        boolean isPaired = record.getReadPairedFlag();
        alignment.setReadPairedFlag(isPaired);
        if(isPaired){
            alignment.setFirstOfPairFlag(record.getFirstOfPairFlag());
            alignment.setSecondOfPairFlag(record.getSecondOfPairFlag());
        }

        List<SAMTagAndValue> attributeList = record.getAttributes();
        for(SAMTagAndValue attribute : attributeList){
            
            String tag = attribute.tag;
            Object value = attribute.value;            
            if( alignment.getAttribute(tag) == null && value != null ){
               alignment.setAttribute(tag, value);
            }
        }

    }
    
    
    /**
     * example: ALIGNED=testdata/bam/6210_8_aligned.sam I=testdata/bam/6210_8.sam OUTPUT=testdata/6210_8_merged.bam  TMP_DIR=testdata VALIDATION_STRINGENCY=SILENT
     * 
     * @param args 
     */
    public static void main(final String[] args) {
        
        System.exit(new BamMerger().instanceMain(args));
    }
}
