/*
 * process a tile
 */
package illumina;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

/**
 *
 * @author Guoying Qi
 */
public class Tile {

    //fields must be given
    private final String intensityDir;
    private final String id;
    private final int lane;
    private final int tileNumber;

    private final HashMap<String, int[]> cycleRangeByRead;

    private final boolean includeSecondCall;
    private final boolean pfFilter;

    //temp fields
    private final String baseCallDir;
    private final String laneSubDir;
    private final String tileName;
    private final boolean pairedRead;
    private final boolean indexed;

    //file name
    private final String cLocsFileName;
    private final String filterFileName;

    //file reader list
    private final HashMap<String, BCLFileReader[]> bclFileReaderListByRead;
    private final HashMap<String, SCLFileReader[]> sclFileReaderListByRead;

    /**
     * 
     * @param intensityDir intensities directory
     * @param id instrument with run id, which will be used for read name
     * @param lane the run lane number
     * @param tileNumber this tile number
     * @param cycleRangeByRead cycle range for each read, the hash key could be read1, read2 or readIndex
     * @param secondCall include second base call or not
     * @param pfFilter include PF filtered reads or not
     */
    public Tile(String intensityDir,
            String id,
            int lane,
            int tileNumber,
            HashMap<String, int[]> cycleRangeByRead,
            boolean secondCall,
            boolean pfFilter) {

        this.id = id;
        this.lane = lane;
        this.tileNumber = tileNumber;
        this.intensityDir = intensityDir;

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
            Logger.getLogger(Tile.class.getName()).log(Level.SEVERE, "Read 1 must be given");
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

        this.laneSubDir = "L00" + this.lane;
        this.tileName = "s_1_" + this.tileNumber;
        this.baseCallDir = this.intensityDir
                + File.separator
                + "BaseCalls";

        this.cLocsFileName = this.intensityDir
                + File.separator
                + this.laneSubDir
                + File.separator
                + this.tileName + ".clocs";

        this.filterFileName = this.baseCallDir
                + File.separator
                + this.laneSubDir
                + File.separator
                + this.tileName + ".filter";
    }

    /**
     * read each cluster and write them to output bam file
     * @param outputSam
     * @throws Exception
     */
    public void processTile(SAMFileWriter outputSam) throws Exception {

        CLocsFileReader clocsFileReader = new CLocsFileReader(this.getcLocsFileName());
        FilterFileReader filterFileReader = new FilterFileReader(this.getFilterFileName());
        SAMFileHeader samFileHeader = outputSam.getFileHeader();

        int clusterIndex = 0;
        while (filterFileReader.hasNext()) {

            clusterIndex++;

            //position
            String[] pos = clocsFileReader.next();
            String readName = this.getReadName(pos);

            //filtered
            int filtered = (Integer) filterFileReader.next();

            //read 1
            String [] basesQuals1 = this.getNextClusterBaseQuals("read1");

            //read 2
            String [] basesQuals2 = null;
            if(this.isPairedRead()){
                basesQuals2 = this.getNextClusterBaseQuals("read2");
            }
            
            //index read
            String [] basesQualsIndex = null;
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
                outputSam.addAlignment(recordRead1);
                if(this.pairedRead){
                    SAMRecord recordRead2 = this.getSAMRecord(samFileHeader, readName, clusterIndex, basesQuals2, secondBases2, null, filtered, pairedRead, false);
                    outputSam.addAlignment(recordRead2);
                }
            }
        }

        //check number of cluster from filter file match the cluster nubmer in clocs file
        //the number of cluster in each bcl or scl not checked here
        if (clocsFileReader.getCurrentTotalClusters() != filterFileReader.getTotalClusters()) {
            throw new Exception("Number of clusters in clocs file does not match filter file "
                    + filterFileReader.getTotalClusters() + " "
                    + clocsFileReader.getCurrentTotalClusters());
        }

        //close clocs and filter file
        clocsFileReader.close();
        filterFileReader.close();
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

