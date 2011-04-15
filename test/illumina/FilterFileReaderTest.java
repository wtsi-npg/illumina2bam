/*
 * test FilterFileReader class
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
public class FilterFileReaderTest {

    private static String testFilterFile = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/s_1_1101.filter";
    private static FilterFileReader filterFileReader;

    @BeforeClass
    public static void setUpClass() throws Exception {
        filterFileReader = new FilterFileReader(testFilterFile);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        filterFileReader.close();
    }

    @Test
    public void checkBCLHeaderOK() {

        assertEquals(filterFileReader.getTotalClusters(), 2609912);
        assertEquals(filterFileReader.getCurrentCluster(), 0);
        assertTrue(filterFileReader.hasNext());
    }

    @Test
    public void checkNextFirstClusterOK() {

        int filter = (Integer) filterFileReader.next();
        assertEquals(filter, 0);
        assertEquals(filterFileReader.getCurrentCluster(), 1);
        assertEquals(filterFileReader.getCurrentPFClusters(), 0);
        assertTrue(filterFileReader.hasNext());
    }

    @Test
    public void checkNextMiddleClusterOK() {

        for (int i = 0; i < 317; i++) {
            filterFileReader.next();
        }
        int filter = (Integer) filterFileReader.next();
        assertEquals(filter, 1);
        assertEquals(filterFileReader.getCurrentCluster(), 319);
        assertEquals(filterFileReader.getTotalClusters(), 2609912);
        assertEquals(filterFileReader.getCurrentPFClusters(), 264);
        assertTrue(filterFileReader.hasNext());
    }

    @Test
    public void checkNextLastClusterOK() {

        int filter = -1;

        while (filterFileReader.hasNext()) {
            filter = (Integer) filterFileReader.next();
        }
        assertEquals(filter, 0);
        assertEquals(filterFileReader.getCurrentCluster(), 2609912);
        assertEquals(filterFileReader.getTotalClusters(), 2609912);
        assertEquals(filterFileReader.getCurrentPFClusters(), 2425954);
        assertFalse(filterFileReader.hasNext());
        assertEquals(filterFileReader.next(), null);
    }
}
