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
 * This is the test class for Lane
 *
 */

package uk.ac.sanger.npg.illumina;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import net.sf.samtools.*;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 * @author dj3@sanger.ac.uk
 */
public class LaneTest {

    private static String intensityDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities";
    private static String baseCallDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls";
    private static String runfolderDir = "testdata/110323_HS13_06000_B_B039WABXX";
    private static int laneNumber = 1;
    private static boolean includeSecondCall = true;
    private static boolean pfFilter = true;
    private static String barcodeSeqTagName = "RT";
    private static String barcodeQualTagName = "QT";

    private static File output = new File("testdata/6000_1.bam");

    public LaneTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @Test (expected = RuntimeException.class)
    public void basecallDirectoryCheck(){
       
        System.out.println("Failed to create a lane with non-existent basecall directories");
        Lane failedLane = new Lane(intensityDir + "_not", baseCallDir + "_not", null, laneNumber, includeSecondCall, pfFilter, output, barcodeSeqTagName, barcodeQualTagName);
    }
    
    @Test
    public void readLaneOK(){
        
    	Lane lane = new Lane(intensityDir, baseCallDir, runfolderDir, laneNumber, includeSecondCall, pfFilter, output, barcodeSeqTagName, barcodeQualTagName);
        System.out.println("readBaseCallProgramRecord");
        SAMProgramRecord baseCallProgram = lane.readBaseCallProgramRecord();
        assertEquals( baseCallProgram.getId(), "basecalling");
        assertEquals( baseCallProgram.getProgramName(), "RTA");
        assertEquals( baseCallProgram.getProgramVersion(), "1.10.36.0");
        assertEquals( baseCallProgram.getAttribute("DS"), "Basecalling Package");
        
        System.out.println("readTileList");
        int [] expectedTileList = {
            1101,1102,1103,1104,1105,1106,1107,1108,
            1201,1202,1203,1204,1205,1206,1207,1208,
            2101,2102,2103,2104,2105,2106,2107,2108,
            2201,2202,2203,2204,2205,2206,2207,2208
        };
        assertArrayEquals(expectedTileList, lane.readTileList());

        System.out.println("reduceTileList");
        int [] givenTileList = {
            1101,1102,1103,1104,1105,1106,1107,1108,
            1201,1202,1203,1204,1205,1206,1207,1208,
            2101,2102,2103,2104,2105,2106,2107,2108,
            2201,2202,2203,2204,2205,2206,2207,2208
        };
        lane.setTileList(givenTileList);
        
        lane.reduceTileList(1102, 2);
        
        int [] newExpectedTileList = {1102,1103};
        assertArrayEquals(lane.getTileList(), newExpectedTileList);
    
        System.out.println("readRunfolder");
        assertEquals(lane.readRunfolder(), "110323_HS13_06000_B_B039WABXX");

        System.out.println("readRunDate");
        Date runDate = lane.readRunDate();
        long expected = 1300838400000L;
        assertEquals(runDate.getTime(), expected);

        System.out.println("readInstrumentAndRunID");
        assertEquals(lane.readInstrumentAndRunID(), "HS13_6000");

        System.out.println("readCycleRangeByRead");
        int[][] expectedRange = {
        		{1,2},
        		{51,52}
        };
        assertArrayEquals(lane.readCycleRangeByRead(), expectedRange);

        System.out.println("readBarCodeIndexCycles");
        assertNull(lane.readBarCodeIndexCycles());

    }

    @Test (expected = RuntimeException.class)
    public void reduceTileListTileException(){

    	Lane lane1 = new Lane(intensityDir, baseCallDir, runfolderDir, laneNumber, includeSecondCall, pfFilter, output, barcodeSeqTagName, barcodeQualTagName);
        System.out.println("Fail to reduceTileList");
        int [] givenTileList = {
            1101,1102,1103,1104,1105,1106,1107,1108,
            1201,1202,1203,1204,1205,1206,1207,1208,
            2101,2102,2103,2104,2105,2106,2107,2108,
            2201,2202,2203,2204,2205,2206,2207,2208
        };
        lane1.setTileList(givenTileList);

        lane1.reduceTileList(3308, 2);
    }
    
