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
 * This is the test class for Illumina2bam
 *
 */
package illumina;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
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
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }
    
    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        System.out.println("instanceMain and this program record");
        String[] args = {"INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
            "LANE=1",
            "OUTPUT=testdata/6000_1.sam",
            "VALIDATION_STRINGENCY=STRICT",
            "CREATE_MD5_FILE=true",
            "FIRST_TILE=1101",
            "COMPRESSION_LEVEL=1",
            "TILE_LIMIT=1",
            "LB=Test library",
            "SM=Test Sample",
            "ST=testStudy",
            "TMP_DIR=testdata/",
            "RUN_START_DATE=2011-03-23T00:00:00+0000"
        };
        illumina2bam.instanceMain(args);
        

        File samFile = new File("testdata/6000_1.sam");
        samFile.deleteOnExit();      

        File md5File = new File("testdata/6000_1.sam.md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();
        assertEquals(md5, "12b542a6b19097ca6ea559d9d1bd5cf7");

        assertEquals(illumina2bam.getCommandLine(), "illumina.Illumina2bam INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities "
                + "LANE=1 OUTPUT=testdata/6000_1.sam SAMPLE_ALIAS=Test Sample LIBRARY_NAME=Test library "
                + "STUDY_NAME=testStudy RUN_START_DATE=2011-03-23T00:00:00+0000 FIRST_TILE=1101 TILE_LIMIT=1 TMP_DIR=testdata "
                + "VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1 CREATE_MD5_FILE=true    "
                + "GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1 SEQUENCING_CENTER=SC "
                + "PLATFORM=ILLUMINA VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"
               );
        
        SAMProgramRecord result = illumina2bam.getThisProgramRecord("illumina2bam", "Convert Illumina BCL to BAM or SAM file");
        assertEquals(result.getId(), "illumina2bam");
        assertEquals(result.getProgramName(), "illumina2bam");
        assertEquals(result.getProgramVersion(), illumina2bam.getProgramVersion());
        assertEquals(result.getAttribute("DS"), "Convert Illumina BCL to BAM or SAM file");
    }

}
