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

package illumina.file.reader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import java.io.FileNotFoundException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Guoying Qi
 */
public class PosFileReaderTest {

    private static PosFileReader posFileReader = null;

    public PosFileReaderTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        posFileReader = new PosFileReader("testdata/110519_IL33_06284/Data/Intensities/s_8_0112_pos.txt");
        assertNotNull(posFileReader);
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        posFileReader.close();
    }
    
    @Test (expected= FileNotFoundException.class)
    public void testConstructorNotOK() throws FileNotFoundException{        
        PosFileReader posFileReader1 = new PosFileReader("testdata/110519_IL33_06284/Data/Intensities/s_8_0112_pos_not_exist.txt");
    }
    
    @Test
    public void testNext() throws FileNotFoundException{
        System.out.println("test next method");
        int [] firstPos = posFileReader.next();
        assertEquals(firstPos[0], 1547);
        assertEquals(firstPos[1], 997);
        assertEquals(posFileReader.getCurrentTotalClusters(), 1);
        for(int i = 0; i< 353692; i++){
            posFileReader.next();
        }
        assertEquals(posFileReader.getCurrentTotalClusters(), 353693);
    }
    
    @Test
    public void testNoMoreNext(){
        System.out.println("test no more next method");
        int [] firstPos = posFileReader.next();
        assertNull(firstPos);
    }
}
