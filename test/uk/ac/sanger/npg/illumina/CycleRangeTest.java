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
 * @author ib5@sanger.ac.uk
 * Test specification of cycle ranges on command line
 */
public class CycleRangeTest {



    @Test
        public void testMain() {
        System.out.println("Simple test of cycle range command-line arguments");
        File tempBamFile = new File("testdata/cycleRangeTest_6000_1.sam");
        File md5File = new File(tempBamFile.getPath() + ".md5");
        tempBamFile.deleteOnExit();
        md5File.deleteOnExit();
        Illumina2bam illumina2bam = new Illumina2bam();
        String[] args = {
            "INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
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
            "RUN_START_DATE=2011-03-23T00:00:00+0000",
            "FIRST=1",
            "FIRST=51",
            "FIRST_INDEX=50",
            "FINAL=2",
            "FINAL=52",
            "FINAL_INDEX=51",
        };
        assertEquals(0, illumina2bam.instanceMain(args));
    }

    @Test
        public void badArgumentTest() {
        File tempBamFile = new File("testdata/cycleRangeTest_6000_2.sam");
        tempBamFile.deleteOnExit();
        Illumina2bam illumina2bam = new Illumina2bam();
        String[] args = {
            "INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
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
            "RUN_START_DATE=2011-03-23T00:00:00+0000",
            "FIRST=52",
            "FINAL=2",
        };
        assertEquals(2, illumina2bam.instanceMain(args));
        System.out.println("Test of incorrect arguments finished.");
    }

    @Test
        public void incompleteArgumentTest() {
        File tempBamFile = new File("testdata/cycleRangeTest_6000_2.sam");
        tempBamFile.deleteOnExit();
        Illumina2bam illumina2bam = new Illumina2bam();
        String[] args = {
            "INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities",
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
            "RUN_START_DATE=2011-03-23T00:00:00+0000",
            "FIRST=52",
        };
        assertEquals(2, illumina2bam.instanceMain(args));
        System.out.println("Test of incomplete arguments finished.");
    }


}
