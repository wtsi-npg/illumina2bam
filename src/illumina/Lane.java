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

package illumina;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.picard.util.Log;



/**
 * Process an illumina lane
 * 
 * @author Guoying Qi
 */
public class Lane {
    
    private final Log log = Log.getInstance(Lane.class);
    
    //fields must be given about input data
    private final String intensityDir;
    private final String baseCallDir;
    private final int laneNumber;

    //fields must be given about what to output
    private final boolean includeSecondCall;
    private final boolean pfFilter;

    //fields must be given for output bam
    private final File output;
    
    //fields for tag name
    private final String barcodeSeqTagName;
    private final String barcodeQualTagName;



    //config xml file name and XML Documetns
    private final String baseCallsConfig;
    private final String intensityConfig;
    private Document baseCallsConfigDoc = null;
    private Document intensityConfigDoc = null;
    private Node runConfigXmlNode = null;


    //read from config file
    private String id;
    private HashMap<String, int[]> cycleRangeByRead;
    private int [] tileList;
    private SAMProgramRecord baseCallProgram;
    private SAMProgramRecord instrumentProgram;
    
    private Date runDateConfig;
    private String runfolderConfig;
    
    //other fields    
    private SAMProgramRecord illumina2bamProgram;
    private SAMReadGroupRecord readGroup;

    private final XPath xpath;


    /**
     *
     * @param intensityDir Illumina intensities directory including config xml file and clocs files under lane directory. Required.
     * @param baseCallDir Illumina basecalls directory including config xml file, and filter files, bcl, maybe scl 
     * files under lane cycle directory, using BaseCalls directory under intensities if not given.
     * @param laneNumber lane number
     * @param secondCall including second base call or not, default false.
     * @param pfFilter Filter cluster or not, default true.
     * @param output Output file
     */
    public Lane(String intensityDir,
                String baseCallDir,
                int laneNumber,
                boolean secondCall,
                boolean pfFilter,
                File output,
                String barcodeSeqTagName,
                String barcodeQualTagName){

        this.intensityDir      = intensityDir;
        this.baseCallDir       = baseCallDir;
        this.laneNumber        = laneNumber;
        this.includeSecondCall = secondCall;
        this.pfFilter          = pfFilter;
        this.output            = output;
        this.barcodeSeqTagName  = barcodeSeqTagName;
        this.barcodeQualTagName = barcodeQualTagName;

        this.baseCallsConfig = this.baseCallDir
                             + File.separator
                             + "config.xml";

        this.intensityConfig = this.intensityDir
                             + File.separator
                             + "config.xml";

        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();

        this.initConfigsDoc();
    }

    /**
     * read both config XML files under BaseCalls and Intensities.
     * 
     * @return true if successfully
     * @throws Exception
     */
    public boolean readConfigs() throws Exception{

        this.readBaseCallsConfig();
        this.readIntensityConfig();
        this.readRunConfig();

        return true;
    }

    /**
     *
     * @return outputSam with header to write bam records
     */
    public SAMFileWriter generateOutputSamStream(){

        SAMFileWriterFactory factory = new SAMFileWriterFactory();

        SAMFileHeader header = this.generateHeader();

        SAMFileWriter outputSam = factory.makeSAMOrBAMWriter(header, false, output);

        return outputSam;
    }

    /**
     * write BCL file to output stream tile by tile
     * 
     * @param outputSam
     * @return true if successfully
     * @throws Exception
     */
    public boolean processTiles(SAMFileWriter outputSam) throws Exception{

        for(int tileNumber : this.tileList){
            
            log.info("Tile: " + tileNumber);
            
            Tile tile = new Tile(intensityDir, baseCallDir, id, laneNumber, tileNumber,
                                 cycleRangeByRead,
                                 this.includeSecondCall, this.pfFilter,
                                 this.barcodeSeqTagName, this.barcodeQualTagName);
            
            log.info("Opening all basecall files");
            tile.openBaseCallFiles();
            
            log.info("Reading all base call files");
            tile.processTile(outputSam);
            
            log.info("Closing base call files");
            tile.closeBaseCallFiles();
        }

        return true;
    }

