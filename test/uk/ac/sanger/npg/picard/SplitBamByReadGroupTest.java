/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.sanger.npg.picard;


import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import net.sf.samtools.SAMFileReader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import uk.ac.sanger.npg.bam.util.CheckMd5;

/**
 *
 * @author gq1@sanger.ac.uk
 * 
 */
public class SplitBamByReadGroupTest {
    
    SplitBamByReadGroup splitter = new SplitBamByReadGroup();
    
    public SplitBamByReadGroupTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
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
        
        assertEquals(splitter.getCommandLine(), "uk.ac.sanger.npg.picard.SplitBamByReadGroup INPUT=testdata/decode/6551_8.sam OUTPUT_PREFIX=testdata/6551_8_split OUTPUT_COMMON_RG_HEAD_TO_TRIM=1 TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"); 
    
        File splitFile1 = new File("testdata/6551_8_split#1.sam");
        splitFile1.deleteOnExit();
        assertTrue(splitFile1.exists());

        File md5File1 = new File("testdata/6551_8_split#1.sam.md5");
        md5File1.deleteOnExit();
        assertEquals("7164bc3b679249824716879e838c313d", CheckMd5.getBamMd5AfterRemovePGVersion(splitFile1, "SplitBamByReadGroup"));
        
        File splitFile9 = new File("testdata/6551_8_split#9.sam");
        splitFile9.deleteOnExit();
        assertTrue(splitFile9.exists());

        File md5File9 = new File("testdata/6551_8_split#9.sam.md5");
        md5File9.deleteOnExit();
        assertEquals("c5276464230efa12edaeb4df04e06940", CheckMd5.getBamMd5AfterRemovePGVersion(splitFile9, "SplitBamByReadGroup"));

    
    }
}
