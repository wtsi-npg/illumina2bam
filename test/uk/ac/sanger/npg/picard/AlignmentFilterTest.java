
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
 *
 */
package uk.ac.sanger.npg.picard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TimeZone;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import uk.ac.sanger.npg.bam.util.CheckMd5;

/**
 * This is the test class for BamMerger
 * 
 * @author gq1@sanger.ac.uk
 */

public class AlignmentFilterTest {
    
    AlignmentFilter filter = new AlignmentFilter();

    public AlignmentFilterTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }
    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain");

        String[] args = {
            "IN=testdata/bam/986_1.sam",
            "IN=testdata/bam/986_1_human.sam",
            "OUT=testdata/986_1.bam",
            "OUT=testdata/986_1_human.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        filter.instanceMain(args);
        assertEquals(
          filter.getCommandLine(),
          "uk.ac.sanger.npg.picard.AlignmentFilter INPUT_ALIGNMENT=[testdata/bam/986_1.sam, testdata/bam/986_1_human.sam] OUTPUT_ALIGNMENT=[testdata/986_1.bam, testdata/986_1_human.bam] TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"
                    );
        
        File filteredBamFile = new File("testdata/986_1.bam");
        filteredBamFile.deleteOnExit();

        File md5File = new File("testdata/986_1.bam.md5");
        md5File.deleteOnExit();
 
        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"), "b49b5f7a87e78ed4f52385d2153a8c1b");
        
        File filteredHumanBamFile = new File("testdata/986_1_human.bam");
        filteredHumanBamFile.deleteOnExit();

        File humanMd5File = new File("testdata/986_1_human.bam.md5");
        humanMd5File.deleteOnExit();
        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"), "19669ce184d8ef2d514a4984196d5df5");
        
        File metricsFile = new File("testdata/986_1_human.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();

    }
    /**
     * Test of instanceMain method and program record to separate unmapped reads into one file.
     */
    @Test
    public void testMainUnMappedFile() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain with unmapped file");

        String[] args = {
            "IN=testdata/bam/986_1.sam",
            "IN=testdata/bam/986_1_human_unmapped_with_ref.sam",
            "OUT=testdata/986_1.bam",
            "OUT=testdata/986_1_human.bam",
            "OUTPUT_UNALIGNED=testdata/986_1_unaligned.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        filter.instanceMain(args);
        assertEquals(
          filter.getCommandLine(),
          "uk.ac.sanger.npg.picard.AlignmentFilter INPUT_ALIGNMENT=[testdata/bam/986_1.sam, testdata/bam/986_1_human_unmapped_with_ref.sam] OUTPUT_ALIGNMENT=[testdata/986_1.bam, testdata/986_1_human.bam] OUTPUT_UNALIGNED=testdata/986_1_unaligned.bam TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        File filteredBamFile = new File("testdata/986_1.bam");
        filteredBamFile.deleteOnExit();

        File md5File = new File("testdata/986_1.bam.md5");
        md5File.deleteOnExit();
        
        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"), "a63f5de7e5724881a04c646cdf9dfa4c");
       
        File filteredHumanBamFile = new File("testdata/986_1_human.bam");
        filteredHumanBamFile.deleteOnExit();

        File humanMd5File = new File("testdata/986_1_human.bam.md5");
        humanMd5File.deleteOnExit();

        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"), "df8ab1b3bc45bdf4a1e195ac95cdb03a");
  
        File unalignedBamFile = new File("testdata/986_1_unaligned.bam");
        unalignedBamFile.deleteOnExit();

        File unalignedMd5File = new File("testdata/986_1_unaligned.bam.md5");
        unalignedMd5File.deleteOnExit();
        
        File metricsFile = new File("testdata/986_1_unaligned.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();

        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(unalignedBamFile, "AlignmentFilter"), "7af865eec6f41718e5f34c5e0759d1fe");
  
    }
    /**
     * Test of instanceMain method and program record to separate unmapped reads into one file for single read data.
     */
    @Test
    public void testSingleReadMainUnMappedFile() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain with unmapped file");

        String[] args = {
            "IN=testdata/bam/single_986_1.sam",
            "IN=testdata/bam/single_986_1_human_unmapped_with_ref.sam",
            "OUT=testdata/single_986_1.bam",
            "OUT=testdata/single_986_1_human.bam",
            "OUTPUT_UNALIGNED=testdata/single_986_1_unaligned.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        filter.instanceMain(args);
        assertEquals(
          filter.getCommandLine(),
          "uk.ac.sanger.npg.picard.AlignmentFilter INPUT_ALIGNMENT=[testdata/bam/single_986_1.sam, testdata/bam/single_986_1_human_unmapped_with_ref.sam] OUTPUT_ALIGNMENT=[testdata/single_986_1.bam, testdata/single_986_1_human.bam] OUTPUT_UNALIGNED=testdata/single_986_1_unaligned.bam TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        File filteredBamFile = new File("testdata/single_986_1.bam");
        filteredBamFile.deleteOnExit();

        File md5File = new File("testdata/single_986_1.bam.md5");
        md5File.deleteOnExit();
        
        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"), "566a490e155bfcec59cc41a8733bf9ab");
       
        File filteredHumanBamFile = new File("testdata/single_986_1_human.bam");
        filteredHumanBamFile.deleteOnExit();

        File humanMd5File = new File("testdata/single_986_1_human.bam.md5");
        humanMd5File.deleteOnExit();

        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"), "cf443faa25bf61cde53d0342857df4a5");
  
        File unalignedBamFile = new File("testdata/single_986_1_unaligned.bam");
        unalignedBamFile.deleteOnExit();

        File unalignedMd5File = new File("testdata/single_986_1_unaligned.bam.md5");
        unalignedMd5File.deleteOnExit();
        
        File metricsFile = new File("testdata/single_986_1_unaligned.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();

        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(unalignedBamFile, "AlignmentFilter"), "eab239ff547092f419432218f9856291");
  
    }

}
