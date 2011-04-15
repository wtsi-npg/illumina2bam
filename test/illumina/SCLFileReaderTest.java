/*
 * test class for SCLFileReader
 * 
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
public class SCLFileReaderTest {

    private static String testSCLFile = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/s_1_1101.scl";
    private static SCLFileReader sclFileReader;

    @BeforeClass
    public static void setUpClass() throws Exception {
        sclFileReader = new SCLFileReader(testSCLFile);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        sclFileReader.close();
    }

    @Test
    public void checkBCLHeaderOK() {

        assertEquals(sclFileReader.getTotalClusters(), 2609912);
        assertEquals(sclFileReader.getCurrentCluster(), 0);
        assertTrue(sclFileReader.hasNext());
    }

    @Test
    public void checkNextFirstClusterOK() {

        char cluster = sclFileReader.next();
        assertEquals(cluster, 'A');
        assertEquals(sclFileReader.getCurrentCluster(), 1);
        assertTrue(sclFileReader.hasNext());
    }

    @Test
    public void checkNextMiddleClusterOK() {

        for (int i = 0; i < 305; i++) {
            sclFileReader.next();
        }
        char cluster = sclFileReader.next();
        assertEquals(cluster, 'T');

        assertEquals(sclFileReader.getCurrentCluster(), 307);
        assertEquals(sclFileReader.getTotalClusters(), 2609912);
        assertTrue(sclFileReader.hasNext());
    }

    @Test
    public void checkNextLastClusterOK() {

        char cluster = ' ';

        while (sclFileReader.hasNext()) {
            cluster = sclFileReader.next();
        }
        assertEquals(cluster, 'C');
        assertEquals(sclFileReader.getCurrentCluster(), 2609912);
        assertEquals(sclFileReader.getTotalClusters(), 2609912);
        assertFalse(sclFileReader.hasNext());
        assertEquals(sclFileReader.next(), null);
    }
}
