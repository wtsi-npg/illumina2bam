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
import java.util.HashMap;
import java.util.List;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.metrics.MetricsFile;
import net.sf.picard.util.Log;
import net.sf.samtools.*;

/**
 * This class is used decode the multiplexed bam file.
 * 
 * Each read in BAM file will be marked in its read name and read group,
 * There is an option to output bam file by tag.
 * 
 * The bar code list can be passed in through command line
 * or by a file with extra information barcode name, library name, sample name and description, which are separated by tab
 * this file must have a header: barcode_sequence	barcode_name	library_name	sample_name	description
 * 
 * The read group will be changed and re-added in.
 * 
 * @author gq1@sanger.ac.uk
 * 
 */

public class BamIndexDecoder extends PicardCommandLine {
    
    private final Log log = Log.getInstance(BamIndexDecoder.class);
    
    private final String programName = "BamIndexDecoder";
    
    private final String programDS = "A command-line tool to decode multiplexed bam file";
   
    @Usage(programVersion= version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". ";
   
    @Option(shortName= StandardOptionDefinitions.INPUT_SHORT_NAME, doc="The input SAM or BAM file to decode.")
    public File INPUT;
    
    @Option(shortName=StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc="The output file after decoding.", mutex = {"OUTPUT_DIR"} )
    public File OUTPUT;
    
    @Option(doc="The output directory for bam files for each barcode if you want to split the output", mutex = {"OUTPUT"})
    public File OUTPUT_DIR;
    
    @Option(doc="The prefix for bam or sam file when you want to split output by barcodes", mutex = {"OUTPUT"})
    public String OUTPUT_PREFIX;
    
    @Option(doc="The extension name for split file when you want to split output by barcodes: bam or sam", mutex = {"OUTPUT"})
    public String OUTPUT_FORMAT;
    
    @Option(shortName="BC_SEQ", doc="The tag name used to store barcode read in bam records")
    public String BARCODE_TAG_NAME = "BC";

    @Option(shortName="BC_QUAL", doc="Tag name for barcode quality.")
    public String BARCODE_QUALITY_TAG_NAME = "QT";
  
    @Option(doc="Barcode sequence.  These must be unique, and all the same length.", mutex = {"BARCODE_FILE"})
    public List<String> BARCODE = new ArrayList<String>();

    @Option(doc="Tab-delimited file of barcode sequences, and optionally barcode name and library name.  " +
            "Barcodes must be unique, and all the same length.  Column headers must be 'barcode_sequence', " +
            "'barcode_name', and 'library_name'.", mutex = {"BARCODE"})
    public File BARCODE_FILE;

    @Option(doc="Per-barcode and per-lane metrics written to this file.", shortName = StandardOptionDefinitions.METRICS_FILE_SHORT_NAME)
    public File METRICS_FILE;

    @Option(doc="Maximum mismatches for a barcode to be considered a match.")
    public int MAX_MISMATCHES = 1;

    @Option(doc="Minimum difference between number of mismatches in the best and second best barcodes for a barcode to be considered a match.")
    public int MIN_MISMATCH_DELTA = 1;

    @Option(doc="Maximum allowable number of no-calls in a barcode read before it is considered unmatchable.")
    public int MAX_NO_CALLS = 2;
    
    @Option(doc="Convert low quality bases in barcode read to Ns .")
    public boolean CONVERT_LOW_QUALITY_TO_NO_CALL = false;
    
    @Option(doc="Max low quality phred value to convert bases in barcode read to Ns .")
    private int MAX_LOW_QUALITY_TO_CONVERT = 15;

    private int barcodeLength;
    
    private IndexDecoder indexDecoder;
    
    private SAMFileWriter out;
    private HashMap<String, SAMFileWriter> outputList;
    private HashMap<String, String> barcodeNameList;
    
    public BamIndexDecoder() {
    }

    @Override
    protected int doWork() {
        
        this.log.info("Checking input and output file");
        IoUtil.assertFileIsReadable(INPUT);
        if(OUTPUT != null){
            IoUtil.assertFileIsWritable(OUTPUT);
        }
        if(OUTPUT_DIR != null){
            IoUtil.assertDirectoryIsWritable(OUTPUT_DIR);
        }
        IoUtil.assertFileIsWritable(METRICS_FILE);
        
        log.info("Open input file: " + INPUT.getName());
        final SAMFileReader in  = new SAMFileReader(INPUT);        
        final SAMFileHeader header = in.getFileHeader();
        
        this.generateOutputFile(header);
        List<SAMReadGroupRecord> readGroupList = header.getReadGroups();
        String readGroupOnlyIdInHeader = null;
        if(readGroupList.size() == 1){
            readGroupOnlyIdInHeader = readGroupList.get(0).getId();
        }
                
        log.info("Decoding records");        
        SAMRecordIterator inIterator = in.iterator();
        while(inIterator.hasNext()){
            
            String barcodeRead = null;
            String barcodeQual = null;

            SAMRecord record = inIterator.next();            
            String readName = record.getReadName();
            boolean isPaired = record.getReadPairedFlag();
            boolean isPf = ! record.getReadFailsVendorQualityCheckFlag();

            Object barcodeReadObject = record.getAttribute(this.BARCODE_TAG_NAME);
            if(barcodeReadObject != null){
                    barcodeRead = barcodeReadObject.toString();
            }

            if( this.CONVERT_LOW_QUALITY_TO_NO_CALL ){
               Object barcodeQualObject = record.getAttribute( this.BARCODE_QUALITY_TAG_NAME );
               if(barcodeQualObject != null){
                    barcodeQual = barcodeQualObject.toString();
               }
            }
            
            SAMRecord pairedRecord = null;
            
            if(isPaired){
                
                pairedRecord = inIterator.next();
                String readName2 = pairedRecord.getReadName();
                boolean isPaired2 = pairedRecord.getReadPairedFlag();
                
                if( !readName.equals(readName2) || !isPaired2 ){
                    throw new RuntimeException("The paired reads are not together: " + readName + " " + readName2);
                }
                
                Object barcodeReadObject2= pairedRecord.getAttribute(this.BARCODE_TAG_NAME);
                if(barcodeReadObject != null
                        && barcodeReadObject2 != null
                        && ! barcodeReadObject.equals(barcodeReadObject2) ){
                    
                    throw new RuntimeException("barcode read bases are different in paired two reads: "
                            + barcodeReadObject + " " + barcodeReadObject2);
                } else if( barcodeRead == null && barcodeReadObject2 != null ){
                    
                    barcodeRead = barcodeReadObject2.toString();
                    
                    if (this.CONVERT_LOW_QUALITY_TO_NO_CALL) {
                        Object barcodeQualObject2 = pairedRecord.getAttribute(this.BARCODE_QUALITY_TAG_NAME);
                        if (barcodeQualObject2 != null) {
                            barcodeQual = barcodeQualObject2.toString();
                        }
                    }
                }                
            }
            
            if(barcodeRead == null ){
                throw new RuntimeException("No barcode read found for record: " + readName );
            }

            if (this.CONVERT_LOW_QUALITY_TO_NO_CALL) {
               
               barcodeRead = this.checkBarcodeQuality(barcodeRead, barcodeQual);
            }

            if(barcodeRead.length() < this.barcodeLength){
                throw new RuntimeException("The barcode read length is less than barcode lenght: " + readName );
            }else{            
                barcodeRead = barcodeRead.substring(0, this.barcodeLength);
            }

            IndexDecoder.BarcodeMatch match = this.indexDecoder.extractBarcode(barcodeRead, isPf);
            String barcode = match.barcode;
            
            if( match.matched ) {
               barcode = barcode.toUpperCase();
            } else {
               barcode = "";
            }
            
            String barcodeName = this.barcodeNameList.get(barcode);

            this.markBarcode(record, barcodeName, readGroupOnlyIdInHeader);
            
            if (isPaired) {
                this.markBarcode(pairedRecord, barcodeName, readGroupOnlyIdInHeader);
            }
            
            if( OUTPUT != null ){
                out.addAlignment(record);
                if(isPaired){
                    out.addAlignment(pairedRecord);
                }
            } else {
                
                SAMFileWriter outPerBarcode = this.outputList.get(barcode);
                outPerBarcode.addAlignment(record);
                if(isPaired){
                    outPerBarcode.addAlignment(pairedRecord);
                }                
            }
            
        }
        
        if(out != null){
           out.close();
        }
        this.closeOutputList();
        
        log.info("Decoding finished");
        
        
        log.info("Writing out metrhics file");        
        final MetricsFile<IndexDecoder.BarcodeMetric, Integer> metrics = getMetricsFile();        
        indexDecoder.writeMetrics(metrics, METRICS_FILE);
        
        log.info("All finished");

        return 0;
    }
    
    private SAMRecord markBarcode(SAMRecord record, String barcodeName, String readGroupOnlyIdInHeader) {

        String readName = record.getReadName();
        record.setReadName(readName + "#" + barcodeName);

        Object oldReadGroupId = record.getAttribute("RG");
        if (oldReadGroupId == null && readGroupOnlyIdInHeader != null) {
            oldReadGroupId = readGroupOnlyIdInHeader;
        } else if( oldReadGroupId == null ) {
            throw new RuntimeException("No read group id given for read " + readName + " and more than one read group defined in header");
        }
        record.setAttribute("RG", oldReadGroupId + "#" + barcodeName);
        return record;
    }
    
    /**
     * 
     * @param header
     */
    public void generateOutputFile(SAMFileHeader header) {
        
        List<IndexDecoder.NamedBarcode> barcodeList = indexDecoder.getNamedBarcodes(); 
        
        this.barcodeNameList = new HashMap<String, String>();
        
        List<SAMReadGroupRecord> oldReadGroupList = header.getReadGroups();        
        List<SAMReadGroupRecord> fullReadGroupList = new ArrayList<SAMReadGroupRecord>();
        
        if (OUTPUT_DIR != null) {
            log.info("Open a list of output bam/sam file per barcode");
            outputList = new HashMap<String, SAMFileWriter>();
        }
        final SAMFileHeader outputHeader = header.clone();
        final SAMProgramRecord programRecord = this.addProgramRecordToHead(outputHeader, this.getThisProgramRecord(programName, programDS));

        for (int count = 0; count <= barcodeList.size(); count++) {

            String barcodeName = null;
            String barcode = null;
            IndexDecoder.NamedBarcode namedBarcode = null;
            List<SAMReadGroupRecord> readGroupList = new ArrayList<SAMReadGroupRecord>();

            if ( count != 0 ) {
                namedBarcode = barcodeList.get(count - 1);
                barcodeName = namedBarcode.barcodeName;
                barcode = namedBarcode.barcode;
                barcode = barcode.toUpperCase();
            }else{
                barcode = "";
            }

            if (barcodeName == null || barcodeName.equals("")) {
                barcodeName = Integer.toString(count);
            }

            for(SAMReadGroupRecord r : oldReadGroupList){
                    SAMReadGroupRecord newReadGroupRecord = new SAMReadGroupRecord(r.getId() + "#" + barcodeName, r);
                    newReadGroupRecord.setAttribute("PG", programRecord.getProgramGroupId());
                    String pu = newReadGroupRecord.getPlatformUnit();
                    if(pu != null){
                        newReadGroupRecord.setPlatformUnit(pu + "#" + barcodeName);
                    }
                    if(namedBarcode != null){
                        if( namedBarcode.libraryName != null && !namedBarcode.libraryName.equals("") ){
                           newReadGroupRecord.setLibrary(namedBarcode.libraryName);
                        }
                        if( namedBarcode.sampleName !=null && !namedBarcode.sampleName.equals("") ){
                           newReadGroupRecord.setSample(namedBarcode.sampleName);
                        }
                        if(namedBarcode.description != null && !namedBarcode.description.equals("") ){
                            newReadGroupRecord.setDescription(namedBarcode.description);
                        }
                    }
                    readGroupList.add(newReadGroupRecord);
            }
            fullReadGroupList.addAll(readGroupList);


            if (OUTPUT_DIR != null) {
                String barcodeBamOutputName = OUTPUT_DIR
                        + File.separator
                        + OUTPUT_PREFIX
                        + "#"
                        + barcodeName
                        + "."
                        + OUTPUT_FORMAT;
                final SAMFileHeader perBarcodeOutputHeader = outputHeader.clone();
                perBarcodeOutputHeader.setReadGroups(readGroupList);
                final SAMFileWriter outPerBarcode = new SAMFileWriterFactory().makeSAMOrBAMWriter(perBarcodeOutputHeader, true, new File(barcodeBamOutputName));
                outputList.put(barcode, outPerBarcode);
            }
            barcodeNameList.put(barcode, barcodeName);
        }
        
        if (OUTPUT != null) {
            log.info("Open output file with header: " + OUTPUT.getName());
            outputHeader.setReadGroups(fullReadGroupList);
            this.out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader, true, OUTPUT);
        }

    }
    
