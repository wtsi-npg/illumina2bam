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
 * 
 */

package uk.ac.sanger.npg.illumina.file.reader;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class LocsFileReaderTest {

    private static LocsFileReader locsFileReader = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Creating a locs file reader");
        locsFileReader = new LocsFileReader("testdata/111014_M00119_0028_AMS0001310-00300/Data/Intensities/L001/s_1_1.locs");
        assertNotNull(locsFileReader);
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("Close the locs file reader");
        locsFileReader.close();
    }
    
    @Test (expected= FileNotFoundException.class)
    public void testConstructorNotOK() throws FileNotFoundException, Exception{
        System.out.println("creating a locs file reader using a non-exist file");
        LocsFileReader locsFileReader1 = new LocsFileReader("testdata/111014_M00119_0028_AMS0001310-00300/Data/Intensities/L001/s_1_1_nonexist.locs");
    }
    @Test
    public void testTotalNextCluster() {
        System.out.println("checking total cluster number and current total cluster number");
        assertEquals(locsFileReader.getTotalCluster(), 235085);
        assertEquals(locsFileReader.getCurrentTotalClusters(), 0);
  
        System.out.println("test next method");
        String [] firstPos = locsFileReader.next().toArray();
        assertEquals(firstPos[0], "16440");
        assertEquals(firstPos[1], "1321");
        assertEquals(locsFileReader.getCurrentTotalClusters(), 1);
        
        String [] lastPos = null;
        for(int i = 1; i< 235085; i++){
            lastPos = locsFileReader.next().toArray();
        }
        assertEquals(lastPos[0], "15605");
        assertEquals(lastPos[1], "29408");
        assertEquals(locsFileReader.getCurrentTotalClusters(), 235085);
    }
    
    @Test (expected= RuntimeException.class)
    public void testNoMoreNext() throws Exception {
        LocsFileReader locsFileReader2 = new LocsFileReader("testdata/111014_M00119_0028_AMS0001310-00300/Data/Intensities/L001/s_1_1.locs");
        assertNotNull(locsFileReader2);
        
        String [] lastPos = null;
        for(int i = 1; i< 235086; i++){
            lastPos = locsFileReader2.next().toArray();
        }
        
        System.out.println("test no more next method");
        locsFileReader2.next();
    }
}
