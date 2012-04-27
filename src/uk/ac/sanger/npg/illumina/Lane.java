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
import java.util.Map.Entry;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import net.sf.picard.util.Log;
import net.sf.samtools.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    private String runFolder;
    private final int laneNumber;   

    //fields must be given about what to output
    private final boolean includeSecondCall;
    private final boolean pfFilter;

    //fields must be given for output bam
    private final File output;
    
    //fields for tag name
    private final String barcodeSeqTagName;
    private final String barcodeQualTagName;
    
    private String secondBarcodeSeqTagName;
    private String secondBarcodeQualTagName;

  
    //config xml file name and XML Documetns
    private final String baseCallsConfig;
    private final String intensityConfig ;
    private final String runParametersFile;
    private final String runInfoFile;
    
    private Document baseCallsConfigDoc = null;
    private Document intensityConfigDoc = null;
    private Document runParametersDoc = null;
    private Document runInfoDoc = null;
    
    //run config information from basecall config,
    //or from intersity config if not available there
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
     * @param runFolder Illumina runfolder directory, upwards two levels from Intensities directory if not given
     * @param laneNumber lane number
     * @param secondCall including second base call or not, default false.
     * @param pfFilter Filter cluster or not, default true.
     * @param output Output file
     * @param barcodeSeqTagName
     * @param barcodeQualTagName  
     */
    public Lane(String intensityDir,
                String baseCallDir,
                String runFolder,
                int laneNumber,
                boolean secondCall,
                boolean pfFilter,
                File output,
                String barcodeSeqTagName,
                String barcodeQualTagName){

        this.intensityDir      = intensityDir;
        this.baseCallDir       = baseCallDir;
        this.runFolder         = runFolder;
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
        
        if (this.runFolder != null) {

            this.runParametersFile = this.runFolder
                    + File.separator
                    + "runParameters.xml";
            this.runInfoFile = this.runFolder
                    + File.separator
                    + "RunInfo.xml";
        } else {
            this.runParametersFile = null;
            this.runInfoFile       = null;
        
        }

        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();

        this.initConfigsDoc();
    }

    /**
     * Read both config XML files under BaseCalls and Intensities.
     * And RunInfo and runParameters xml under runfolder.
     * 
     * @return true if successfully
     * @throws Exception
     */
    public boolean readConfigs() throws Exception{

        //read basecall program record from basecalls config file
        this.readBaseCallsConfig();
        
        //read instrument program from runParameter xml, try intensity config if not available
        this.readIntensityConfig();
        
        // read tile list, run id, run date, run folder and instrument from runConfigXmlNodes
        // get cycle range and read information from runInfo, or runParameters or runConfigXmlNodes. Try these files in order.
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
            
            if(this.secondBarcodeSeqTagName != null && this.secondBarcodeQualTagName != null){
                tile.setSecondBarcodeQualTagName(secondBarcodeQualTagName);
                tile.setSecondBarcodeSeqTagName(secondBarcodeSeqTagName);
            }
            
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
     * 
     * covert basecalls and intensity config file, runInfo and runParameters xml file into xml document
     * 
     * try to get runConfigXmlNode from  basecalls or intensity config file.
     * 
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
        
        this.runParametersDoc = this.fromXmlToDocument(this.runParametersFile, db);
        
        this.runInfoDoc       = this.fromXmlToDocument(this.runInfoFile, db);
        
    }
    
    private Document fromXmlToDocument(String xmlFileName, DocumentBuilder db) {

        Document doc = null;
        if (xmlFileName != null) {
            File xmlFileObj = new File(xmlFileName);
            if (xmlFileObj.exists()) {
                try {
                    doc = db.parse(xmlFileObj);
                } catch (SAXException ex) {
                    log.error(ex, "Problems to parsing runParameters xml file " + xmlFileName);
                } catch (IOException ex) {
                    log.error(ex, "Problems to read run parameters xml file " + xmlFileName);
                }
            } else {
                log.warn("XML file not exists " + xmlFileName);
            }
        }
        return doc;
    }

    /**
     * read base calls configure XML file for basecalls Program Record
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
     * read runParameters or intensity configure XML file for Instrument Program Record
     * @throws Exception
     */
    private void readIntensityConfig() throws Exception {

        if (intensityConfigDoc == null && this.runParametersDoc == null) {
            log.info("Intensity config xml file and runParameters xml file both are not available");
            this.instrumentProgram = new SAMProgramRecord("SCS");
            return;
        }

        //read instrument software name and version
        if(this.runParametersDoc != null){
            log.info("Reading runParameters XML file for instrument program record " + this.runParametersFile );
            this.instrumentProgram = this.readInstrumentProgramRecordFromRunParameterFile();
        }else{
            log.info("Reading intensity config XML file for instrument program record " + this.intensityConfig );
            this.instrumentProgram = this.readInstrumentProgramRecord();
        }
        
        if(instrumentProgram == null){
            throw new Exception("Problems to get instrument software version from config or runParameters file: " + this.intensityConfig);
        }else{
            log.info("Instrument Program: " + instrumentProgram.getProgramName() + " " + instrumentProgram.getProgramVersion());
        }

    }
    
    /**
     * read runConfigXmlNode for tile list, run id , instrument, run folder, run date
     * get cycle and read information from runInfo. Otherwise try runParameters file or runConfigXmlNode.
     * 
     * @throws Exception
     */   
    private void readRunConfig() throws Exception{
        
        log.info("Reading run config from RunInfo, runParameters, BaseCalls or Intensities files");
        
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
        
        this.runfolderConfig = this.readRunfoder();
        if(this.runfolderConfig != null ){
            log.info("Runfolder: " + runfolderConfig);
        }
        
        this.runDateConfig = this.readRunDate();
        if(this.runDateConfig != null){
            log.info("Run date: " + runDateConfig);
        }
        
        //try different file for cycle and read information
        if(this.runInfoDoc != null){
            
            log.info("Check cycle reange per read from RunInfo file");
            this.cycleRangeByRead = this.getCycleRangeByReadFromRunInfoFile();
        }
        
        if(this.cycleRangeByRead == null && this.runParametersDoc != null){
            
           log.info("Check cycle reange per read from runParameter file");
           this.cycleRangeByRead = this.getCycleRangeByReadFromRunParametersFile();
        }
        
        if(this.cycleRangeByRead == null){
        
           log.info("Check number of Reads and cycle numbers for each read");
           this.cycleRangeByRead = this.checkCycleRangeByRead();
        }
        
        if(this.cycleRangeByRead == null){
            throw new RuntimeException("Problems to get cycle and read infomation from config files");
        }
        
        this.mergeIndexReads();
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

    /**
     * 
     * @return
     */
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
     * @return
     */
    public HashMap<String, int[]> getCycleRangeByReadFromRunInfoFile() {
        if(this.runInfoDoc == null) {
            return null;
        }

        TreeMap<Integer, NamedNodeMap> readAttributesList = this.getReadInfoFromRunParametersOrRunInfoFile("RunInfo/Run/Reads/Read", this.runInfoDoc);
        if(readAttributesList == null || readAttributesList.isEmpty()){
            return null;
        }
        return this.readCycleRangeByReadMap(readAttributesList);
    }
        
    /**
     * 
     * @return
     */
    public HashMap<String, int[]> getCycleRangeByReadFromRunParametersFile(){
        
        HashMap<String, int[]> cycleRangeByReadMap;
        
        TreeMap<Integer,NamedNodeMap> readAttributesList;
        
        //for HiSeq run
        readAttributesList= this.getReadInfoFromRunParametersOrRunInfoFile("RunParameters/Setup/Reads/Read", this.runParametersDoc);
       
        if(readAttributesList== null || readAttributesList.isEmpty()){
            
            //if not, try MiSeq format
            readAttributesList = this.getReadInfoFromRunParametersOrRunInfoFile("RunParameters/Reads/RunInfoRead", this.runParametersDoc);            
        }
        
        cycleRangeByReadMap = this.readCycleRangeByReadMap(readAttributesList);
   
        return cycleRangeByReadMap;
    }
    
    private HashMap<String, int[]> readCycleRangeByReadMap(TreeMap<Integer, NamedNodeMap> readAttributesList) {
        
        HashMap<String, int[]> cycleRangeByReadMap = new HashMap<String, int[]>();
        
        int readCount = 0;
        int indexReadCount = 0;
        int cycleCount = 1;
        for (Entry<Integer, NamedNodeMap> entry : readAttributesList.entrySet()) {

            NamedNodeMap namedNodeMap = entry.getValue();

            int readNumCycles = Integer.parseInt(namedNodeMap.getNamedItem("NumCycles").getNodeValue());
            String isIndexedRead = namedNodeMap.getNamedItem("IsIndexedRead").getNodeValue();

            int[] cycleRange = {cycleCount, cycleCount + readNumCycles - 1};
            cycleCount += readNumCycles;

            if (!isIndexedRead.equalsIgnoreCase("Y")) {
                readCount++;
                cycleRangeByReadMap.put("read" + readCount, cycleRange);
            } else {
                indexReadCount++;
                String indexReadName = "readIndex";
                if (indexReadCount != 1) {
                    indexReadName += indexReadCount;
                }
                cycleRangeByReadMap.put(indexReadName, cycleRange);
            }
        }

        return cycleRangeByReadMap;

    }
    
    private void mergeIndexReads(){

        if( this.secondBarcodeSeqTagName != null && this.secondBarcodeQualTagName != null ){
            return;
        }

        int [] cycleRangeIndexRead2 = this.getCycleRangeByRead().get("readIndex2");
        if(cycleRangeIndexRead2 == null ){
            return;
        }
        
        int [] cycleRangeIndexRead = this.getCycleRangeByRead().get("readIndex");
        
        if(cycleRangeIndexRead[1] + 1 != cycleRangeIndexRead2[0]){
            throw new RuntimeException("Two indexing reads are not next each other");
        }
        
        int [] cycleRangeMergedInexRead = {cycleRangeIndexRead[0], cycleRangeIndexRead2[1] };
        this.getCycleRangeByRead().put("readIndex", cycleRangeMergedInexRead);
        this.getCycleRangeByRead().remove("readIndex2");
        
    }
    
    /**
     * From runParameters file:
     * "RunParameters/Setup/Reads/Read" for HiSeq, "RunParameters/Reads/RunInfoRead" for MiSeq and no file for GA
     * 
     * From RunInfo file: "RunInfo/Run/Reads/Read" only for HiSeq and MiSeq, not cope with GA
     * 
     * @param readInfoPath
     * 
     * @return
     */
    public TreeMap<Integer,NamedNodeMap> getReadInfoFromRunParametersOrRunInfoFile(String readInfoPath, Document xmlDoc){

        TreeMap<Integer, NamedNodeMap> readAttributesList = new TreeMap<Integer, NamedNodeMap>();

        try {
            XPathExpression exprReadList = xpath.compile(readInfoPath);
            NodeList readNodeList = (NodeList) exprReadList.evaluate(xmlDoc, XPathConstants.NODESET);
            for (int i = 0; i < readNodeList.getLength(); i++) {
                Node readNode = readNodeList.item(i);
                NamedNodeMap readAttributes = readNode.getAttributes();
                Node numberAttributeNode = readAttributes.getNamedItem("Number");
                if (numberAttributeNode == null) {
                    return null;
                } else {
                    int readNumber = Integer.parseInt(numberAttributeNode.getNodeValue());
                    readAttributesList.put(readNumber, readAttributes);
                }
            }
        }catch (XPathExpressionException ex) {
            log.error(ex, "Problems to get reads information from runParameters file based on xpath " + readInfoPath, ex);
        }

        return readAttributesList;
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

        Node nodeSoftware = null;
        try {
            XPathExpression exprBaseCallSoftware = xpath.compile("/ImageAnalysis/Run/Software");
            nodeSoftware = (Node) exprBaseCallSoftware.evaluate(intensityConfigDoc, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            log.error(ex, "Problems to read instrument software from intensity config file");
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
     * @return instrument Program record
     */
    public SAMProgramRecord readInstrumentProgramRecordFromRunParameterFile(){

        String applicationName = null;
        try {
            XPathExpression exprApplicationName = xpath.compile("/RunParameters/Setup/ApplicationName/text()");
            Node nodeApplicationName = (Node) exprApplicationName.evaluate(this.runParametersDoc, XPathConstants.NODE);
            applicationName = nodeApplicationName.getNodeValue();
        } catch (XPathExpressionException ex) {
            log.error(ex, "Problems to read instrument software from run parameter file");
        }

        String applicationVersion = null;
        try {
            XPathExpression exprApplicationVersion = xpath.compile("/RunParameters/Setup/ApplicationVersion/text()");
            Node nodeApplicationVersion = (Node) exprApplicationVersion.evaluate(this.runParametersDoc, XPathConstants.NODE);
            applicationVersion = nodeApplicationVersion.getNodeValue();
        } catch (XPathExpressionException ex) {
            log.error(ex, "Problems to read instrument software from run parameter file");
        }    
        
        if(applicationName == null || applicationVersion == null){
            log.warn("No instrument software name or version returned from run paramaters file");
        }

        SAMProgramRecord instrumentProgramConfig = new SAMProgramRecord("SCS");
        instrumentProgramConfig.setProgramName(applicationName);
        instrumentProgramConfig.setProgramVersion(applicationVersion);
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

    /**
     * @param secondBarcodeSeqTagName the secondBarcodeSeqTagName to set
     */
    public void setSecondBarcodeSeqTagName(String secondBarcodeSeqTagName) {
        this.secondBarcodeSeqTagName = secondBarcodeSeqTagName;
    }

    /**
     * @param secondBarcodeQualTagName the secondBarcodeQualTagName to set
     */
    public void setSecondBarcodeQualTagName(String secondBarcodeQualTagName) {
        this.secondBarcodeQualTagName = secondBarcodeQualTagName;
    }

    /**
     * @return the cycleRangeByRead
     */
    public HashMap<String, int[]> getCycleRangeByRead() {
        return cycleRangeByRead;
    }
}
