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
import java.util.List;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMFileReader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public static void setUpClass() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() {
        
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
    
        SAMFileReader samFileReader = new SAMFileReader(testData.tempBamFile);
        SAMProgramRecord pgr = samFileReader.getFileHeader().getProgramRecords().get(0);
        List<SAMReadGroupRecord> rgl =  samFileReader.getFileHeader().getReadGroups();
        assertEquals(1, rgl.size());
        assertNull(pgr.getPreviousProgramGroupId() );
        assertEquals("Ensure PG field of RG record corresponds to the appropriate program", pgr.getId(),  rgl.get(0).getAttribute("PG"));
        samFileReader.close();

        assertEquals("ce104cf5c591cdbf94eb7b584d6f5352", CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }

    /**
     * Test of instanceMain method and program record if no FIRST_TILE specified.
     */
    @Test
    public void noFirstTileTest() {
        
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
        
        assertEquals("1ce96b8c94224cfaa260e871f2e997dc", CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }

    /**
     * Test for processing specific cycle range.
     */
    @Test
    public void cycleRangeTest() {
        
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
                + " FIRST_CYCLE=[50] FINAL_CYCLE=[51]"
                + " TMP_DIR=[testdata] VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1"
                + " CREATE_MD5_FILE=true    GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1"
                + " SEQUENCING_CENTER=SC PLATFORM=ILLUMINA BARCODE_SEQUENCE_TAG_NAME=BC BARCODE_QUALITY_TAG_NAME=QT"
                + " VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false",
                testData.illumina2bam.getCommandLine()
               );

        assertEquals("f8452061bbc72a2dbfb4eda3d5ed896a",CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }

    /**
     * Test for BC_READ option.
     */
    @Test
    public void bcReadTest() {
        
        System.out.println("processing specific cycle range");
        Data testData = new Data("testdata/test_6000_BC_READ.sam");
        String[] args = {"INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
            "BC_READ=2",
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
            "FIRST_CYCLE=1",
            "FINAL_CYCLE=2",
            "FIRST_CYCLE=52",
            "FINAL_CYCLE=53",
            "FIRST_INDEX=50",
            "FINAL_INDEX=51",
            "RUN_START_DATE=2011-03-23T00:00:00+0000"
        };
        
        testData.commonAsserts(args);        
        assertEquals("uk.ac.sanger.npg.illumina.Illumina2bam"
                + " INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities"
                + " LANE=1 OUTPUT=" + testData.tempBamFile.getPath()
                + " SAMPLE_ALIAS=TestSample LIBRARY_NAME=TestLibrary"
                + " STUDY_NAME=TestStudy RUN_START_DATE=2011-03-23T00:00:00+0000 FIRST_TILE=1101 TILE_LIMIT=1 BC_READ=2"
                + " FIRST_CYCLE=[1, 52] FINAL_CYCLE=[2, 53] FIRST_INDEX_CYCLE=[50] FINAL_INDEX_CYCLE=[51]"
                + " TMP_DIR=[testdata] VALIDATION_STRINGENCY=STRICT COMPRESSION_LEVEL=1"
                + " CREATE_MD5_FILE=true    GENERATE_SECONDARY_BASE_CALLS=false PF_FILTER=true READ_GROUP_ID=1"
                + " SEQUENCING_CENTER=SC PLATFORM=ILLUMINA BARCODE_SEQUENCE_TAG_NAME=BC BARCODE_QUALITY_TAG_NAME=QT"
                + " VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false",
                testData.illumina2bam.getCommandLine()
               );

        assertEquals("c31d1cc84eff1ab6ce5ba5fa462fe014",CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }

    /**
     * Test dual index run.
     */
    @Test
    public void dualIndexRunTest() {
        System.out.println("processing dual index run");
        Data testData = new Data("testdata/test_dual_13349.sam");
        String[] args = {"INTENSITY_DIR=testdata/140624_MS6_13349_A_MS2639979-300V2/Data/Intensities",
                "LANE=1",
                "OUTPUT=" + testData.tempBamFile.getPath(),
                "SAMPLE_ALIAS=TestSample",
                "LIBRARY_NAME=TestLibrary",
                "STUDY_NAME=TestStudy",
                "RUN_START_DATE=2011-03-23T00:00:00+0000",
                "FIRST_TILE=1101",
                "TILE_LIMIT=1",
                "TMP_DIR=testdata/",
                "VALIDATION_STRINGENCY=STRICT",
                "COMPRESSION_LEVEL=1",
                "CREATE_MD5_FILE=true",
                "PF_FILTER=false"
               };
        testData.commonAsserts(args);        
        assertEquals("2c0d8b137bd8fe5afe5e50c11b561b0f",CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }

    /**
     * Test propagation of SEC_BC_SEQ option.
     */
    @Test
    public void propagationOfSecondIndexParameters() {
        System.out.println("processing dual index run - index reads in separate tags");
        Data testData = new Data("testdata/test_dual_st_13349.sam");
        String[] args = {"INTENSITY_DIR=testdata/140624_MS6_13349_A_MS2639979-300V2/Data/Intensities",
                "LANE=1",
                "OUTPUT=" + testData.tempBamFile.getPath(),
                "SAMPLE_ALIAS=TestSample",
                "LIBRARY_NAME=TestLibrary",
                "STUDY_NAME=TestStudy",
                "RUN_START_DATE=2011-03-23T00:00:00+0000",
                "FIRST_TILE=1101",
                "TILE_LIMIT=1",
                "TMP_DIR=testdata/",
                "VALIDATION_STRINGENCY=STRICT",
                "COMPRESSION_LEVEL=1",
                "CREATE_MD5_FILE=true",
                "PF_FILTER=false",
                "BC_SEQ=tr","BC_QUAL=tq","SEC_BC_SEQ=BC","SEC_BC_QUAL=QT"
               };
        testData.commonAsserts(args);        
        assertEquals("cc191980b6d85386ae003971b02a3cc9",CheckMd5.getBamMd5AfterRemovePGVersion(testData.tempBamFile, "Illumina2bam"));
    }
}
