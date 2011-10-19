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
package illumina;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMRecord;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Guoying Qi
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

    public TileTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        HashMap<String, int[]> cycleRangeByRead = new HashMap<String, int[]>(3);
        cycleRangeByRead.put("read1", cycleRangeRead1);
        cycleRangeByRead.put("read2", cycleRangeRead2);
        cycleRangeByRead.put("readIndex", cycleRangeIndex);
        tile = new Tile(intensityDir, baseCallDir, id, lane, tileNumber, cycleRangeByRead, true, true);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        tile.closeBaseCallFiles();
    }

    @Test
    public void checkBclSclFileName() {
        assertEquals(tile.getBaseCallFileName(100, true), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C100.1/s_1_1101.bcl");
        assertEquals(tile.getBaseCallFileName(100, false), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C100.1/s_1_1101.scl");
    }
    
    @Test
    public void checkFilterFileName(){
        System.out.println("Checking filter file name in base call lane directory");
        assertEquals(tile.getFilterFileName(), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/s_1_1101.filter");
    }

    @Test
    public void checkReadName() {
        String[] pos = {"21238", "9999"};
        assertEquals(tile.getReadName(pos), "HS13_6000:1:1101:21238:9999");
    }

    @Test
    public void checkOneSAMRecord() {
        byte [][] baseQuals = { {78, 71}, {33, 32}};
        String readName = "HS13_6000:1:1101:21238:9999";
        String secondBases = "AAA";
        byte [][] baseQualsIndex = {{84, 67}, {37 ,34} };
        SAMRecord record = tile.getSAMRecord(
                null, readName, 5, baseQuals, secondBases, baseQualsIndex, 0, true, true);
        String result = "HS13_6000:1:1101:21238:9999	589"
                + "	*	*	0	*	*	*	*	"
                + "NG	BA	E2:Z:AAA	RG:Z:1	QT:Z:FC	RT:Z:TC	ci:i:5";
        assertEquals(record.format(), result);
    }

    @Test
    public void checkFullInitialMethods() throws Exception {
        assertEquals(tile.getcLocsFileName(), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/L001/s_1_1101.clocs");
        assertEquals(tile.getFilterFileName(), "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/s_1_1101.filter");
        assertTrue(tile.isIndexed());
        assertTrue(tile.isPairedRead());
        assertEquals(tile.getBclFileReaderListByRead().size(), 0);
        assertEquals(tile.getSclFileReaderListByRead().size(), 0);
    }

    @Test
    public void checkOpenBaseCallFiles() throws Exception {
        tile.openBaseCallFiles();
        assertEquals(tile.getBclFileReaderListByRead().size(), 3);
        assertEquals(tile.getSclFileReaderListByRead().size(), 3);
        assertEquals(tile.getBclFileReaderListByRead().get("read1").length, 2);
        assertEquals(tile.getBclFileReaderListByRead().get("read2").length, 2);
        assertEquals(tile.getBclFileReaderListByRead().get("readIndex").length, 1);
    }

    @Test
    public void checkNextClusterMethods() throws Exception {

        byte [][] read1 = tile.getNextClusterBaseQuals("read1");
        assertEquals(tile.convertByteArrayToString(read1[0]), "NG");
        assertEquals(tile.convertPhredQualByteArrayToFastqString(read1[1]), "!*");

        byte [][] read2 = tile.getNextClusterBaseQuals("read2");
        assertEquals(tile.convertByteArrayToString(read2[0]), "NN");
        assertEquals(tile.convertPhredQualByteArrayToFastqString(read2[1]), "!!");

        byte [][] readIndex = tile.getNextClusterBaseQuals("readIndex");
        assertEquals(tile.convertByteArrayToString(readIndex[0]), "N");
        assertEquals(tile.convertPhredQualByteArrayToFastqString(readIndex[1]), "!");
    }


    @Test
    public void checkProcessTileOK() throws Exception {

        File tempBamFile = File.createTempFile("test", ".bam", new File("testdata/"));
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

        assertEquals(md5, "3e256b176c26283991ce0704457f0d3d");        
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkCorruptedBCLFile() throws Exception {
        File tempBamFile = File.createTempFile("test", ".bam", new File("testdata/"));
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

        Tile tile2 = new Tile(intensityDir_2, baseCallDir_2, id_2, lane_2, tileNumber_2, cycleRangeByRead, false, true);
        tile2.openBaseCallFiles();
        tile2.processTile(outputSam);
        tile2.closeBaseCallFiles();
        outputSam.close();
    }

    @Test
    public void checkClocsFile() throws Exception {
        File tempBamFile = File.createTempFile("test", ".bam", new File("testdata/"));
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

        Tile tile2 = new Tile(intensityDir_2, baseCallDir_2, id_2, lane_2, tileNumber_2, cycleRangeByRead, false, false);
        tile2.openBaseCallFiles();
        tile2.processTile(outputSam);
        tile2.closeBaseCallFiles();
        outputSam.close();
        
        File md5File = new File(tempBamFile.getAbsolutePath() + ".md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals(md5, "7d72f9d8702d4b4440cc207e3d2f80a3");        
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
        
        Tile tileGA = new Tile(intensityDirGA, baseCallDirGA, idGA, laneGA, tileNumberGA, cycleRangeByRead, false, true);
        
        assertEquals(tileGA.getFilterFileName(), "testdata/110519_IL33_06284/Data/Intensities/BaseCalls//s_8_0112.filter");
        
        File tempBamFileGA = File.createTempFile("test_ga", ".bam", new File("testdata/"));
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
        assertEquals(md5, "d5dc58337bb8bb1494344338b5aead91");       
        
    }
}
