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
package uk.ac.sanger.npg.illumina.file.reader;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class CLocsFileReaderTest {

    private static String testCLocsFile = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/L001/s_1_1101.clocs";
    private static CLocsFileReader cLocsFileReader;

    @Test
    public void checkClocsFileOK() {
    	System.out.println("Create a clocs file reader");
        try {
			cLocsFileReader = new CLocsFileReader(testCLocsFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        System.out.println("Check header");
        assertEquals(cLocsFileReader.getTotalBlocks(), 65600);
        assertEquals(cLocsFileReader.getCurrentBlock(), 1);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 0);
        assertTrue(cLocsFileReader.hasNext());
   
        System.out.println("Check first cluster");
        String[] cluster = cLocsFileReader.next().toArray();
        assertEquals(cluster[0], "1235");
        assertEquals(cluster[1], "1989");
        assertEquals(cLocsFileReader.getCurrentBlock(), 247);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 1);
        assertTrue(cLocsFileReader.hasNext());
   
        System.out.println("Check some more clusters and check 307th cluster");
        for (int i = 0; i < 305; i++) {
            cLocsFileReader.next();
        }
        cluster = cLocsFileReader.next().toArray();
        assertEquals(cluster[0], "1279");
        assertEquals(cluster[1], "2120");
        assertEquals(cLocsFileReader.getCurrentBlock(), 330);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 307);
        assertTrue(cLocsFileReader.hasNext());
  
        System.out.println("Read all clusters but still blocks not read");
        cluster = null;

        while (cLocsFileReader.getCurrentTotalClusters() < 2609912) {
            cluster = cLocsFileReader.next().toArray();
        }
        assertEquals(cluster[0], "21324");
        assertEquals(cluster[1], "200731");
        assertEquals(cLocsFileReader.getCurrentBlock(), 65518);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 2609912);
        assertTrue(cLocsFileReader.hasNext());
  
        System.out.println("No more cluster and block");
        PositionFileReader.Position cluster2 = null;

        while (cLocsFileReader.hasNext()) {
            cluster2 = cLocsFileReader.next();
        }
        assertNull(cluster2);
        assertEquals(cLocsFileReader.getCurrentBlock(), 65600);
        assertEquals(cLocsFileReader.getCurrentTotalClusters(), 2609912);
        
        System.out.println("Close the clocs file reader");
        cLocsFileReader.close();
    }

    @Test(expected = RuntimeException.class)
    public void checkAfterLastBlock() throws RuntimeException {
    	System.out.println("Create a clocs file reader");
        try {
			cLocsFileReader = new CLocsFileReader(testCLocsFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		PositionFileReader.Position cluster2 = null;
    	while (cLocsFileReader.hasNext()) {
            cluster2 = cLocsFileReader.next();
        }
    	assertNull(cluster2);
    	
        System.out.println("Try again after all blocks and cluster being read");
        assertFalse(cLocsFileReader.hasNext());
        assertNull(cLocsFileReader.next());
    }

    @Test
    public void testCorruptionFile2() throws Exception {
        //the BCL file corruped, but clocs file ok and some clusters in last block
        System.out.println("Try to read aother clocs file");
        String testCLocsFile2 = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/L003/s_3_1101.clocs";
        CLocsFileReader cLocsFileReader2 = new CLocsFileReader(testCLocsFile2);
        while (cLocsFileReader2.hasNext()) {
            cLocsFileReader2.next();
        }
        assertEquals(cLocsFileReader2.getCurrentTotalClusters(), 3658339);
        cLocsFileReader2.close();
    }
}
