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
public class PosFileReaderTest {

    private static PosFileReader posFileReader = null;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Create a pos file reader");
        posFileReader = new PosFileReader("testdata/110519_IL33_06284/Data/Intensities/s_8_0112_pos.txt");
        assertNotNull(posFileReader);
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("Close the pos file reader");
        posFileReader.close();
    }
    
    @Test (expected= FileNotFoundException.class)
        public void testConstructorNotOK() throws FileNotFoundException, IOException{
        System.out.println("The given pos file is not available");
        PosFileReader posFileReader1 = new PosFileReader("testdata/110519_IL33_06284/Data/Intensities/s_8_0112_pos_not_exist.txt");
    }
    @Test
    public void testCountTotalClusters(){
        System.out.println("Check total cluster number correct");
        assertEquals(posFileReader.getTotalCluster(), 353693);
   
        System.out.println("Test next method");
        PositionFileReader.Position firstPos = posFileReader.next();
        assertEquals(firstPos.x, "1547");
        assertEquals(firstPos.y, "997");
        assertEquals(posFileReader.getCurrentTotalClusters(), 1);
        for(int i = 0; i< 353692; i++){
            posFileReader.next();
        }
        assertEquals(posFileReader.getCurrentTotalClusters(), 353693);
    
        System.out.println("Test no more next method");
        PositionFileReader.Position lastPos = posFileReader.next();
        assertNull(lastPos);
    }
}
