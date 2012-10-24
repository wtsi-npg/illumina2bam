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
package uk.ac.sanger.npg.illumina.file.reader;

import java.io.FileNotFoundException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class IlluminaFileReaderTest {

    private String testBCLDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/";
    private String testBCLFile = "s_1_1101.bcl.gz";
    private String testBCLFileNoSuffix = "s_1_1101.bcl";
    private String testBCLFileUncompressed = "s_1_1101_uncompressed.bcl";

    @Test
    public void testConstructorAndReadFourBytes() throws Exception {
        System.out.println("readFourBytes from gzip compressed BCL file");
        IlluminaFileReader fileReader = new IlluminaFileReader(testBCLDir + testBCLFile);
        assertEquals(fileReader.readFourBytes(), 2609912);
        fileReader.close();
    }

    @Test
    public void testConstructorAndReadFourBytesNoSuffix() throws Exception {
        System.out.println("readFourBytes from gzipped BCL, no .gz suffix in argument");
        IlluminaFileReader fileReader = new IlluminaFileReader(testBCLDir + testBCLFileNoSuffix);
        assertEquals(fileReader.readFourBytes(), 2609912);
        fileReader.close();
    }

    @Test
    public void testConstructorAndReadFourBytesUncompressed() throws Exception {
        System.out.println("readFourBytes from uncompressed BCL file");
        IlluminaFileReader fileReader = 
            new IlluminaFileReader(testBCLDir + testBCLFileUncompressed);
        assertEquals(fileReader.readFourBytes(), 2609912);
        fileReader.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorExpectedException3() throws Exception {
        System.out.println("The given file is a directory");
        IlluminaFileReader fileReader = new IlluminaFileReader(testBCLDir);
        assertNull(fileReader);
    }

    @Test(expected = FileNotFoundException.class)
    public void testConstructorExpectedException2() throws Exception {
        System.out.println("The given file doesn't exist");
        IlluminaFileReader fileReader = new IlluminaFileReader("nonexisted.file");
        assertNull(fileReader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorExpectedException1() throws Exception {
        System.out.println("No file given to read");
        IlluminaFileReader fileReader = new IlluminaFileReader(null);
        assertNull(fileReader);
    }
}
