/*
 * test class IlluminaFileReader
 *
 */
package illumina;

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
        assertEquals(fileReader, null);
    }

    @Test(expected = FileNotFoundException.class)
    public void testConstructorExpectedException2() throws Exception {
        IlluminaFileReader fileReader = new IlluminaFileReader("nonexisted.file");
        assertEquals(fileReader, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorExpectedException1() throws Exception {
        IlluminaFileReader fileReader = new IlluminaFileReader(null);
        assertEquals(fileReader, null);
    }
}
