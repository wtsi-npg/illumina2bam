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
 * 
 */
package illumina;

import illumina.file.reader.FilterFileReader;
import illumina.file.reader.CLocsFileReader;
import illumina.file.reader.BCLFileReader;
import illumina.file.reader.SCLFileReader;
import illumina.file.reader.IlluminaFileReader;
import illumina.file.reader.PosFileReader;
import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.picard.util.Log;

/**
 * Process an illumina tile
 * 
 * @author Guoying Qi
 */
public class Tile {
    
    private final Log log = Log.getInstance(Tile.class);
    
    //fields must be given
    private final String intensityDir;
    private final String baseCallDir;
    private final String id;
    private final int laneNumber;
    private final int tileNumber;

    //fields for tag name
    private final String barcodeSeqTagName;
    private final String barcodeQualTagName;

    private final HashMap<String, int[]> cycleRangeByRead;

    private final boolean includeSecondCall;
    private final boolean pfFilter;

    //temp fields    
    private final String laneSubDir;
    private final String tileName;
    private final String tileNameInFour;
    private final boolean pairedRead;
    private final boolean indexed;

    //file name
    private final String cLocsFileName;
    protected final String posFileName;
    private final String filterFileName;

    //file reader list
    private final HashMap<String, BCLFileReader[]> bclFileReaderListByRead;
    private final HashMap<String, SCLFileReader[]> sclFileReaderListByRead;

    /**
     * 
     * @param intensityDir intensities directory
     * @param id instrument with run id, which will be used for read name
     * @param laneNumber the run laneNumber number
     * @param tileNumber this tile number
     * @param cycleRangeByRead cycle range for each read, the hash key could be read1, read2 or readIndex
     * @param secondCall include second base call or not
     * @param pfFilter include PF filtered reads or not
     */
    public Tile(String intensityDir,
            String baseCallDir,
            String id,
            int laneNumber,
            int tileNumber,
            HashMap<String, int[]> cycleRangeByRead,
            boolean secondCall,
            boolean pfFilter,
            String barcodeSeqTagName,
            String barcodeQualTagName) {

        this.id = id;
        this.laneNumber = laneNumber;
        this.tileNumber = tileNumber;
        this.intensityDir = intensityDir;
        this.baseCallDir  = baseCallDir;
        
        this.barcodeSeqTagName  = barcodeSeqTagName;
        this.barcodeQualTagName = barcodeQualTagName;
        
        this.includeSecondCall = secondCall;
        this.pfFilter = pfFilter;

        this.cycleRangeByRead = cycleRangeByRead;

        int numOfReads = this.cycleRangeByRead.size();
        this.bclFileReaderListByRead = new HashMap<String, BCLFileReader[]>(numOfReads);
        if (this.includeSecondCall) {
            this.sclFileReaderListByRead = new HashMap<String, SCLFileReader[]>(numOfReads);
        } else {
            this.sclFileReaderListByRead = null;
        }

        if (cycleRangeByRead.get("read1") == null) {
            log.error("Read 1 must be given");
        }

        if (cycleRangeByRead.get("read2") != null) {
            this.pairedRead = true;
        } else {
            this.pairedRead = false;
        }

        if (cycleRangeByRead.get("readIndex") != null) {
            this.indexed = true;
        } else {
            this.indexed = false;
        }

        this.laneSubDir = "L00" + this.laneNumber;
        this.tileName = "s_" + this.laneNumber + "_" + this.tileNumber;
        
        final DecimalFormat tileNumberFormatter = new DecimalFormat("0000");
        this.tileNameInFour = "s_" + this.laneNumber + "_" + tileNumberFormatter.format(this.tileNumber);

        this.cLocsFileName = this.intensityDir
                + File.separator
                + this.laneSubDir
                + File.separator
                + this.tileNameInFour + ".clocs";
        
        this.posFileName = this.intensityDir
                + File.separator
                + this.tileNameInFour + "_pos.txt";

        this.filterFileName = this.checkFilterFileName();
    }

