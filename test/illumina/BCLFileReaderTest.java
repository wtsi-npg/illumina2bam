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

        char[] cluster = bclFileReader.next();
        assertEquals(cluster[0], '.');
        assertEquals(cluster[1], 'B');
        assertEquals(bclFileReader.getCurrentCluster(), 1);
        assertTrue(bclFileReader.hasNext());
    }

    @Test
    public void checkNextMiddleClusterOK() {

        for (int i = 0; i < 305; i++) {
            bclFileReader.next();
        }
        char[] cluster = bclFileReader.next();
        assertEquals(cluster[0], 'A');
        assertEquals(cluster[1], '^');
        assertEquals(bclFileReader.getCurrentCluster(), 307);
        assertEquals(bclFileReader.getTotalClusters(), 2609912);
        assertTrue(bclFileReader.hasNext());
    }

    @Test
    public void checkNextLastClusterOK() {

        char[] cluster = null;

        while (bclFileReader.hasNext()) {
            cluster = bclFileReader.next();
        }
        assertEquals(cluster[0], 'G');
        assertEquals(cluster[1], 'T');
        assertEquals(bclFileReader.getCurrentCluster(), 2609912);
        assertEquals(bclFileReader.getTotalClusters(), 2609912);
        assertFalse(bclFileReader.hasNext());
        assertEquals(bclFileReader.next(), null);
    }
}