    @Test (expected = RuntimeException.class)
    public void reduceTileListTileAgainException(){
    	Lane lane1 = new Lane(intensityDir, baseCallDir, runfolderDir, laneNumber, includeSecondCall, pfFilter, output, barcodeSeqTagName, barcodeQualTagName);
    	System.out.println("Fail to reduceTileList again");
    	int [] newGivenTileList = {
    			1101,1102,1103,1104,1105,1106,1107,1108,
    			1201,1202,1203,1204,1205,1206,1207,1208,
    			2101,2102,2103,2104,2105,2106,2107,2108,
    			2201,2202,2203,2204,2205,2206,2207,2208
    	};
    	lane1.setTileList(newGivenTileList);

    	lane1.reduceTileList(1103, 33);
    }

    @Test
    public void checkCycleRangeByReadOK() throws Exception{
           	
    	Lane lane = new Lane(intensityDir, baseCallDir, runfolderDir, laneNumber, includeSecondCall, pfFilter, output, barcodeSeqTagName, barcodeQualTagName);
        System.out.println("checkCycleRangeByRead");
        HashMap<String, int[]> cycleRangeByRead = lane.checkCycleRangeByRead();
        int [] read1CycleRange = {1, 2};
        assertArrayEquals(cycleRangeByRead.get("read1"), read1CycleRange);
        int [] read2CycleRange = {51, 52};
        assertArrayEquals(cycleRangeByRead.get("read2"), read2CycleRange);
    
        System.out.println("getCycleRangeByReadFromRunInfoFile");
        HashMap<String, int[]> cycleRangeByRead1 = lane.getCycleRangeByReadFromRunInfoFile();
        assertNull(cycleRangeByRead1);
    }
    
    @Test
    public void readCycleRangeFromRunParameters(){

        System.out.println("Checking cycle range from run parameters file");
        
        String intensityDir2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities";
        String baseCallDir2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/BaseCalls";
        String runfolderDir2 = "testdata/110405_HS17_06067_A_B035CABXX";
        int laneNumber2 = 1;
        boolean includeSecondCall2 = true;
        boolean pfFilter2 = true;

        Lane lane2 = new Lane(intensityDir2, baseCallDir2, runfolderDir2, laneNumber2, includeSecondCall2, pfFilter2, output, barcodeSeqTagName, barcodeQualTagName);
    
        HashMap<String, int[]> cycleRangeByRead = lane2.getCycleRangeByReadFromRunParametersFile();
        
        assertEquals(cycleRangeByRead.keySet().size(), 3);
        
        int [] read1CycleRange = {1,100};
        assertArrayEquals(cycleRangeByRead.get("read1"), read1CycleRange);
        
        int [] read2CycleRange = {109,208};
        assertArrayEquals(cycleRangeByRead.get("read2"), read2CycleRange);
        
        int [] readIndex1CycleRange = {101,108};
        assertArrayEquals(cycleRangeByRead.get("readIndex"), readIndex1CycleRange);

        HashMap<String, int[]> cycleRangeByReadFromRunInfo = lane2.getCycleRangeByReadFromRunInfoFile();
        assertArrayEquals(cycleRangeByReadFromRunInfo.get("read1"), read1CycleRange);
        assertArrayEquals(cycleRangeByReadFromRunInfo.get("readIndex"), readIndex1CycleRange);

        SAMProgramRecord instrumentProgram = lane2.readInstrumentProgramRecordFromRunParameterFile();
        assertEquals( instrumentProgram.getId(), "SCS");
        assertEquals( instrumentProgram.getProgramName(), "HiSeq Control Software");
        assertEquals( instrumentProgram.getProgramVersion(), "1.5.15");
        assertEquals( instrumentProgram.getAttribute("DS"), "Controlling software on instrument");

    }
    
    @Test
    public void checkReadCycleRangeOK() throws Exception {
        
        System.out.println("Check read cycle range");
        
        String intensityDir2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities";
        String baseCallDir2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/BaseCalls";
        int laneNumber2 = 1;
        boolean includeSecondCall2 = true;
        boolean pfFilter2 = true;

        Lane lane2 = new Lane(intensityDir2, baseCallDir2, null, laneNumber2, includeSecondCall2, pfFilter2, output, barcodeSeqTagName, barcodeQualTagName);

        int [] barCodeCycleList = lane2.readBarCodeIndexCycles();
        int [] expected = {76,77,78,79,80,81,82,83};
        assertArrayEquals(barCodeCycleList, expected);

        HashMap<String, int[]> cycleRangeByRead = lane2.checkCycleRangeByRead();
        int [] read1CycleRange = {1, 75};
        assertArrayEquals(cycleRangeByRead.get("read1"), read1CycleRange);
        int [] read2CycleRange = {84, 158};
        assertArrayEquals(cycleRangeByRead.get("read2"), read2CycleRange);
        int [] readIndexCycleRange = {76, 83};
        assertArrayEquals(cycleRangeByRead.get("readIndex"), readIndexCycleRange);
        
    }