    /**
     * read each cluster and write them to output bam file
     * @param outputSam
     * @throws Exception
     */
    public void processTile(SAMFileWriter outputSam) throws Exception {
        
        log.info("Open filter file: " + this.getFilterFileName());
        FilterFileReader filterFileReader = new FilterFileReader(this.getFilterFileName());
        
        File clocsFile = new File( this.getcLocsFileName() );
        File posFile = new File( this.getPosFileName() );

        CLocsFileReader clocsFileReader = null;
        PosFileReader posFileReader = null;
        boolean clocsExisted;

        if(clocsFile.exists()){
           log.info("open clocs file: " + this.getcLocsFileName());
           clocsFileReader = new CLocsFileReader(this.getcLocsFileName());
           clocsExisted = true;
        }else if( posFile.exists() ) {
           log.info("open pos file: " + this.getPosFileName());
           posFileReader = new PosFileReader(this.getPosFileName());
           clocsExisted = false;
        }else{
            String errorMessage = "Both clocs and pos files are not available for this tile: "
                    + this.getcLocsFileName() + " "
                    + this.getPosFileName();
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        SAMFileHeader samFileHeader = outputSam.getFileHeader();

        int totalClusterInTile = filterFileReader.getTotalClusters();
        //log.info("Total cluster from filter file: " + totalClusterInTile);

        //the number of cluster in each bcl or scl checked here
        this.checkBCLClusterNumber(totalClusterInTile);
        if(this.includeSecondCall){
            this.checkSCLClusterNumber(totalClusterInTile);
        }

        log.info("Reading cluster one by one");
        int clusterIndex = 0;
        while (filterFileReader.hasNext()) {

            clusterIndex++;

            //position
            String[] pos;
            if(clocsExisted){
                pos = clocsFileReader.next();
            }else{
                pos = posFileReader.next();
            }
            String readName = this.getReadName(pos);

            //filtered
            int filtered = (Integer) filterFileReader.next();

            //read 1
            byte [][] basesQuals1 = this.getNextClusterBaseQuals("read1");

            //read 2
            byte [][] basesQuals2 = null;
            if(this.isPairedRead()){
                basesQuals2 = this.getNextClusterBaseQuals("read2");
            }
            
            //index read
            byte [][] basesQualsIndex = null;
            if(this.isIndexed()){
                 basesQualsIndex = this.getNextClusterBaseQuals("readIndex");
            }

            //second call
            String secondBases1 = null;
            String secondBases2 = null;
            if(this.includeSecondCall){
                secondBases1 = this.getNextClusterSecondBases("read1");
                if(this.isPairedRead()){
                   secondBases2 = this.getNextClusterSecondBases("read2");
                }
            }


            //write to bam
            if(!(this.pfFilter && filtered == 0)){
                SAMRecord recordRead1 = this.getSAMRecord(samFileHeader, readName, clusterIndex, basesQuals1, secondBases1, basesQualsIndex, filtered, pairedRead, true);
                this.writeToBam(outputSam, recordRead1);
                if(this.pairedRead){
                    SAMRecord recordRead2 = this.getSAMRecord(samFileHeader, readName, clusterIndex, basesQuals2, secondBases2, null, filtered, pairedRead, false);
                    this.writeToBam(outputSam, recordRead2);
                }
            }
        }

        //check number of clusters from filter header is correct

        if(totalClusterInTile != filterFileReader.getCurrentCluster()){
            throw new Exception("Number of clusters in filter file "
                    + filterFileReader.getFileName()
                    + " is incorrect");
        }
        log.debug("Correct number of clusters processed in filter file: " + filterFileReader.getCurrentCluster());
       

        //check number of clusters from filter file match the cluster nubmer in clocs file
        if (clocsFileReader != null && clocsFileReader.getCurrentTotalClusters() != totalClusterInTile) {
            throw new Exception("Number of clusters in clocs file does not match filter file "
                    + filterFileReader.getTotalClusters() + " "
                    + clocsFileReader.getCurrentTotalClusters());
        }
      
        int totalCurrentClusters = 0;
        if(clocsFileReader != null){
             totalCurrentClusters = clocsFileReader.getCurrentTotalClusters();
        }else if(posFileReader != null){
             totalCurrentClusters = posFileReader.getCurrentTotalClusters();
        }
        log.debug("Correct number of clusters processed in clocs or pos file: " + totalCurrentClusters);
        
        if(clocsFileReader != null && clocsFileReader.hasNext()){
            log.debug("There may be more clusters in clocs file");
        }

        log.info(filterFileReader.getCurrentPFClusters() + " PF clusters in this tile out of total " + totalClusterInTile);

        //close clocs or pos,  and filter file
        if(clocsFileReader != null){
            clocsFileReader.close();
        }
        if(posFileReader != null){
            posFileReader.close();
        }
        filterFileReader.close();
    }
    
    /**
     * 
     * @param outputSam where to write bam record
     * @param recordRead bam record
     */
    private void writeToBam(SAMFileWriter outputSam, SAMRecord recordRead ){
        outputSam.addAlignment(recordRead);
    }

    /**
     *
     * @param expectedClusterNumber
     * @return true if cluster number in bcl file match the one in filter file
     * @throws Exception
     */
    public boolean checkBCLClusterNumber(int expectedClusterNumber) throws Exception{
 
        log.debug("Checking cluster number in BCL files");
        for (Map.Entry<String, BCLFileReader []> entry : this.bclFileReaderListByRead.entrySet()) {
              BCLFileReader [] bclFileReaderList = entry.getValue();
              for(BCLFileReader bclFileReader: bclFileReaderList){
                  if( bclFileReader.getTotalClusters() != expectedClusterNumber){
                      throw new Exception("Number of Clusters in BCL file "
                              + bclFileReader.getFileName()
                              + " "
                              + bclFileReader.getTotalClusters()
                              + " not as expected:"
                              + expectedClusterNumber
                              );
                  }
              }
        }
        return true;
    }

    /**
     *
     * @param expectedClusterNumber
     * @return true if cluster number in scl file match the one in filter file
     * @throws Exception
     */
    public boolean checkSCLClusterNumber(int expectedClusterNumber) throws Exception {
        
        log.debug("Checking cluster number in SCL Files");

        for (Map.Entry<String, SCLFileReader[]> entry : this.sclFileReaderListByRead.entrySet()) {
            SCLFileReader[] sclFileReaderList = entry.getValue();
            for (SCLFileReader sclFileReader : sclFileReaderList) {
                if (sclFileReader.getTotalClusters() != expectedClusterNumber) {
                    throw new Exception("Number of Clusters in SCL file "
                            + sclFileReader.getFileName()
                            + " "
                            + sclFileReader.getTotalClusters()
                            + " not as expected:"
                            + expectedClusterNumber);
                }
            }
        }
        return true;
    }
    
    /**
     * open all BCL or SCL files
     *
     * @throws Exception
     */
    public void openBaseCallFiles() throws Exception {

        for (Map.Entry<String, int[]> entry : this.cycleRangeByRead.entrySet()) {

            String read = entry.getKey();
            int[] cycleRange = entry.getValue();

            log.info("Opening BCL Files for " + read );
            BCLFileReader[] bclFileReaderListRead = this.openBCLFileByCycles(cycleRange);
            this.getBclFileReaderListByRead().put(read, bclFileReaderListRead);

            if (this.includeSecondCall) {
                
                log.info("Opening SCL Files for " + read);
                SCLFileReader[] sclFileReaderListRead = this.openSCLFileByCycles(cycleRange);
                this.getSclFileReaderListByRead().put(read, sclFileReaderListRead);
            }
        }

    }

    /**
     *
     * open a list of BCL file for a range of cycles
     * @param cycleRange
     * @return an array of BCLFileReader
     * @throws Exception
     */
    private BCLFileReader[] openBCLFileByCycles(int[] cycleRange) throws Exception {

        int start = cycleRange[0];
        int end = cycleRange[1];
        int readLength = end - start + 1;
        BCLFileReader[] bclFileReaderList = new BCLFileReader[readLength];

        int index = 0;
        for (int cycle = start; cycle <= end; cycle++) {
            bclFileReaderList[index] = new BCLFileReader(this.getBaseCallFileName(cycle, true));
            index++;
        }
        return bclFileReaderList;
    }

    /**
     * open a list of SCL file for a range of cycles
     *
     * @param cycleRange
     * @return an array of SCLFileReader
     * @throws Exception
     */
    private SCLFileReader[] openSCLFileByCycles(int[] cycleRange) throws Exception {
        int start = cycleRange[0];
        int end = cycleRange[1];
        SCLFileReader[] sclFileReaderList = new SCLFileReader[end - start + 1];

        int index = 0;
        for (int cycle = start; cycle <= end; cycle++) {
            sclFileReaderList[index] = new SCLFileReader(this.getBaseCallFileName(cycle, false));
            index++;
        }
        return sclFileReaderList;
    }

    /**
     * close all BCL or SCL file in not closed yet
     *
     */
    public void closeBaseCallFiles() {

        if (this.getBclFileReaderListByRead() != null) {
            for (BCLFileReader[] list : this.getBclFileReaderListByRead().values()) {
                this.closeFileReaderList(list);
            }
        }

        if (this.getSclFileReaderListByRead() != null) {
            for (SCLFileReader[] list : this.getSclFileReaderListByRead().values()) {
                this.closeFileReaderList(list);
            }
        }
    }

    /**
     * close a list of file readers
     * @param fileReaderList
     */
    private void closeFileReaderList(IlluminaFileReader[] fileReaderList) {
        for (IlluminaFileReader fileReader : fileReaderList) {
            fileReader.close();
        }
    }

    /**
     * read bases and qualities for next cluster of one read
     * @param read
     * @return next cluster base and quality value as byte array for a read 
     * @throws Exception
     */
    public byte[][] getNextClusterBaseQuals(String read) throws Exception {
       BCLFileReader[] bclFileList = this.getBclFileReaderListByRead().get(read);
       return this.getNextClusterBaseQuals(bclFileList);
    }

    /**
     * read bases and qualities for next cluster of one read from its BCL file list
     * @param bclFileList
     * @return next cluster base and quality value as byte array from BCL file list
     * @throws Exception
     */
    public byte [][] getNextClusterBaseQuals(BCLFileReader[] bclFileList) throws Exception {

        int readLength = bclFileList.length;

        byte [][] clusterBaseQuals = new byte[2][readLength];
        int count = 0;
        for (BCLFileReader fileReader : bclFileList) {            
            byte [] cluster = fileReader.next();
            clusterBaseQuals[0][count] = cluster[0];
            clusterBaseQuals[1][count] = cluster[1];
            count++;
        }

        return clusterBaseQuals;
    }

    /**
     * read second bases for next cluster of one read
     * @param read
     * @return get next cluster second bases for a read
     * @throws Exception
     */

    public String getNextClusterSecondBases(String read) throws Exception {
       SCLFileReader[] sclFileList = this.getSclFileReaderListByRead().get(read);
       return this.getNextClusterSecondBases(sclFileList);
    }

    /**
     * read second bases for next cluster of one read from its BCL file list
     * @param sclFileList
     * @return get next cluster second bases from SCL file list
     * @throws Exception
     */
    public String getNextClusterSecondBases(SCLFileReader[] sclFileList) throws Exception {

        int readLength = sclFileList.length;

        StringBuilder bases = new StringBuilder(readLength);

        for (SCLFileReader fileReader : sclFileList) {
            char cluster = fileReader.next();
            bases.append(cluster);
        }
        return bases.toString();
    }


    /**
     * write all together for one SAM Record
     *
     * @param fileHeader
     * @param readName
     * @param clusterIndex
     * @param baseQuals
     * @param secondBases
     * @param baseQualsIndex
     * @param filter
     * @param paired
     * @param firstRead
     * @return SAM record
     */
    public SAMRecord getSAMRecord(
            SAMFileHeader fileHeader,
            String readName,
            int clusterIndex,
            byte [][] baseQuals,
            String secondBases,
            byte [][] baseQualsIndex,
            int filter,
            boolean paired,
            boolean firstRead) {

        SAMRecord samRecord = new SAMRecord(fileHeader);

        samRecord.setReadName(readName);
        samRecord.setAttribute("ci", clusterIndex);
        samRecord.setReadBases(baseQuals[0]);
        samRecord.setBaseQualities(baseQuals[1]);
        samRecord.setReadUnmappedFlag(true);
        
        
        String rgId = "1";
        
        List<SAMReadGroupRecord> readGroupList = null;
        if(fileHeader != null) {
            readGroupList = fileHeader.getReadGroups();
        }

        if (readGroupList != null && !readGroupList.isEmpty()) {
            SAMReadGroupRecord readGroup = readGroupList.get(0);
            rgId = readGroup.getId();
        }
        
        samRecord.setAttribute("RG", rgId);

        if(filter == 0){
            samRecord.setReadFailsVendorQualityCheckFlag(true);
        }

        if(paired){
           samRecord.setReadPairedFlag(paired);
           samRecord.setMateUnmappedFlag(true);
           if(firstRead){
               samRecord.setFirstOfPairFlag(firstRead);
           }else{
               samRecord.setSecondOfPairFlag(true);
           }
        }

        if( secondBases != null ){
            samRecord.setAttribute("E2", secondBases);
        }

        if(baseQualsIndex != null){

            samRecord.setAttribute(this.barcodeSeqTagName, this.convertByteArrayToString(baseQualsIndex[0]));
            samRecord.setAttribute(this.barcodeQualTagName, this.convertPhredQualByteArrayToFastqString(baseQualsIndex[1]));
        }
        
        return samRecord;
    }

    /**
     * form read name for one cluster
     *
     * @param pos
     * @return whole read name
     */
    public String getReadName(String [] pos){
        return this.id
                + ":" + this.laneNumber
                + ":" + this.tileNumber
                + ":" + pos[0]
                + ":" + pos[1];
    }

    /**
     *
     * @param cycle
     * @param firstCall
     * @return BCL or SCL base call file name 
     */
    public String getBaseCallFileName(int cycle, boolean firstCall) {
        String cycleDir = this.baseCallDir
                + File.separator
                + this.laneSubDir
                + File.separator
                + "C" + cycle + ".1"
                + File.separator
                + this.tileName;
        return firstCall ? cycleDir + ".bcl" : cycleDir + ".scl";
    }
    
    private String checkFilterFileName(){

        String filterFileNameLocal = this.baseCallDir
                + File.separator
                + this.laneSubDir
                + File.separator
                + this.tileNameInFour + ".filter";
        File filterFile = new File(filterFileNameLocal);
    
        if(!filterFile.exists()){
            log.info("Filter file " + filterFileNameLocal + " not in the basecall lane directory");
            filterFileNameLocal = this.baseCallDir
                + File.separator
                + this.tileNameInFour + ".filter";
            log.info("Now trying base call directory for the filter file: " + filterFileNameLocal);
        }
        filterFile = new File(filterFileNameLocal);
        if( !filterFile.exists() ){
            log.error("No filter file found for this tile");
            filterFileNameLocal = null;
        }
        return filterFileNameLocal;
    }

    /**
     *
     * @param array
     * @return fastq quality string from phred qual byte array
     */
    public String convertPhredQualByteArrayToFastqString(byte [] array){
        return Illumina2bamUtils.convertPhredQualByteArrayToFastqString(array);
    }

    /**
     * 
     * @param array
     * @return string from a byte array
     */
    public String convertByteArrayToString(byte [] array){
        return Illumina2bamUtils.convertByteArrayToString(array);
    }

    /**
     * @return the cLocsFileName
     */
    public String getcLocsFileName() {
        return cLocsFileName;
    }

    /**
     * @return the filterFileNameLocal
     */
    public String getFilterFileName() {
        return filterFileName;
    }

    /**
     * @return the pairedRead
     */
    public boolean isPairedRead() {
        return pairedRead;
    }

    /**
     * @return the indexed
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     * @return the bclFileReaderListByRead
     */
    public HashMap<String, BCLFileReader[]> getBclFileReaderListByRead() {
        return bclFileReaderListByRead;
    }

    /**
     * @return the sclFileReaderListByRead
     */
    public HashMap<String, SCLFileReader[]> getSclFileReaderListByRead() {
        return sclFileReaderListByRead;
    }

    /**
     * test main method
     *
     * @param args
     * @throws Exception
     */
    public static void main (String [] args) throws Exception{

        String barcodeSeqTagName = "BC";
        String barcodeQualTagName = "QT";
     
        String intensityDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities";
        String baseCallDir  = intensityDir + "/BaseCalls";
        if(args.length > 0  && args[0] != null){
            intensityDir = args[0];
        }
        
        String id = "HS13_6000";
        int lane = 1;
        int tileNumber = 1101;

        int [] cycleRangeRead1 = {1, 2};
        int [] cycleRangeRead2 = {51, 52};
        int [] cycleRangeIndex = {50, 50};

        HashMap< String, int[] > cycleRangeByRead = new HashMap<String, int[]>(1);
        cycleRangeByRead.put("read1", cycleRangeRead1);
        cycleRangeByRead.put("read2", cycleRangeRead2);
        cycleRangeByRead.put("readIndex", cycleRangeIndex);

        Tile tile = new Tile(intensityDir, baseCallDir, id, lane, tileNumber, cycleRangeByRead, true, true, barcodeSeqTagName, barcodeQualTagName);

        File outBam = new File("test.bam");
        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileHeader header = new SAMFileHeader();
        SAMFileWriter outputSam = factory
                .makeSAMOrBAMWriter(header, true, outBam);

        tile.openBaseCallFiles();
        tile.processTile(outputSam);
        tile.closeBaseCallFiles();
        
        outputSam.close();
    }

    /**
     * @return the posFileName
     */
    public String getPosFileName() {
        return posFileName;
    }
}
