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
package uk.ac.sanger.npg.illumina;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TimeZone;
import net.sf.samtools.SAMProgramRecord;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.sanger.npg.bam.util.CheckMd5;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class Illumina2bamTest {
    
    public static Illumina2bam illumina2bam = null;
    public static Illumina2bam illumina2bamCycleRange = null;
    public static File tempBamFile = null;
    public static File md5File = null;
    public static File tempBamFileCycleRange = null;
    public static File md5FileCycleRange = null;
    
        @BeforeClass
    public static void setUpClass() throws Exception {
            
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        illumina2bam =  new Illumina2bam();
        illumina2bamCycleRange =  new Illumina2bam();
        tempBamFile = new File("testdata/test_6000_1.sam");
        tempBamFile.deleteOnExit();
        md5File = new File(tempBamFile.getPath() + ".md5");
        md5File.deleteOnExit();
        tempBamFileCycleRange = new File("testdata/test_6000_1_50-51.sam");
	tempBamFileCycleRange.deleteOnExit();
        md5FileCycleRange = new File(tempBamFileCycleRange.getPath() + ".md5");
        md5FileCycleRange.deleteOnExit();
    }

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain and this program record command line");
        String[] args = {"INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
            "LANE=1",
            "OUTPUT=" + tempBamFile.getPath(),
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
        
        assertEquals("uk.ac.sanger.npg.illumina.Illumina2bam"
                + " INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities"
                + " LANE=1 OUTPUT=" + tempBamFile.getPath()
                + " SAMPLE_ALIAS=Test Sample LIBRARY_NAME=Test library"
                + " STUDY_NAME=testStudy RUN_START_DATE=2011-03-23T00:00:00+0000 FIRST_TILE=1101 TILE_LIMIT=1"
                + " TMP_DIR=[testdata] VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1"
                + " CREATE_MD5_FILE=true    GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1"
                + " SEQUENCING_CENTER=SC PLATFORM=ILLUMINA BARCODE_SEQUENCE_TAG_NAME=BC BARCODE_QUALITY_TAG_NAME=QT"
		+ " VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false",
		illumina2bam.getCommandLine()
               );
     
        System.out.println("getThisProgramRecord");
        
        SAMProgramRecord result = illumina2bam.getThisProgramRecord("Illumina2bam", "Convert Illumina BCL to BAM or SAM file");
        assertEquals(result.getId(), "Illumina2bam");
        assertEquals(result.getProgramName(), "Illumina2bam");
        assertEquals(result.getProgramVersion(), illumina2bam.getProgramVersion());
        assertEquals(result.getAttribute("DS"), "Convert Illumina BCL to BAM or SAM file");
    }
    
    @Test 
    public void checkOutputMd5(){
        assertEquals("62749a4c4cd90e192cd7b8765108d6f8", CheckMd5.getBamMd5AfterRemovePGVersion(tempBamFile, "Illumina2bam"));
    }

    /**
     * Test for processing specific cycle range.
     */
    @Test
    public void cycleRangeTest() throws FileNotFoundException, IOException {
        
        System.out.println("processing specific cycle range");
        String[] args = {"INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
            "LANE=1",
	    "OUTPUT=" + tempBamFileCycleRange.getPath(),
            "VALIDATION_STRINGENCY=STRICT",
            "CREATE_MD5_FILE=true",
            "FIRST_TILE=1101",
            "COMPRESSION_LEVEL=1",
            "TILE_LIMIT=1",
            "LB=TestLibrary",
            "SM=TestSample",
            "ST=TestStudy",
            "TMP_DIR=testdata/",
            "FIRST_CYCLE=50",
            "FINAL_CYCLE=51",
            "RUN_START_DATE=2011-03-23T00:00:00+0000"
        };
        illumina2bamCycleRange.instanceMain(args);
        
        assertEquals("uk.ac.sanger.npg.illumina.Illumina2bam"
                + " INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities"
                + " LANE=1 OUTPUT=" + tempBamFileCycleRange.getPath()
                + " SAMPLE_ALIAS=TestSample LIBRARY_NAME=TestLibrary"
                + " STUDY_NAME=TestStudy RUN_START_DATE=2011-03-23T00:00:00+0000 FIRST_TILE=1101 TILE_LIMIT=1"
                + " FIRST_CYCLE=50 FINAL_CYCLE=51"
                + " TMP_DIR=[testdata] VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1"
                + " CREATE_MD5_FILE=true    GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1"
                + " SEQUENCING_CENTER=SC PLATFORM=ILLUMINA BARCODE_SEQUENCE_TAG_NAME=BC BARCODE_QUALITY_TAG_NAME=QT"
		+ " VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false",
                illumina2bamCycleRange.getCommandLine()
               );
        System.out.println("getThisProgramRecord");
     
        SAMProgramRecord result = illumina2bamCycleRange.getThisProgramRecord("Illumina2bam", "Convert Illumina BCL to BAM or SAM file");
        assertEquals(result.getId(), "Illumina2bam");
        assertEquals(result.getProgramName(), "Illumina2bam");
        assertEquals(result.getProgramVersion(), illumina2bam.getProgramVersion());
        assertEquals(result.getAttribute("DS"), "Convert Illumina BCL to BAM or SAM file");
    }
    
    @Test 
    public void checkCycleRangeOutputMd5(){
        System.out.println("expected &&&: " +  CheckMd5.getBamMd5AfterRemovePGVersion(tempBamFile, "Illumina2bam"));
        assertEquals("eee452bde37d9564a51045f2f92c3570",CheckMd5.getBamMd5AfterRemovePGVersion(tempBamFileCycleRange, "Illumina2bam"));
    }
}