    @Test
    public void readInstrumentLaneHeaderOK() throws Exception{
    	
    	Lane lane = new Lane(intensityDir, baseCallDir, runfolderDir, laneNumber, includeSecondCall, pfFilter, output, barcodeSeqTagName, barcodeQualTagName);
        System.out.println("readInstrumentProgramRecord");
        SAMProgramRecord instrumentProgram = lane.readInstrumentProgramRecord();
        assertEquals( instrumentProgram.getId(), "SCS");
        assertEquals( instrumentProgram.getProgramName(), "RTA");
        assertEquals( instrumentProgram.getProgramVersion(), "1.10.36.0");
        assertEquals( instrumentProgram.getAttribute("DS"), "Controlling software on instrument");
    
        System.out.println("readConfigs");
        assertTrue(lane.readConfigs());

        System.out.println("Generate output bam header");
        SAMReadGroupRecord readGroup = new SAMReadGroupRecord("1");
        lane.setReadGroup(readGroup);

        SAMFileHeader header = lane.generateHeader();
        
        File tempBamFile = new File("testdata/testGenerateHeader.bam");
        tempBamFile.deleteOnExit();

        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileWriter outputSamHeader = factory.makeSAMOrBAMWriter(header, true, tempBamFile);

        outputSamHeader.close();

        File headerMd5File = new File(tempBamFile.getAbsolutePath() + ".md5");
        headerMd5File.deleteOnExit();
        BufferedReader headerMd5Stream = new BufferedReader(new FileReader(headerMd5File));
        String headerMd5 = headerMd5Stream.readLine();
        assertEquals("121501fc17f92ccc1360ccb7c6bb8762", headerMd5);
        	
        System.out.println("Generate output stream");

        SAMFileWriterFactory.setDefaultCreateMd5File(true);
        SAMFileWriter outputSam = lane.generateOutputSamStream();
        assertNotNull(outputSam);
        outputSam.close();
        output.deleteOnExit();

        File md5File = new File(output + ".md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();
        assertEquals("121501fc17f92ccc1360ccb7c6bb8762", md5);

        System.out.println("Process Tiles");

        String id = "HS13_6000";
        int[] cycleRangeRead1 = {1, 2};
        int[] cycleRangeRead2 = {51, 52};
        int[] cycleRangeIndex = {50, 50};
        int[] tileList = {1101};

        HashMap<String, int[]> cycleRangeByRead = new HashMap<String, int[]>(3);
        cycleRangeByRead.put("read1", cycleRangeRead1);
        cycleRangeByRead.put("read2", cycleRangeRead2);
        cycleRangeByRead.put("readIndex", cycleRangeIndex);

        lane.setCycleRangeByRead(cycleRangeByRead);
        lane.setId(id);
        lane.setTileList(tileList);
      
        File tempBamFile1 = new File("testdata/testProcessTiles.bam");
        tempBamFile1.deleteOnExit();

        SAMFileWriterFactory factory1 = new SAMFileWriterFactory();
        factory1.setCreateMd5File(true);
        SAMFileHeader header1 = new SAMFileHeader();
        SAMFileWriter outputSam1 = factory1.makeSAMOrBAMWriter(header1, true, tempBamFile1);

        assertTrue(lane.processTiles(outputSam1));

        outputSam1.close();

        File md5File1 = new File(tempBamFile1.getAbsolutePath() + ".md5");
        md5File1.deleteOnExit();
        BufferedReader md5Stream1 = new BufferedReader(new FileReader(md5File1));
        String md51 = md5Stream1.readLine();
        assertEquals("0dbd4158a9d9dea6403daa285945113b", md51);
    }

    @Test
    public void checkGARunOK() throws Exception {
        
        System.out.println("Test a GA run");
        
        String intensityDir3 = "testdata/110519_IL33_06284/Data/Intensities/";
        String baseCallDir3 = "testdata/110519_IL33_06284/Data/Intensities/BaseCalls";
        int laneNumber3 = 8;
        boolean includeSecondCall3 = false;
        boolean pfFilter3 = true;
        File output3 = new File("testdata/6284_8.bam");

        Lane lane3 = new Lane(intensityDir3, baseCallDir3, null, laneNumber3, includeSecondCall3, pfFilter3, output3, barcodeSeqTagName, barcodeQualTagName);

        int [] barCodeCycleList = lane3.readBarCodeIndexCycles();
        int [] expected = {77};
        assertArrayEquals(barCodeCycleList, expected);

        HashMap<String, int[]> cycleRangeByRead = lane3.checkCycleRangeByRead();
        int [] read1CycleRange = {10, 11};
        assertArrayEquals(cycleRangeByRead.get("read1"), read1CycleRange);
        int [] read2CycleRange = {94, 95};
        assertArrayEquals(cycleRangeByRead.get("read2"), read2CycleRange);
        int [] readIndexCycleRange = {77, 77};
        assertArrayEquals(cycleRangeByRead.get("readIndex"), readIndexCycleRange);
        
        lane3.readConfigs();
        int [] tileList = lane3.getTileList();
        assertEquals(tileList.length, 120);
        assertEquals(tileList[0], 1);
        
        assertEquals(lane3.getBaseCallProgram().getProgramName(), "Bustard");
        assertEquals(lane3.getBaseCallProgram().getProgramVersion(), "1.8.1a2");

    }
    
    @Test
    public void checkMiSeqRunOK() throws Exception {
        
        System.out.println("Test a MiSeq run");
        
        String intensityDir4 = "testdata/120110_M00119_0068_AMS0002022-00300/Data/Intensities";
        String baseCallDir4 = "testdata/120110_M00119_0068_AMS0002022-00300/Data/Intensities/BaseCalls";
        String runFolder = "testdata/120110_M00119_0068_AMS0002022-00300";
        int laneNumber4 = 1;
        boolean includeSecondCall4 = false;
        boolean pfFilter4 = true;
        File output4 = new File("testdata/miseq_1.bam");

        Lane lane4 = new Lane(intensityDir4, baseCallDir4, runFolder, laneNumber4, includeSecondCall4, pfFilter4, output4, barcodeSeqTagName, barcodeQualTagName);

        HashMap<String, int[]> cycleRangeByRead = lane4.getCycleRangeByReadFromRunParametersFile();
        assertEquals(cycleRangeByRead.size(), 4);
        
        int [] cycleRangeRead1 = {1,151};
        int [] cycleRangeRead2 = {168,318};
        int [] cycleRangeIndex = {152,159};
        int [] cycleRangeIndex2 = {160,167};
        assertArrayEquals(cycleRangeByRead.get("read1"), cycleRangeRead1);
        assertArrayEquals(cycleRangeByRead.get("read2"), cycleRangeRead2);
        assertArrayEquals(cycleRangeByRead.get("readIndex"), cycleRangeIndex);
        assertArrayEquals(cycleRangeByRead.get("readIndex2"), cycleRangeIndex2);
        
        lane4.readConfigs();
        
        cycleRangeByRead = lane4.getCycleRangeByRead();
        assertEquals(cycleRangeByRead.size(), 3);
        int [] cycleRangeMergedIndex = {152, 167};
        assertArrayEquals(cycleRangeByRead.get("readIndex"), cycleRangeMergedIndex);
        assertArrayEquals(cycleRangeByRead.get("read1"), cycleRangeRead1);
        assertArrayEquals(cycleRangeByRead.get("read2"), cycleRangeRead2);
        
        SAMProgramRecord instrumentProgram = lane4.readInstrumentProgramRecordFromRunParameterFile();
        assertEquals( instrumentProgram.getId(), "SCS");
        assertEquals( instrumentProgram.getProgramName(), "MiSeq Control Software");
        assertEquals( instrumentProgram.getProgramVersion(), "1.1.1");
        assertEquals( instrumentProgram.getAttribute("DS"), "Controlling software on instrument");

    }

    @Test
    public void readHiSeqXLaneOK(){
        String intensityDir = "testdata/140609_HX1_ValB_B_H04ENALXX/Data/Intensities";
        String baseCallDir = "testdata/140609_HX1_ValB_B_H04ENALXX/Data/Intensities/BaseCalls";
        String runfolderDir = "testdata/140609_HX1_ValB_B_H04ENALXX";
        int laneNumber = 1;
        boolean includeSecondCall = false;
        boolean pfFilter = true;
        String barcodeSeqTagName = "RT";
        String barcodeQualTagName = "QT";

    	Lane lane = new Lane(intensityDir, baseCallDir, runfolderDir, laneNumber, includeSecondCall, pfFilter, output, barcodeSeqTagName, barcodeQualTagName);
        System.out.println("readBaseCallProgramRecord");
        SAMProgramRecord baseCallProgram = lane.getBaseCallProgram();
        assertNotNull( "Creating basecalling SAMProgramRecord", baseCallProgram);
        assertEquals( "baseCallProgram.getId()", "basecalling", baseCallProgram.getId() );
        assertEquals( "baseCallProgram.getProgramName()", "RTA", baseCallProgram.getProgramName() );
        assertEquals( "baseCallProgram.getProgramVersion()", "2.2.1", baseCallProgram.getProgramVersion() );
        assertEquals( "baseCallProgram.getAttribute(\"DS\")", "Basecalling Package", baseCallProgram.getAttribute("DS") );
        System.out.println("getInstrumentProgram");
        SAMProgramRecord instrumentProgram = lane.getInstrumentProgram();
        assertNotNull( "Creating instrument SAMProgramRecord", instrumentProgram);
        assertEquals( "instrumentProgram.getId()", "SCS", instrumentProgram.getId() );
        assertEquals( "instrumentProgram.getProgramName()", "HiSeq X Control Software", instrumentProgram.getProgramName() );
        assertEquals( "instrumentProgram.getProgramVersion()", "3.0.29.0", instrumentProgram.getProgramVersion() );
        assertEquals( "instrumentProgram.getAttribute(\"DS\")", "Controlling software on instrument", instrumentProgram.getAttribute("DS") );
        
        System.out.println("getTileList");
        int [] expectedTileList = {
            1101,1102,1103,1104,1105,1106,1107,1108,1109,1110,1111,1112,1113,1114,1115,1116,1117,1118,1119,1120,1121,1122,1123,1124,
            1201,1202,1203,1204,1205,1206,1207,1208,1209,1210,1211,1212,1213,1214,1215,1216,1217,1218,1219,1220,1221,1222,1223,1224,
            2101,2102,2103,2104,2105,2106,2107,2108,2109,2110,2111,2112,2113,2114,2115,2116,2117,2118,2119,2120,2121,2122,2123,2124,
            2201,2202,2203,2204,2205,2206,2207,2208,2209,2210,2211,2212,2213,2214,2215,2216,2217,2218,2219,2220,2221,2222,2223,2224
        };
        assertArrayEquals("getTileList()", expectedTileList, lane.getTileList());

        System.out.println("reduceTileList");
        int [] givenTileList = {
            1101,1102,1103,1104,1105,1106,1107,1108,
            1201,1202,1203,1204,1205,1206,1207,1208,
            2101,2102,2103,2104,2105,2106,2107,2108,
            2201,2202,2203,2204,2205,2206,2207,2208
        };
        lane.setTileList(givenTileList);
        
        lane.reduceTileList(1102, 2);
        
        int [] newExpectedTileList = {1102,1103};
        assertArrayEquals( newExpectedTileList, lane.getTileList() );
    
        System.out.println("getRunfolderConfig");
        assertEquals("getRunfolderConfig()", "140609_HX1_ValB_B_H04ENALXX", lane.getRunfolderConfig() );

        System.out.println("getRunDateConfig");
        Date runDateConfig = lane.getRunDateConfig();
        long expected = 1402272000000L;
        assertEquals( "runDateConfig.getTime()", expected, runDateConfig.getTime() );

        System.out.println("readInstrumentAndRunID");
        assertEquals("lane.readInstrumentAndRunID()", null, lane.readInstrumentAndRunID());

        System.out.println("getCycleRangeByRead");
        int[][] expectedRange = {
        		{1,151},
        		{152,302}
        };
        HashMap<String,int[]> cycleRangeByRead = lane.getCycleRangeByRead();
        assertNotNull("getCycleRangeByRead()", cycleRangeByRead );
        assertArrayEquals(new int[]{1,151}, cycleRangeByRead.get("read1"));
        assertArrayEquals(new int[]{152,302}, cycleRangeByRead.get("read2"));
        assertNull(cycleRangeByRead.get("readIndex"));
        assertNull(cycleRangeByRead.get("readIndex2"));

    }

}
