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
 * this is the test class for CLocsFileReader
 * 
 */
package illumina.file.reader;

import illumina.file.reader.CLocsFileReader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Guoying Qi
 */
public class CLocsFileReaderTest {

    private static String testCLocsFile = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/L001/s_1_1101.clocs";
    private static CLocsFileReader cLocsFileReader;

    @BeforeClass
    public static void setUpClass() throws Exception {
        cLocsFileReader = new CLocsFileReader(testCLocsFile);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        cLocsFileReader.close();
    }

    @Test
    public void checkBCLHeaderOK() {

        assertEquals(cLocsFileReader.getTotalBlocks(), 65600);
        assertEquals(cLocsFileReader.getCurrentBlock(), 1);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 0);
        assertTrue(cLocsFileReader.hasNext());
    }

    @Test
    public void checkNextFirstClusterOK() {

        String[] cluster = cLocsFileReader.next();
        assertEquals(cluster[0], "1235");
        assertEquals(cluster[1], "1989");
        assertEquals(cLocsFileReader.getCurrentBlock(), 247);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 1);
        assertTrue(cLocsFileReader.hasNext());
    }

    @Test
    public void checkNextMiddleClusterOK() {

        for (int i = 0; i < 305; i++) {
            cLocsFileReader.next();
        }
        String[] cluster = cLocsFileReader.next();
        assertEquals(cluster[0], "1279");
        assertEquals(cluster[1], "2120");
        assertEquals(cLocsFileReader.getCurrentBlock(), 330);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 307);
        assertTrue(cLocsFileReader.hasNext());
    }

    @Test
    public void checkNextLastClusterOK() {

        String[] cluster = null;

        while (cLocsFileReader.getCurrentTotalClusters() < 2609912) {
            cluster = cLocsFileReader.next();
        }
        assertEquals(cluster[0], "21324");
        assertEquals(cluster[1], "200731");
        assertEquals(cLocsFileReader.getCurrentBlock(), 65518);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 2609912);
        assertTrue(cLocsFileReader.hasNext());
    }

    @Test
    public void checkNextLastBlockOK() {

        String[] cluster = null;

        while (cLocsFileReader.hasNext()) {
            cluster = cLocsFileReader.next();
        }
        assertNull(cluster);
        assertEquals(cLocsFileReader.getCurrentBlock(), 65600);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 2609912);
    }

    @Test(expected = RuntimeException.class)
    public void checkAfterLastBlock() throws RuntimeException {
        assertFalse(cLocsFileReader.hasNext());
        assertNull(cLocsFileReader.next());
    }

    @Test
    public void testCorruptionFile2() throws Exception{
        //the BCL file corruped, but clocs file ok and some clusters in last block
       String testCLocsFile2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/L003/s_3_1101.clocs";
       CLocsFileReader cLocsFileReader2 = new CLocsFileReader(testCLocsFile2);
       while(cLocsFileReader2.hasNext()){
          cLocsFileReader2.next();
       }
       assertEquals(cLocsFileReader2.getCurrentTotalClusters(), 3658339);
       cLocsFileReader2.close();
    }
}