    /**
     * initial XML document
     */
    private void initConfigsDoc(){

        DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            log.error(ex, "Problems to generate XML DocumentBuilder");
        }

        File baseCallsConfigFile = new File(this.baseCallsConfig);
        
        if (baseCallsConfigFile.exists()) {
            try {
                baseCallsConfigDoc = db.parse(baseCallsConfigFile);
            } catch (SAXException ex) {
                log.error(ex, "Problems to parsing basecalls config xml file " + this.baseCallsConfig);
            } catch (IOException ex) {
                log.error(ex, "Problems to read basecall config file " + this.baseCallsConfig);
            }

            NodeList runNodeList = baseCallsConfigDoc.getElementsByTagName("Run");
            if(runNodeList.getLength() == 1){
                this.runConfigXmlNode = runNodeList.item(0);
            }
        }

        File intensityConfigFile = new File(this.intensityConfig);
        
        if (intensityConfigFile.exists()) {
            try {
                intensityConfigDoc = db.parse(intensityConfigFile);
            } catch (SAXException ex) {
                log.error(ex, "Problems to parsing intensity config xml file " + this.intensityConfig);
            } catch (IOException ex) {
                log.error(ex, "Problems to read intensity config xml file " + this.intensityConfig);
            }
            if( this.runConfigXmlNode == null ){
                NodeList runNodeList = intensityConfigDoc.getElementsByTagName("Run");
                if(runNodeList.getLength() == 1){
                   this.runConfigXmlNode = runNodeList.item(0);
                }
            }
        }
   
