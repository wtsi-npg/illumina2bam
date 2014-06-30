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
 * This is the test class for Tile
 * 
 */
package uk.ac.sanger.npg.illumina;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import org.junit.AfterClass;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class TileTest {

    private static String intensityDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities";
    private static String baseCallDir  = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls";
    private static String id = "HS13_6000";
    private static int lane = 1;
    private static int tileNumber = 1101;
    private static int[] cycleRangeRead1 = {1, 2};
    private static int[] cycleRangeRead2 = {51, 52};
    private static int[] cycleRangeIndex = {50, 50};
    private static Tile tile;

    private static String barcodeSeqTagName = "RT";
    private static String barcodeQualTagName = "QT";
    
    public TileTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
        System.out.println("Create a new tile");
        HashMap<String, int[]> cycleRangeByRead = new HashMap<String, int[]>(3);
        cycleRangeByRead.put("read1", cycleRangeRead1);
        cycleRangeByRead.put("read2", cycleRangeRead2);
        cycleRangeByRead.put("readIndex", cycleRangeIndex);
        tile = new Tile(intensityDir, baseCallDir, id, lane, tileNumber, cycleRangeByRead, true, true, barcodeSeqTagName, barcodeQualTagName);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        
        System.out.println("Close all basecall files");
        tile.closeBaseCallFiles();
    }

    @Test
    public void checkTileOK() throws Exception {
        
        System.out.println("getBaseCallFileName");
        assertEquals(tile.getBaseCallFileName(100, true), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C100.1/s_1_1101.bcl");
        assertEquals(tile.getBaseCallFileName(100, false), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C100.1/s_1_1101.scl");
        
        System.out.println("Checking filter file name in base call lane directory");
        assertEquals(tile.getFilterFileName(), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/s_1_1101.filter");
        
        System.out.println("Checking pos file name");
        assertEquals(tile.getPosFileName(), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/s_1_1101_pos.txt");
        
        System.out.println("Checking clocs file name");
        assertEquals(tile.getcLocsFileName(), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/L001/s_1_1101.clocs");
        
        System.out.println("Checking locs file name");
        assertEquals("testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/s.locs", tile.getLocsFileName());
    
        System.out.println("getReadName");
        String[] pos = {"21238", "9999"};
        assertEquals(tile.getReadName(pos), "HS13_6000:1:1101:21238:9999");
        
        System.out.println("getSAMRecord");
        byte [][] baseQuals = { {78, 71}, {33, 32}};
        String readName = "HS13_6000:1:1101:21238:9999";
        String secondBases = "AAA";
        byte [][] baseQualsIndex = {{84, 67}, {37 ,34} };
        SAMRecord record = tile.getSAMRecord(
                null, readName, 5, baseQuals, secondBases, baseQualsIndex, null, 0, true, true);
        String result = "HS13_6000:1:1101:21238:9999	589"
                + "	*	*	0	*	*	*	*	"
                + "NG	BA	E2:Z:AAA	RG:Z:1	QT:Z:FC	RT:Z:TC	ci:i:5";
        assertEquals(record.format(), result);
        
        System.out.println("getSAMRecord with dual index reads");
        byte [][] baseQuals1 = { {78, 71}, {33, 32}};
        String readName1 = "HS13_6000:1:1101:21238:9999";
        String secondBases1 = "AAA";
        byte [][] baseQualsIndex1 = {{84, 67}, {37 ,34} };
        byte [][] baseQualsIndex2 = {{84, 67}, {37 ,34} };
        tile.setSecondBarcodeSeqTagName("sb");
        tile.setSecondBarcodeQualTagName("qd");
        SAMRecord record1 = tile.getSAMRecord(
                null, readName1, 5, baseQuals1, secondBases1, baseQualsIndex1, baseQualsIndex2, 0, true, true);
        String result1 = "HS13_6000:1:1101:21238:9999	589	*	*	0	*	*	*	*	NG	BA	E2:Z:AAA	RG:Z:1	QT:Z:FC	RT:Z:TC	sb:Z:TC	qd:Z:FC	ci:i:5";
        assertEquals(record1.format(), result1);
        
        System.out.println("Check initial methods");
        assertEquals(tile.getcLocsFileName(), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/L001/s_1_1101.clocs");
        assertEquals(tile.getFilterFileName(), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/s_1_1101.filter");
        assertTrue(tile.isIndexed());
        assertTrue(tile.isPairedRead());
        assertEquals(tile.getBclFileReaderListByRead().size(), 0);
        assertEquals(tile.getSclFileReaderListByRead().size(), 0);
        
        System.out.println("Open basecall files");
        tile.openBaseCallFiles();
        assertEquals(tile.getBclFileReaderListByRead().size(), 3);
        assertEquals(tile.getSclFileReaderListByRead().size(), 3);
        assertEquals(tile.getBclFileReaderListByRead().get("read1").length, 2);
        assertEquals(tile.getBclFileReaderListByRead().get("read2").length, 2);
        assertEquals(tile.getBclFileReaderListByRead().get("readIndex").length, 1);

        System.out.println("Next Cluster methods");
        byte [][] read1 = tile.getNextClusterBaseQuals("read1");
        assertEquals(tile.convertByteArrayToString(read1[0]), "NG");
        assertEquals(tile.convertPhredQualByteArrayToFastqString(read1[1]), "!*");

        byte [][] read2 = tile.getNextClusterBaseQuals("read2");
        assertEquals(tile.convertByteArrayToString(read2[0]), "NN");
        assertEquals(tile.convertPhredQualByteArrayToFastqString(read2[1]), "!!");

        byte [][] readIndex = tile.getNextClusterBaseQuals("readIndex");
        assertEquals(tile.convertByteArrayToString(readIndex[0]), "N");
        assertEquals(tile.convertPhredQualByteArrayToFastqString(readIndex[1]), "!");

        System.out.println("ProcessTile");
        File tempBamFile = File.createTempFile("testNextCluster", ".bam", new File("testdata/"));
        tempBamFile.deleteOnExit();

        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileHeader header = new SAMFileHeader();
        SAMFileWriter outputSam = factory.makeSAMOrBAMWriter(header, true, tempBamFile);

        tile.openBaseCallFiles();
        tile.processTile(outputSam);
        outputSam.close();

        File md5File = new File(tempBamFile.getAbsolutePath() + ".md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals("0dbd4158a9d9dea6403daa285945113b", md5);        
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkCorruptedBCLFile() throws Exception {
        
        System.out.println("Corrupted bcl file");
        
        File tempBamFile = new File("testdata/testCorruptedBCL.bam");
        tempBamFile.deleteOnExit();

        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        SAMFileHeader header = new SAMFileHeader();
        SAMFileWriter outputSam = factory.makeSAMOrBAMWriter(header, true, tempBamFile);

        String intensityDir_2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities";
        String baseCallDir_2  = intensityDir_2 + File.separator + "BaseCalls";
        String id_2 = "HS17_6067";
        int lane_2 = 3;
        int tileNumber_2 = 1101;
        int[] cycleRangeRead_1 = {59, 59};

        HashMap<String, int[]> cycleRangeByRead = new HashMap<String, int[]>(1);
        cycleRangeByRead.put("read1", cycleRangeRead_1);

        Tile tile2 = new Tile(intensityDir_2, baseCallDir_2, id_2, lane_2, tileNumber_2, cycleRangeByRead, false, true, barcodeSeqTagName, barcodeQualTagName);
        tile2.openBaseCallFiles();
        tile2.processTile(outputSam);
        tile2.closeBaseCallFiles();
        outputSam.close();
    }

    @Test
    public void processAnotherTile() throws Exception {
        
        System.out.println("Process another tile from another run");
        
        File tempBamFile = new File("testdata/testAnotherTile.bam");
        tempBamFile.deleteOnExit();

        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileHeader header = new SAMFileHeader();
        SAMFileWriter outputSam = factory.makeSAMOrBAMWriter(header, true, tempBamFile);

        String intensityDir_2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities";
        String baseCallDir_2  = intensityDir_2 + File.separator + "BaseCalls";
        String id_2 = "HS17_6067";
        int lane_2 = 3;
        int tileNumber_2 = 1101;
        int[] cycleRangeRead_1 = {58, 58};

        HashMap<String, int[]> cycleRangeByRead = new HashMap<String, int[]>(1);
        cycleRangeByRead.put("read1", cycleRangeRead_1);

        Tile tile2 = new Tile(intensityDir_2, baseCallDir_2, id_2, lane_2, tileNumber_2, cycleRangeByRead, false, false, barcodeSeqTagName, barcodeQualTagName);
        tile2.openBaseCallFiles();
        tile2.processTile(outputSam);
        tile2.closeBaseCallFiles();
        outputSam.close();
        
        File md5File = new File(tempBamFile.getAbsolutePath() + ".md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals("9849624a131db28e5cd2af92c1064159", md5);        
    }
    
    @Test
    public void testGARun() throws IOException, Exception {
        System.out.println("Generating a GA tile");
        
        String intensityDirGA = "testdata/110519_IL33_06284/Data/Intensities";
        String baseCallDirGA = "testdata/110519_IL33_06284/Data/Intensities/BaseCalls/";
        String idGA = "IL33_6284";
        int laneGA = 8;
        int tileNumberGA = 112;
        int[] cycleRangeRead1GA = {10, 11};
        int[] cycleRangeRead2GA = {94, 95};
        int[] cycleRangeIndexGA = {77, 77};
        
        HashMap<String, int[]> cycleRangeByRead = new HashMap<String, int[]>(3);
        cycleRangeByRead.put("read1", cycleRangeRead1GA);
        cycleRangeByRead.put("read2", cycleRangeRead2GA);
        cycleRangeByRead.put("readIndex", cycleRangeIndexGA);
        
        Tile tileGA = new Tile(intensityDirGA, baseCallDirGA, idGA, laneGA, tileNumberGA, cycleRangeByRead, false, true, barcodeSeqTagName, barcodeQualTagName);
        
        assertEquals(tileGA.getFilterFileName(), "testdata/110519_IL33_06284/Data/Intensities/BaseCalls//s_8_0112.filter");
        
        File tempBamFileGA = new File("testdata/test_ga.bam");
        tempBamFileGA.deleteOnExit();
 
        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileHeader header = new SAMFileHeader();
        SAMFileWriter outputSam = factory.makeSAMOrBAMWriter(header, true, tempBamFileGA);
        
        System.out.println("Processing tiles");
        tileGA.openBaseCallFiles();
        tileGA.processTile(outputSam);
        tileGA.closeBaseCallFiles();
        outputSam.close();
        
        File md5File = new File(tempBamFileGA.getAbsolutePath() + ".md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();
        
        System.out.println("Checking bam md5 correct");
        assertEquals("2d432d23ac62bd201fae5b79c61fa8e7", md5);       
        
    }
}
