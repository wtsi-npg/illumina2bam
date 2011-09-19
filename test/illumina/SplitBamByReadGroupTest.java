/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package illumina;


import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Guoying Qi
 */
public class SplitBamByReadGroupTest {
    
    SplitBamByReadGroup splitter = new SplitBamByReadGroup();
    
    public SplitBamByReadGroupTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws IOException {
        System.out.println("instanceMain");
        
        String[] args = {
            "I=testdata/decode/6551_8.sam",
            "O=testdata/6551_8_split",
            "TRIM=1",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        splitter.instanceMain(args);
        
        assertEquals(splitter.getCommandLine(), "illumina.SplitBamByReadGroup INPUT=testdata/decode/6551_8.sam OUTPUT_PREFIX=testdata/6551_8_split OUTPUT_COMMON_RG_HEAD_TO_TRIM=1 TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"); 
    
        File splitFile1 = new File("testdata/6551_8_split#1.sam");
        splitFile1.deleteOnExit();
        assertTrue(splitFile1.exists());

        File md5File1 = new File("testdata/6551_8_split#1.sam.md5");
        md5File1.deleteOnExit();
        BufferedReader md5Stream1 = new BufferedReader(new FileReader(md5File1));
        String md5 = md5Stream1.readLine();
        assertEquals(md5, "e2d095b4acd5e7de77962b6732a64977");
        
        File splitFile9 = new File("testdata/6551_8_split#9.sam");
        splitFile9.deleteOnExit();
        assertTrue(splitFile9.exists());

        File md5File9 = new File("testdata/6551_8_split#9.sam.md5");
        md5File9.deleteOnExit();
        BufferedReader md5Stream9 = new BufferedReader(new FileReader(md5File9));
        md5 = md5Stream9.readLine();
        assertEquals(md5, "b5cb704dee3e6bacd028d5f551af7ccb");
    
    }
}