        if( this.runConfigXmlNode == null ){
            throw new RuntimeException("Both Intensities and BassCalls config files are not available or format wrong");
        }

    }
    /**
     * read base calls configure XML file
     * 
     * @throws Exception
     */
    private void readBaseCallsConfig() throws Exception {
        
        log.info("Reading BaseCalls config xml file " + this.baseCallsConfig);

        if (baseCallsConfigDoc == null) {
            log.info("Problems to read baseCalls config file: " + this.baseCallsConfig);
            this.baseCallProgram = new SAMProgramRecord("basecalling");
            return;
        }

        //read basecall software name and version
        this.baseCallProgram = this.readBaseCallProgramRecord();
        if(baseCallProgram == null){
            throw new Exception("Problems to get base call software version from config file: " + this.baseCallsConfig);
        }else{
            log.info("BaseCall Program: " + baseCallProgram.getProgramName() + " " + baseCallProgram.getProgramVersion());
        }

    }

    /**
     * read intensity configure XML file
     * @throws Exception
     */
    private void readIntensityConfig() throws Exception {

        log.info("Reading intensity config XML file " + this.intensityConfig );
        
        if (intensityConfigDoc == null) {
            log.info("Intensity config xml file is not available");
            this.instrumentProgram = new SAMProgramRecord("SCS");
            return;
        }

        //read instrument software name and version
        this.instrumentProgram = this.readInstrumentProgramRecord();
        if(instrumentProgram == null){
            throw new Exception("Problems to get instrument software version from config file: " + this.intensityConfig);
        }else{
            log.info("Instrument Program: " + instrumentProgram.getProgramName() + " " + instrumentProgram.getProgramVersion());
        }

    }
    
    private void readRunConfig() throws Exception{
        
        log.info("Reading run config from BaseCalls or Intensities");
        //read tile list
        this.tileList = this.readTileList();
        if(tileList == null || tileList.length == 0){
            this.tileList = this.readTileRange();
        }
        if(tileList == null){
            throw new RuntimeException("Problems to read tile list from config file:" + this.baseCallsConfig);
        }else{
            log.info("Number of Tiles: " + tileList.length);
        }

        this.id = this.readInstrumentAndRunID();
        if(id == null){
            log.warn("Problems to read run id and instrument name from config file:" + this.baseCallsConfig);
            this.id = "";
        }else{
            log.info("Instrument name and run id to be used as part of read name: " + this.id );
        }

        log.info("Check number of Reads and cycle numbers for each read");
        this.cycleRangeByRead = this.checkCycleRangeByRead();
        
        this.runfolderConfig = this.readRunfoder();
        if(this.runfolderConfig != null ){
            log.info("Runfolder: " + runfolderConfig);
        }
        
        this.runDateConfig = this.readRunDate();
        if(this.runDateConfig != null){
            log.info("Run date: " + runDateConfig);
        }

    }
    
    /**
     *
     * @return an object of SAMProgramRecord for base calling program
     */
    public SAMProgramRecord readBaseCallProgramRecord (){

        Node nodeSoftware;
        try {
            XPathExpression exprBaseCallSoftware = xpath.compile("/BaseCallAnalysis/Run/Software");
            nodeSoftware = (Node) exprBaseCallSoftware.evaluate(baseCallsConfigDoc, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            log.error(ex, "Problems to read base calling program /BaseCallAnalysis/Run/Software");
            return null;
        }

        NamedNodeMap nodeMapSoftware = nodeSoftware.getAttributes();
        String softwareName = nodeMapSoftware.getNamedItem("Name").getNodeValue();
        String softwareVersion = nodeMapSoftware.getNamedItem("Version").getNodeValue();
    
        if(softwareName == null || softwareVersion == null){
            log.warn("No base calling program name or version returned");
        }
        
        SAMProgramRecord baseCallProgramConfig = new SAMProgramRecord("basecalling");
        baseCallProgramConfig.setProgramName(softwareName);
        baseCallProgramConfig.setProgramVersion(softwareVersion);
        baseCallProgramConfig.setAttribute("DS", "Basecalling Package");

        return baseCallProgramConfig;
    }

    /**
     *
     * @return a list of tile number
     */
    public int[] readTileList() {

        int[] tileListConfig = null;
        
        NodeList tilesForLane;
        try {
            XPathExpression exprLane = xpath.compile("TileSelection/Lane[@Index=" + this.laneNumber + "]/Tile/text()");
            tilesForLane = (NodeList) exprLane.evaluate(this.runConfigXmlNode, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            log.error(ex, "Problems to got a list of tiles from config files." );
            return null;
        }       
        tileListConfig = new int[tilesForLane.getLength()];
        for (int i = 0; i < tilesForLane.getLength(); i++) {
            Node tile = tilesForLane.item(i);
            tileListConfig[i] = Integer.parseInt(tile.getNodeValue());
        }
        
        //TODO: the order of tile numbers
        Arrays.sort(tileListConfig);

        //TODO: <Sample>s</Sample> used in filename?

        return tileListConfig;
    }

    public int[] readTileRange() {

        int[] tileRangeConfig = null;
        
        NodeList tileRangeList;
        try {
            XPathExpression exprLane = xpath.compile("TileSelection/Lane[@Index=" + this.laneNumber + "]/TileRange");
            tileRangeList = (NodeList) exprLane.evaluate(this.runConfigXmlNode, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            log.error(ex, "Problems to got a list of tiles from config files." );
            return null;
        }
        
        ArrayList<Integer> tileArrayList = new ArrayList<Integer>(); 
        for (int j = 0; j < tileRangeList.getLength(); j++) {
            
            Node tileRangeForLane = tileRangeList.item(j);
            NamedNodeMap rangeAttributes = tileRangeForLane.getAttributes();
            int minTileNumber = Integer.parseInt(rangeAttributes.getNamedItem("Min").getNodeValue());
            int maxTileNumber = Integer.parseInt(rangeAttributes.getNamedItem("Max").getNodeValue());

            int numberOfTiles = maxTileNumber - minTileNumber + 1;
            for (int i = 0; i < numberOfTiles; i++) {
                tileArrayList.add(minTileNumber + i);
            }
        }

        tileRangeConfig = new int [tileArrayList.size()];
        int i = 0;
        for(int tileNumber : tileArrayList){
            tileRangeConfig[i] = tileNumber;
            i++;
        }
        return tileRangeConfig;
    }
    /**
     *
     * @return instrument name with id_run as part of read name, for example, HS13_6000
     */
    public String readInstrumentAndRunID(){

        Node nodeRunID;
        Node nodeInstrument;

        try {
            XPathExpression exprRunID = xpath.compile("RunParameters/RunFolderId/text()");
            nodeRunID = (Node) exprRunID.evaluate(this.runConfigXmlNode, XPathConstants.NODE);

            XPathExpression exprInstrument = xpath.compile("RunParameters/Instrument/text()");
            nodeInstrument = (Node) exprInstrument.evaluate(this.runConfigXmlNode, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            log.error("Problems to read instrument name and id run from config file: " + ex.getMessage() );
            return null;
        }

        String runID = nodeRunID.getNodeValue();
        String instrument = nodeInstrument.getNodeValue();
        if(runID == null || instrument ==null){
            log.warn("No instrument name or id run returned.");
            return null;
        }
        return instrument + "_" + runID;
    }

    /**
     *
     * @return the cycle range for each read
     * 
     * @throws Exception
     */
    public HashMap<String, int[]> checkCycleRangeByRead() throws Exception {
        
        HashMap<String, int[]> cycleRangeByReadMap = new HashMap<String, int[]>();

        int [][] cycleRangeByReadConfig = this.readCycleRangeByRead();
        int [] barCodeCycleList = this.readBarCodeIndexCycles();

        int numberOfReads = cycleRangeByReadConfig.length;
        log.info("There are " + numberOfReads + " reads returned");

        if( (numberOfReads  > 3
                || numberOfReads <1
                ||(barCodeCycleList == null && numberOfReads >2))){
            throw new RuntimeException("Problems with number of reads in config file: " + numberOfReads);
        }

        int countActualReads = 0;
        boolean indexReadFound = false;

        for(int i = 0; i < numberOfReads; i++){
            int firstCycle = cycleRangeByReadConfig[i][0];
            int lastCycle  = cycleRangeByReadConfig[i][1];
            int readLength = lastCycle - firstCycle + 1;
            if(barCodeCycleList != null
                    && firstCycle == barCodeCycleList[0]
                    && readLength == barCodeCycleList.length){
                
                indexReadFound = true;
                cycleRangeByReadMap.put("readIndex", cycleRangeByReadConfig[i]);
                log.info("Indexing read cycle range: " + cycleRangeByReadConfig[i][0] + "-" +cycleRangeByReadConfig[i][1] );
            }else{
                countActualReads++;
                cycleRangeByReadMap.put("read"+ countActualReads, cycleRangeByReadConfig[i]);
                log.info("read"+ countActualReads + " cycle range: " + cycleRangeByReadConfig[i][0] + "-" +cycleRangeByReadConfig[i][1] );
            }
        }

        if( !indexReadFound && barCodeCycleList != null ) {
            throw new RuntimeException("Barcode cycle not found in read list");
        }

        return cycleRangeByReadMap;
    }

    /**
     *
     * @return cycle range for a list of reads
     */
    public int [][] readCycleRangeByRead(){

        log.info("Reading cycle numbers for each read");
        
        int [][] cycleRangeByReadConfig = null;
        NodeList readList = null;
        try {
            XPathExpression exprReads = xpath.compile("RunParameters/Reads");
            readList = (NodeList) exprReads.evaluate(this.runConfigXmlNode, XPathConstants.NODESET);

            cycleRangeByReadConfig = new int [readList.getLength()][2];

            for(int i = 0; i<readList.getLength(); i++){

                Node readNode = readList.item(i);

                int readIndex = Integer.parseInt(readNode.getAttributes().getNamedItem("Index").getNodeValue());
                readIndex--;

                XPathExpression exprFirstCycle= xpath.compile("FirstCycle/text()");
                Node firstCycleNode = (Node) exprFirstCycle.evaluate(readNode, XPathConstants.NODE);
                cycleRangeByReadConfig[readIndex][0] = Integer.parseInt(firstCycleNode.getNodeValue());

                XPathExpression exprLastCycle= xpath.compile("LastCycle/text()");
                Node lastCycleNode = (Node) exprLastCycle.evaluate(readNode, XPathConstants.NODE);
                cycleRangeByReadConfig[readIndex][1] = Integer.parseInt(lastCycleNode.getNodeValue());
            }

        } catch (XPathExpressionException ex) {
            log.error(ex, "Problems to read cycles numbers");
            return null;
        }
        return cycleRangeByReadConfig;
    }

    /**
     *
     * @return indexing cycle number list
     */
    public int [] readBarCodeIndexCycles(){
        
        log.info("Reading barcode indexing cycle numbers");
        
        int [] barCodeCycleList = null;
        try {
            XPathExpression exprBarCode = xpath.compile("RunParameters/Barcode/Cycle/text()");
            NodeList barCodeNodeList = (NodeList) exprBarCode.evaluate(this.runConfigXmlNode, XPathConstants.NODESET);
            if(barCodeNodeList.getLength() == 0){
                return null;
            }
            barCodeCycleList = new int[barCodeNodeList.getLength()];
            for(int i=0; i<barCodeNodeList.getLength(); i++){
                barCodeCycleList[i] = Integer.parseInt(barCodeNodeList.item(i).getNodeValue());
            }
            Arrays.sort(barCodeCycleList);
        } catch (XPathExpressionException ex) {
            log.info("There is no bar code cycle");
        }
        return barCodeCycleList;
    }

    /**
     * 
     * @return instrument Program record
     */
    public SAMProgramRecord readInstrumentProgramRecord(){

        Node nodeSoftware;
        try {
            XPathExpression exprBaseCallSoftware = xpath.compile("/ImageAnalysis/Run/Software");
            nodeSoftware = (Node) exprBaseCallSoftware.evaluate(intensityConfigDoc, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            log.error(ex, "Problems to read instrument software");
            return null;
        }

        NamedNodeMap nodeMapSoftware = nodeSoftware.getAttributes();
        String softwareName = nodeMapSoftware.getNamedItem("Name").getNodeValue();
        String softwareVersion = nodeMapSoftware.getNamedItem("Version").getNodeValue();
        
        if(softwareName == null || softwareVersion == null){
            log.warn("No instrument software name or version returned");
        }

        SAMProgramRecord instrumentProgramConfig = new SAMProgramRecord("SCS");
        instrumentProgramConfig.setProgramName(softwareName);
        instrumentProgramConfig.setProgramVersion(softwareVersion);
        instrumentProgramConfig.setAttribute("DS", "Controlling software on instrument");

        return instrumentProgramConfig;
    }
    
    /**
     * 
     * @return  runfolder name
     */
    public String readRunfoder(){
        
        String runfolder = null;
        try {
            XPathExpression exprRunfolder = xpath.compile("RunParameters/RunFolder/text()");
            Node runfolderNode = (Node) exprRunfolder.evaluate(this.runConfigXmlNode, XPathConstants.NODE);
            if(runfolderNode == null){
                return null;
            }
            runfolder = runfolderNode.getNodeValue();
        } catch (XPathExpressionException ex) {
            log.warn(ex, "Problems to read runfolder");
        }
        
        return runfolder;
    }
    
    /**
     *  
     * @return run date
     */
    public Date readRunDate(){

        Date runDate = null;
        try {
            XPathExpression exprRunDate = xpath.compile("RunParameters/RunFolderDate/text()");
            Node runDateNode = (Node) exprRunDate.evaluate(this.runConfigXmlNode, XPathConstants.NODE);
            if(runDateNode == null){
                return null;
            }
            String runDateString = runDateNode.getNodeValue();
            SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
            runDate = formatter.parse(runDateString);
        } catch (ParseException ex) {
            log.warn(ex, "Problems parsing run date");
        } catch (XPathExpressionException ex) {
            log.warn(ex, "Problems reading run date");
        }  

        return runDate;
    }
        
    /**
     * 
     * @return BAM header
     */
    public SAMFileHeader generateHeader(){
        
         log.info("Generate BAM header");
         SAMFileHeader header = new SAMFileHeader();

         header.addProgramRecord(this.instrumentProgram);

         this.baseCallProgram.setPreviousProgramGroupId(this.instrumentProgram.getId());
         header.addProgramRecord(baseCallProgram);
         
         if(this.illumina2bamProgram != null){
             this.illumina2bamProgram.setPreviousProgramGroupId(this.baseCallProgram.getId());
             header.addProgramRecord(this.illumina2bamProgram);
         }

         if(this.readGroup != null){
           header.addReadGroup(readGroup);
         }

         return header;
    }

    /**
     * 
     * @param firstTile first tile number
     * 
     * @param tileLimit the number of tiles to process
     */
    public void reduceTileList(Integer firstTile, Integer tileLimit){

        ArrayList<Integer> reducedTileList = new ArrayList<Integer>();

        for (int tileNumber : this.tileList){
            if( tileNumber >= firstTile.intValue() ){
                reducedTileList.add(tileNumber);
            }
        }

        if(reducedTileList.isEmpty()){
            throw new RuntimeException("The Given first tile number " + firstTile + " was not found.");
        }

        List<Integer> limitedTileList = reducedTileList;
        if(tileLimit != null && tileLimit > 0){
            if(tileLimit > reducedTileList.size()){
                throw new RuntimeException("The Given first tile limit " + tileLimit + " was too big.");
            }
            limitedTileList = reducedTileList.subList(0, tileLimit.intValue());
        }

        int [] newTileList = new int[limitedTileList.size()];
        int i = 0;
        for(Integer tileNumber : limitedTileList){
            newTileList[i] = tileNumber.intValue();
            i++;
        }

        this.tileList = newTileList;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param cycleRangeByRead the cycleRangeByRead to set
     */
    public void setCycleRangeByRead(HashMap<String, int[]> cycleRangeByRead) {
        this.cycleRangeByRead = cycleRangeByRead;
    }

    /**
     * @param tileList the tileList to set
     */
    public void setTileList(int[] tileList) {
        this.tileList = tileList;
    }

    /**
     * @return the tileList
     */
    public int[] getTileList() {
        return tileList;
    }

    /**
     * @return the illumina2bamProgram
     */
    public SAMProgramRecord getIllumina2bamProgram() {
        return illumina2bamProgram;
    }

    /**
     * @param illumina2bamProgram the illumina2bamProgram to set
     */
    public void setIllumina2bamProgram(SAMProgramRecord illumina2bamProgram) {
        this.illumina2bamProgram = illumina2bamProgram;
    }

    /**
     * @param readGroup the readGroup to set
     */
    public void setReadGroup(SAMReadGroupRecord readGroup) {
        this.readGroup = readGroup;
    }

    /**
     * @return the runDateConfig
     */
    public Date getRunDateConfig() {
        return runDateConfig;
    }

    /**
     * @return the runfolderConfig
     */
    public String getRunfolderConfig() {
        return runfolderConfig;
    }

    /**
     * @return the baseCallProgram
     */
    public SAMProgramRecord getBaseCallProgram() {
        return baseCallProgram;
    }

    /**
     * @return the instrumentProgram
     */
    public SAMProgramRecord getInstrumentProgram() {
        return instrumentProgram;
    }
}