            BCLFileReader[] bclFileReaderListRead = this.openBCLFileByCycles(cycleRange);
            this.getBclFileReaderListByRead().put(read, bclFileReaderListRead);

            if (this.includeSecondCall) {
                SCLFileReader[] sclFileReaderListRead = this.openSCLFileByCycles(cycleRange);
                this.getSclFileReaderListByRead().put(read, sclFileReaderListRead);
            }
        }

    }

    /**
     *
     * open a list of BCL file for a range of cycles
     * @param cycleRange
     * @return
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
     * @return
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
     * @return
     * @throws Exception
     */
    public String[] getNextClusterBaseQuals(String read) throws Exception {
       BCLFileReader[] bclFileList = this.getBclFileReaderListByRead().get(read);
       return this.getNextClusterBaseQuals(bclFileList);
    }

    /**
     * read bases and qualities for next cluster of one read from its BCL file list
     * @param bclFileList
     * @return
     * @throws Exception
     */
    public String[] getNextClusterBaseQuals(BCLFileReader[] bclFileList) throws Exception {

        int readLength = bclFileList.length;

        StringBuilder bases = new StringBuilder(readLength);
        StringBuilder quals = new StringBuilder(readLength);

        for (BCLFileReader fileReader : bclFileList) {
            char[] cluster = fileReader.next();
            bases.append(cluster[0]);
            quals.append(cluster[1]);
        }

        String[] clusterPair = {bases.toString(), quals.toString()};

        return clusterPair;
    }

    /**
     * read second bases for next cluster of one read
     * @param read
     * @return
     * @throws Exception
     */

    public String getNextClusterSecondBases(String read) throws Exception {
       SCLFileReader[] sclFileList = this.getSclFileReaderListByRead().get(read);
       return this.getNextClusterSecondBases(sclFileList);
    }

    /**
     * read second bases for next cluster of one read from its BCL file list
     * @param sclFileList
     * @return
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
     * @return
     */
    public SAMRecord getSAMRecord(
            SAMFileHeader fileHeader,
            String readName,
            int clusterIndex,
            String[] baseQuals,
            String secondBases,
            String [] baseQualsIndex,
            int filter,
            boolean paired,
            boolean firstRead) {

        SAMRecord samRecord = new SAMRecord(fileHeader);

        samRecord.setReadName(readName);
        samRecord.setAttribute("ci", clusterIndex);
        samRecord.setReadString(baseQuals[0]);
        samRecord.setBaseQualityString(baseQuals[1]);
        samRecord.setReadUnmappedFlag(true);
        samRecord.setAttribute("RG", "1");

        if(filter == 0){
            samRecord.setReadFailsVendorQualityCheckFlag(true);
        }

        if(paired){
           samRecord.setReadPairedFlag(paired);
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
            samRecord.setAttribute("RT",baseQualsIndex[0]);
            samRecord.setAttribute("QT",baseQualsIndex[1]);
        }
        
        return samRecord;
    }

    /**
     * form read name for one cluster
     *
     * @param pos
     * @return
     */
    public String getReadName(String [] pos){
        return this.id
                + ":" + this.lane
                + ":" + this.tileNumber
                + ":" + pos[0]
                + ":" + pos[1];
    }

    /**
     *
     * @param cycle
     * @param firstCall
     * @return
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

    /**
     * @return the cLocsFileName
     */
    public String getcLocsFileName() {
        return cLocsFileName;
    }

    /**
     * @return the filterFileName
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

        String intensityDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities";
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

        Tile tile = new Tile(intensityDir, id, lane, tileNumber, cycleRangeByRead, true, true);

        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileHeader header = new SAMFileHeader();
        SAMFileWriter outputSam = factory
                .makeSAMOrBAMWriter(header, true, new File("test.bam"));

        tile.openBaseCallFiles();
        tile.processTile(outputSam);
        tile.closeBaseCallFiles();
        
        outputSam.close();
    }
}
