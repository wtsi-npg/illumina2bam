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
 * The test class for IlluminaFileReader
 *
 */
package illumina.file.reader;

import illumina.file.reader.IlluminaFileReader;
import java.io.FileNotFoundException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Guoying Qi
 */
public class IlluminaFileReaderTest {

    private String testBCLDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/";
    private String testBCLFile = "s_1_1101.bcl";

    @Test
    public void testConstructorAndReadFourBytes() throws Exception {
        IlluminaFileReader fileReader = new IlluminaFileReader(testBCLDir + testBCLFile);
        assertEquals(fileReader.readFourBytes(), 2609912);
        fileReader.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorExpectedException3() throws Exception {
        IlluminaFileReader fileReader = new IlluminaFileReader(testBCLDir);
        assertNull(fileReader);
    }

    @Test(expected = FileNotFoundException.class)
    public void testConstructorExpectedException2() throws Exception {
        IlluminaFileReader fileReader = new IlluminaFileReader("nonexisted.file");
        assertNull(fileReader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorExpectedException1() throws Exception {
        IlluminaFileReader fileReader = new IlluminaFileReader(null);
        assertNull(fileReader);
    }
}
