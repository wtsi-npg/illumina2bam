/*
 * this is the test class for Lane
 *
 */
package illumina;

import java.io.FileReader;
import java.io.BufferedReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileHeader;
import java.io.IOException;
import net.sf.samtools.SAMFileWriterFactory;
import java.io.File;
import net.sf.samtools.SAMProgramRecord;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gq1
 */
public class LaneTest {

    private static String intensityDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities";
    private static String baseCallDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls";
    private static int laneNumber = 1;
    private static boolean includeSecondCall = true;
    private static boolean pfFilter = true;

    private static File output = new File("testdata/6000_1.bam");

    private static Lane lane;

    public LaneTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {        
        lane = new Lane(intensityDir, baseCallDir, laneNumber, includeSecondCall, pfFilter, output);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        lane = null;
    }

    @Test
    public void readBaseCallProgramOK(){
        SAMProgramRecord baseCallProgram = lane.readBaseCallProgramRecord();
        assertEquals( baseCallProgram.getId(), "basecalling");
        assertEquals( baseCallProgram.getProgramName(), "RTA");
        assertEquals( baseCallProgram.getProgramVersion(), "1.10.36.0");
        assertEquals( baseCallProgram.getAttribute("DS"), "Basecalling Package");
    }

    @Test
    public void readTileListOK(){
        int [] expectedTileList = {
            1101,1102,1103,1104,1105,1106,1107,1108,
            1201,1202,1203,1204,1205,1206,1207,1208,
            2101,2102,2103,2104,2105,2106,2107,2108,
            2201,2202,2203,2204,2205,2206,2207,2208,
        };
        assertArrayEquals(expectedTileList, lane.readTileList());
    }

    @Test
    public void readInstrumentAndRunIDOK(){
        assertEquals(lane.readInstrumentAndRunID(), "HS13_6000");
    }

    @Test
    public void reaCycleRangeByReadOK(){
        int[][] expected = {
            {1,50},
            {51,100}
        };
        assertArrayEquals(lane.readCycleRangeByRead(), expected);
    }

    @Test
    public void reaBarCodeIndexCyclesNotOK(){
        assertNull(lane.readBarCodeIndexCycles());
    }

    @Test
    public void checkCycleRangeByReadOK() throws Exception{
        HashMap<String, int[]> cycleRangeByRead = lane.checkCycleRangeByRead();
        int [] read1CycleRange = {1, 50};
        assertArrayEquals(cycleRangeByRead.get("read1"), read1CycleRange);
        int [] read2CycleRange = {51, 100};
        assertArrayEquals(cycleRangeByRead.get("read2"), read2CycleRange);
    }
    
    @Test
    public void checkReadCycleRangeOK() throws Exception {
        String intensityDir2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities";
        String baseCallDir2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/BaseCalls";
        int laneNumber2 = 1;
        boolean includeSecondCall2 = true;
        boolean pfFilter2 = true;

        Lane lane2 = new Lane(intensityDir2, baseCallDir2, laneNumber2, includeSecondCall2, pfFilter2, output);

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
    public void readInstrumentProgramOK(){
        SAMProgramRecord instrumentProgram = lane.readInstrumentProgramRecord();
        assertEquals( instrumentProgram.getId(), "SCS");
        assertEquals( instrumentProgram.getProgramName(), "RTA");
        assertEquals( instrumentProgram.getProgramVersion(), "1.10.36.0");
        assertEquals( instrumentProgram.getAttribute("DS"), "Controlling software on instrument");
    }

    @Test
    public void readConfigsOK() throws Exception{
        assertTrue(lane.readConfigs());
    }

    @Test
    public void generateHeaderOK() throws Exception{

        SAMFileHeader header = lane.generateHeader();
        File tempBamFile = File.createTempFile("test", ".bam", new File("testdata/"));
        tempBamFile.deleteOnExit();

        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileWriter outputSam = factory.makeSAMOrBAMWriter(header, true, tempBamFile);

        outputSam.close();

        File md5File = new File(tempBamFile.getAbsolutePath() + ".md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();
        assertEquals(md5, "91006c6f261a94bd15896f3d0e8028bd");
    }

    @Test
    public void generateOutputSamStreamOK() throws Exception{
        SAMFileWriterFactory.setDefaultCreateMd5File(true);
        SAMFileWriter outputSam = lane.generateOutputSamStream();
        assertNotNull(outputSam);
        outputSam.close();
        output.deleteOnExit();

        File md5File = new File(output + ".md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();
        assertEquals(md5, "91006c6f261a94bd15896f3d0e8028bd");
    }

    @Test
    public void processTilesOK() throws IOException, Exception{

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

        File tempBamFile = File.createTempFile("test", ".bam", new File("testdata/"));
        tempBamFile.deleteOnExit();

        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileHeader header = new SAMFileHeader();
        SAMFileWriter outputSam = factory.makeSAMOrBAMWriter(header, true, tempBamFile);

        assertTrue(lane.processTiles(outputSam));

        outputSam.close();

        File md5File = new File(tempBamFile.getAbsolutePath() + ".md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();
        assertEquals(md5, "03785d1102b07a86d5b313a26e5cbb21");
    }
}
