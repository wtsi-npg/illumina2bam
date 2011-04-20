/*
 *test class BCLFileReader
 */
package illumina;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Guoying Qi
 */
public class BCLFileReaderTest {

    private static String testBCLFile = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/s_1_1101.bcl";
    private static BCLFileReader bclFileReader;

    @BeforeClass
    public static void setUpClass() throws Exception {
        bclFileReader = new BCLFileReader(testBCLFile);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        bclFileReader.close();
    }

    @Test
    public void checkBCLHeaderOK() {

        assertEquals(bclFileReader.getTotalClusters(), 2609912);
        assertEquals(bclFileReader.getCurrentCluster(), 0);
        assertTrue(bclFileReader.hasNext());
    }

    @Test
    public void checkNextFirstClusterOK() {

        byte [] cluster = bclFileReader.next();
        assertEquals((char) cluster[0], 'N');
        assertEquals((char) (cluster[1]+ 64), 64);
        assertEquals(bclFileReader.getCurrentCluster(), 1);
        assertTrue(bclFileReader.hasNext());
    }

    @Test
    public void checkNextMiddleClusterOK() {

        for (int i = 0; i < 305; i++) {
            bclFileReader.next();
        }
        byte [] cluster = bclFileReader.next();
        assertEquals((char)cluster[0], 'A');
        assertEquals((char)(cluster[1] + 64 ), '^');
        assertEquals(bclFileReader.getCurrentCluster(), 307);
        assertEquals(bclFileReader.getTotalClusters(), 2609912);
        assertTrue(bclFileReader.hasNext());
    }

    @Test
    public void checkNextLastClusterOK() {

        byte[] cluster = null;

        while (bclFileReader.hasNext()) {
            cluster = bclFileReader.next();
        }
        assertEquals((char)cluster[0], 'G');
        assertEquals((char) (cluster[1] + 64), 'T');
        assertEquals(bclFileReader.getCurrentCluster(), 2609912);
        assertEquals(bclFileReader.getTotalClusters(), 2609912);
        assertFalse(bclFileReader.hasNext());
        assertEquals(bclFileReader.next(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkCorruptedFileReading() throws Exception{
        //total cluster in header is correct but the cluster in the scecond half are corrupted
        String testBCLFileCorrupt = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/BaseCalls/L003/C59.1/s_3_1101.bcl";
        BCLFileReader bclFileReaderCorrupt = new BCLFileReader(testBCLFileCorrupt);
        int totalCluster = bclFileReaderCorrupt.getTotalClusters();
        while( bclFileReaderCorrupt.getCurrentCluster() < totalCluster ) {
           byte[] cluster = bclFileReaderCorrupt.next();
        }
        bclFileReaderCorrupt.close();
    }

    @Test
    public void checkCorruptedFileReadingWringHeader() throws Exception{
        //total cluster in header is wrong
        String testBCLFileCorrupt = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/BaseCalls/L003/C59.1/s_3_1105.bcl";
        BCLFileReader bclFileReaderCorrupt = new BCLFileReader(testBCLFileCorrupt);
        int totalCluster = bclFileReaderCorrupt.getTotalClusters();
        assertEquals(totalCluster, 0);
        bclFileReaderCorrupt.close();
    }
}
