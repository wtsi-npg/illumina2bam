/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package illumina;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import net.sf.samtools.SAMProgramRecord;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gq1
 */
public class Illumina2bamTest {
    
    Illumina2bam illumina2bam = new Illumina2bam();
    
    public Illumina2bamTest() {
    }
    
    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        System.out.println("instanceMane and this program recordd");
        String[] args = {"INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
            "BASECALLS_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls",
            "LANE=1",
            "OUTPUT=testdata/6000_1.sam",
            "VALIDATION_STRINGENCY=STRICT",
            "CREATE_MD5_FILE=true",
            "FIRST_TILE=1101",
            "COMPRESSION_LEVEL=1",
            "TILE_LIMIT=1"};
        illumina2bam.instanceMain(args);

        File samFile = new File("testdata/6000_1.sam");
        samFile.deleteOnExit();

        File md5File = new File("testdata/6000_1.sam.md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();
        assertEquals(md5, "86af96c99eb9a785cf2811b23b01cb23");

        SAMProgramRecord result = illumina2bam.getThisProgramRecord();

        assertEquals(result.getCommandLine(), "illumina.Illumina2bam INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities BASECALLS_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls LANE=1 OUTPUT=testdata/6000_1.sam FIRST_TILE=1101 TILE_LIMIT=1 VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1 CREATE_MD5_FILE=true    GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1 SEQUENCING_CENTER=SC PLATFORM=ILLUMINA TMP_DIR=/tmp/gq1 VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        assertEquals(result.getId(), "illumina2bam");
        assertEquals(result.getProgramName(), "illumina2bam");
        assertEquals(result.getProgramVersion(), illumina2bam.getProgramVersion());
        assertEquals(result.getAttribute("DS"), "Covert Illumina BCL to BAM or SAM file");
    }

}