    /**
     * close output files
     */
    public void closeOutputList(){
        if( this.outputList != null ){
            for(SAMFileWriter writer: this.outputList.values()){
                writer.close();
            }
        }
    }

    /**
     *
     * @return null if command line is valid.  If command line is invalid, returns an array of error message
     *         to be written to the appropriate place.
     */
    @Override
    protected String[] customCommandLineValidation() {
        
        final ArrayList<String> messages = new ArrayList<String>();

        if (BARCODE_FILE != null) {
            this.indexDecoder = new IndexDecoder(BARCODE_FILE);
        } else {
            this.indexDecoder = new IndexDecoder(BARCODE);
        }
        
        indexDecoder.setMaxMismatches(this.MAX_MISMATCHES);
        indexDecoder.setMaxNoCalls(MAX_NO_CALLS);
        indexDecoder.setMinMismatchDelta(this.MIN_MISMATCH_DELTA);
        
        indexDecoder.prepareDecode(messages);
        this.barcodeLength = indexDecoder.getBarcodeLength();

        if (messages.isEmpty()) {
            return null;
        }
        return messages.toArray(new String[messages.size()]);
    }
    
    /**
     * 
     * @param barcodeRead
     * @param barcodeQual
     * @return new bar code read string with low quality bases converted to N
     */
    public String checkBarcodeQuality(String barcodeRead, String barcodeQual){

        if(barcodeQual == null){
            return barcodeRead;
        }
        if(barcodeRead == null || barcodeRead.length() != barcodeQual.length()){
            throw new RuntimeException("Barcode read sequence not available or its lenght not match quality length ");
        }

        StringBuilder newBarcodeRead = new StringBuilder(barcodeRead.length());
        for (int i = 0; i<barcodeRead.length(); i++){
            int qual = (int) barcodeQual.charAt(i);
            char base = barcodeRead.charAt(i);

            if(qual <= this.MAX_LOW_QUALITY_TO_CONVERT + 33 ){
                newBarcodeRead.append('N');
            }else{
                newBarcodeRead.append(base);
            }
        }
        
        return newBarcodeRead.toString();
    }

    /**
     * 
     * @param argv
     */
    public static void main(final String[] argv) {
        System.exit(new BamIndexDecoder().instanceMain(argv));
    }

}
