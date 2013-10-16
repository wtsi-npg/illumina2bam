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
 * the test class for BCLFileReader
 */
package uk.ac.sanger.npg.illumina.file.reader;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class BCLFileReaderTest {

    private static String testBCLFile = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/s_1_1101.bcl";
    private BCLFileReader bclFileReader;

    @Test
    public void checkBCLReaderOK() {
    	System.out.println("Create an bcl file reader");
        try {
			bclFileReader = new BCLFileReader(testBCLFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        System.out.println("Read bcl header");
        assertEquals(bclFileReader.getTotalClusters(), 2609912); 
        assertEquals(bclFileReader.getCurrentCluster(), 0); 
        assertTrue(bclFileReader.hasNext());
   
        System.out.println("Check first cluster");
        byte [] cluster = bclFileReader.next();
        assertEquals((char) cluster[0], 'N');
        assertEquals((char) (cluster[1]+ 64), 64);
        assertEquals(bclFileReader.getCurrentCluster(), 1);
        assertTrue(bclFileReader.hasNext());
  
        System.out.println("Read some more clusters and check the 307th one");
        for (int i = 0; i < 305; i++) {
            bclFileReader.next();
        }
        cluster = bclFileReader.next();
        assertEquals((char)cluster[0], 'A');
        assertEquals((char)(cluster[1] + 64 ), '^');
        assertEquals(bclFileReader.getCurrentCluster(), 307);
        assertEquals(bclFileReader.getTotalClusters(), 2609912);
        assertTrue(bclFileReader.hasNext());
   
        System.out.println("Read cluster until last one");
        
        cluster = null;

        while (bclFileReader.hasNext()) {
            cluster = bclFileReader.next();
        }
        assertEquals((char)cluster[0], 'G');
        assertEquals((char) (cluster[1] + 64), 'T');
        assertEquals(bclFileReader.getCurrentCluster(), 2609912);
        assertEquals(bclFileReader.getTotalClusters(), 2609912);
        assertFalse(bclFileReader.hasNext());
        assertNull(bclFileReader.next());
        
        System.out.println("Close the bcl file reader");
        bclFileReader.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkCorruptedFileReading() throws Exception{
        System.out.println("Read a corrupted bcl file ");
        //total cluster in header is correct but the clusters in the second half are corrupted
        String testBCLFileCorrupt = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/BaseCalls/L003/C59.1/s_3_1101.bcl";
        BCLFileReader bclFileReaderCorrupt = new BCLFileReader(testBCLFileCorrupt);
        int totalCluster = bclFileReaderCorrupt.getTotalClusters();
        while( bclFileReaderCorrupt.getCurrentCluster() < totalCluster ) {
           bclFileReaderCorrupt.next();
        }
        bclFileReaderCorrupt.close();
    }

    @Test
    public void checkCorruptedFileReadingWrongHeader() throws Exception{
        System.out.println("Read a corrupted bcl file the header with wrong information");
        //total cluster in header is wrong
        String testBCLFileCorrupt = "testdata/110405_HS17_06067_A_B035CABXX/Data/Intensities/BaseCalls/L003/C59.1/s_3_1105.bcl";
        BCLFileReader bclFileReaderCorrupt = new BCLFileReader(testBCLFileCorrupt);
        int totalCluster = bclFileReaderCorrupt.getTotalClusters();
        assertEquals(totalCluster, 0);
        bclFileReaderCorrupt.close();
    }
}
