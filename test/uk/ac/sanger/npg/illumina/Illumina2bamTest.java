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

    private static class Data {
        public Illumina2bam illumina2bam = new Illumina2bam();
        public File tempBamFile = null;
        public File md5File = null;
        public Data(String fileName){
            tempBamFile = new File(fileName);
            tempBamFile.deleteOnExit();
            md5File = new File(tempBamFile.getPath() + ".md5");
            md5File.deleteOnExit();
        }
        void commonAsserts(String[] args){
            assertEquals(0, illumina2bam.instanceMain(args));
            SAMProgramRecord result = illumina2bam.getThisProgramRecord("Illumina2bam", "Convert Illumina BCL to BAM or SAM file");
            assertEquals(result.getId(), "Illumina2bam");
            assertEquals(result.getProgramName(), "Illumina2bam");
            assertEquals(result.getProgramVersion(), illumina2bam.getProgramVersion());
            assertEquals(result.getAttribute("DS"), "Convert Illumina BCL to BAM or SAM file");
        }
    }
    
        @BeforeClass
    public static void setUpClass() throws Exception {            
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain and this program record command line");
        Data testData = new Data("testdata/test_6000_1.sam");
        String[] args = {"INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
            "LANE=1",
            "OUTPUT=" + testData.tempBamFile.getPath(),
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
        
        testData.commonAsserts(args);
        assertEquals("uk.ac.sanger.npg.illumina.Illumina2bam"
                + " INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities"
                + " LANE=1 OUTPUT=" + testData.tempBamFile.getPath()
                + " SAMPLE_ALIAS=Test Sample LIBRARY_NAME=Test library"
                + " STUDY_NAME=testStudy RUN_START_DATE=2011-03-23T00:00:00+0000 FIRST_TILE=1101 TILE_LIMIT=1"
                + " TMP_DIR=[testdata] VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1"
                + " CREATE_MD5_FILE=true    GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1"
                + " SEQUENCING_CENTER=SC PLATFORM=ILLUMINA BARCODE_SEQUENCE_TAG_NAME=BC BARCODE_QUALITY_TAG_NAME=QT"
                + " VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false",
                testData.illumina2bam.getCommandLine()
               );

        assertEquals("62749a4c4cd90e192cd7b8765108d6f8", CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }

    /**
     * Test of instanceMain method and program record if no FIRST_TILE specified.
     */
    @Test
    public void noFirstTileTest() { //throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain and this program record command line when no FIRST_TILE specified");
        Data testData = new Data("testdata/test_6000_1_nft.sam");
        String[] args = {"INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
            "TILE_LIMIT=1",
            "LANE=1",
            "OUTPUT=" + testData.tempBamFile.getPath(),
            "VALIDATION_STRINGENCY=STRICT",
            "CREATE_MD5_FILE=true",
            "COMPRESSION_LEVEL=1",
            "LB=Test library",
            "SM=Test Sample",
            "ST=testStudy",
            "TMP_DIR=testdata/",
            "RUN_START_DATE=2011-03-23T00:00:00+0000"
        };

        testData.commonAsserts(args);
        assertEquals("uk.ac.sanger.npg.illumina.Illumina2bam"
                + " INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities"
                + " LANE=1 OUTPUT=" + testData.tempBamFile.getPath()
                + " SAMPLE_ALIAS=Test Sample LIBRARY_NAME=Test library"
                + " STUDY_NAME=testStudy RUN_START_DATE=2011-03-23T00:00:00+0000 TILE_LIMIT=1"
                + " TMP_DIR=[testdata] VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1"
                + " CREATE_MD5_FILE=true    GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1"
                + " SEQUENCING_CENTER=SC PLATFORM=ILLUMINA BARCODE_SEQUENCE_TAG_NAME=BC BARCODE_QUALITY_TAG_NAME=QT"
                + " VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false",
                testData.illumina2bam.getCommandLine()
               );
        
        assertEquals("e4623e8ac08fb9194a5f5a1c1eb836a2", CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }

    /**
     * Test for processing specific cycle range.
     */
    @Test
    public void cycleRangeTest() throws FileNotFoundException, IOException {
        
        System.out.println("processing specific cycle range");
        Data testData = new Data("testdata/test_6000_1_50-51.sam");
        String[] args = {"INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
            "LANE=1",
            "OUTPUT=" + testData.tempBamFile.getPath(),
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
        
        testData.commonAsserts(args);        
        assertEquals("uk.ac.sanger.npg.illumina.Illumina2bam"
                + " INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities"
                + " LANE=1 OUTPUT=" + testData.tempBamFile.getPath()
                + " SAMPLE_ALIAS=TestSample LIBRARY_NAME=TestLibrary"
                + " STUDY_NAME=TestStudy RUN_START_DATE=2011-03-23T00:00:00+0000 FIRST_TILE=1101 TILE_LIMIT=1"
                + " FIRST_CYCLE=50 FINAL_CYCLE=51"
                + " TMP_DIR=[testdata] VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1"
                + " CREATE_MD5_FILE=true    GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1"
                + " SEQUENCING_CENTER=SC PLATFORM=ILLUMINA BARCODE_SEQUENCE_TAG_NAME=BC BARCODE_QUALITY_TAG_NAME=QT"
                + " VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false",
                testData.illumina2bam.getCommandLine()
               );
     
        assertEquals("eee452bde37d9564a51045f2f92c3570",CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }
}
