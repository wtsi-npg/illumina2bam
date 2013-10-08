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
 * The test class of FilterFileReader class
 */
package uk.ac.sanger.npg.illumina.file.reader;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 * 
 */
public class FilterFileReaderTest {

    private static String testFilterFile = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/s_1_1101.filter";
    private static FilterFileReader filterFileReader;

    // Leave the @Before/AfterClass to see if these get threaded as well as @Test
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Create a filter file reader");
        filterFileReader = new FilterFileReader(testFilterFile);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("Close filter file reader");
        filterFileReader.close();
    }

    @Test
    public void checkBCLHeaderOK() {
        System.out.println("Read header");
        assertEquals(filterFileReader.getTotalClusters(), 2609912);
        assertEquals(filterFileReader.getCurrentCluster(), 0);
        assertTrue(filterFileReader.hasNext());
  
        System.out.println("Read first cluster");
        int filter = (Integer) filterFileReader.next();
        assertEquals(filter, 0);
        assertEquals(filterFileReader.getCurrentCluster(), 1);
        assertEquals(filterFileReader.getCurrentPFClusters(), 0);
        assertTrue(filterFileReader.hasNext());
   
        System.out.println("Read some more clusters and check 319th cluster");
        for (int i = 0; i < 317; i++) {
            filterFileReader.next();
        }
        filter = (Integer) filterFileReader.next();
        assertEquals(filter, 1);
        assertEquals(filterFileReader.getCurrentCluster(), 319);
        assertEquals(filterFileReader.getTotalClusters(), 2609912);
        assertEquals(filterFileReader.getCurrentPFClusters(), 264);
        assertTrue(filterFileReader.hasNext());
   
        System.out.println("Read all clusters till the last one");
        filter = -1;

        while (filterFileReader.hasNext()) {
            filter = (Integer) filterFileReader.next();
        }
        assertEquals(filter, 0);
        assertEquals(filterFileReader.getCurrentCluster(), 2609912);
        assertEquals(filterFileReader.getTotalClusters(), 2609912);
        assertEquals(filterFileReader.getCurrentPFClusters(), 2425954);
        assertFalse(filterFileReader.hasNext());
        assertNull(filterFileReader.next());
    }
    
    @Test
    public void checkGAFilterFileReading() throws Exception{
        System.out.println("Testing old format filter file");
        String gaFilterFile = "testdata/110519_IL33_06284/Data/Intensities/BaseCalls/s_8_0112.filter";
        FilterFileReader gaFilterFileReader = new FilterFileReader(gaFilterFile);
        assertEquals(gaFilterFileReader.getTotalClusters(), 353693);
        while(gaFilterFileReader.hasNext()){
           gaFilterFileReader.next();
        }
        assertEquals(gaFilterFileReader.getTotalClusters(), 353693);
        
        //TODO: This number of pf clusters from RTA possibly doesn't match the one from Bustard
        assertEquals(gaFilterFileReader.getCurrentPFClusters(), 308795);
    }
}
