/*
 * This is the test class for Tile
 */
package illumina;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;

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
        HashMap<String, int[]> cycleRangeByRead = new HashMap<String, int[]>(1);
        cycleRangeByRead.put("read1", cycleRangeRead1);
        cycleRangeByRead.put("read2", cycleRangeRead2);
        cycleRangeByRead.put("readIndex", cycleRangeIndex);
        tile = new Tile(intensityDir, id, lane, tileNumber, cycleRangeByRead, true, true);
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
    public void checkReadName() {
        String[] pos = {"21238", "9999"};
        assertEquals(tile.getReadName(pos), "HS13_6000:1:1101:21238:9999");
    }

    @Test
    public void checkOneSAMRecord() {
        String[] baseQuals = {".G", "BA"};
        String readName = "HS13_6000:1:1101:21238:9999";
        String secondBases = "AAA";
        String[] baseQualsIndex = {"TC", "FC"};
        SAMRecord record = tile.getSAMRecord(
                null, readName, 5, baseQuals, secondBases, baseQualsIndex, 0, true, true);
        String result = "HS13_6000:1:1101:21238:9999	581"
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

        String[] read1 = tile.getNextClusterBaseQuals("read1");
        assertEquals(read1[0], ".G");
        assertEquals(read1[1], "BI");

        String[] read2 = tile.getNextClusterBaseQuals("read2");
        assertEquals(read2[0], "..");
        assertEquals(read2[1], "BB");

        String[] readIndex = tile.getNextClusterBaseQuals("readIndex");
        assertEquals(readIndex[0], ".");
        assertEquals(readIndex[1], "B");
    }

    @Test
    public void checkProcessTileOK() throws Exception {

        File tempBamFile = File.createTempFile("test", ".bam");

        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        factory.setCreateMd5File(true);
        SAMFileHeader header = new SAMFileHeader();
        SAMFileWriter outputSam = factory.makeSAMOrBAMWriter(header, true, tempBamFile);

        tile.openBaseCallFiles();
        tile.processTile(outputSam);
        outputSam.close();

        File md5File = new File(tempBamFile.getAbsolutePath() + ".md5");
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals(md5, "491c39b683fbfcea52f595a53c3e4d6a");
        
        md5File.delete();
        tempBamFile.delete();
    }
}
