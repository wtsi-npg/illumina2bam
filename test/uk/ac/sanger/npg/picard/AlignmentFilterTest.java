
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
import java.util.Scanner;
import java.util.TimeZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

        File tmpdir= new File("testdata/" + Long.toString(System.nanoTime()));
        Boolean dir_ok = tmpdir.mkdir();
        
        tmpdir.setReadable(true);		// everybody
        tmpdir.setWritable(true, true); // owner only
        tmpdir.deleteOnExit();
        
        String[] args = {
            "IN=testdata/bam/986_1.sam",
            "IN=testdata/bam/986_1_human.sam",
            "OUT=" + tmpdir + "/986_1.bam",
            "OUT=" + tmpdir + "/986_1_human.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=" + tmpdir,
            "VALIDATION_STRINGENCY=SILENT"
        };

        filter.instanceMain(args);
        assertEquals(
          filter.getCommandLine(),
          "uk.ac.sanger.npg.picard.AlignmentFilter INPUT_ALIGNMENT=[testdata/bam/986_1.sam, testdata/bam/986_1_human.sam] OUTPUT_ALIGNMENT=[" +
          tmpdir + "/986_1.bam, " +
          tmpdir + "/986_1_human.bam] TMP_DIR=[" + 
          tmpdir + "] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"
                    );
     
        
        File filteredBamFile = new File(tmpdir  + "/986_1.bam");
        String filteredBamFileStr = CheckMd5.getFileMd5(filteredBamFile);
        filteredBamFile.deleteOnExit();

        File md5File = new File(tmpdir  + "/986_1.bam.md5");  
        md5File.deleteOnExit();
        String Md5FileContents = new Scanner( md5File).next();
        
        String newMd5 = CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter");
        
        assertFalse(filteredBamFileStr.equals(newMd5));
        assertEquals(Md5FileContents, filteredBamFileStr);
           
        File filteredHumanBamFile = new File(tmpdir  + "/986_1_human.bam");
        String filteredHumanBamFileStr = CheckMd5.getFileMd5(filteredHumanBamFile);
        filteredHumanBamFile.deleteOnExit();

        String newHumanMd5 = CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter");
        
        File humanMd5File = new File(tmpdir  + "/986_1_human.bam.md5");  
        humanMd5File.deleteOnExit();
        String humanMd5FileContents = new Scanner( humanMd5File).next();
        
        assertFalse(filteredHumanBamFileStr.equals(newHumanMd5));
        assertEquals(humanMd5FileContents, filteredHumanBamFileStr);
        
        File metricsFile = new File(tmpdir  + "/986_1_human.bam_alignment_filter_metrics.json");
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
        
        assertEquals("97dd92739317018e81d8ebb996b6d9a6", CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));
       
        File filteredHumanBamFile = new File("testdata/986_1_human.bam");
        filteredHumanBamFile.deleteOnExit();

        File humanMd5File = new File("testdata/986_1_human.bam.md5");
        humanMd5File.deleteOnExit();

        assertEquals("e2fcf77cff026f141643c580cbc5827d", CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"));
  
        File unalignedBamFile = new File("testdata/986_1_unaligned.bam");
        unalignedBamFile.deleteOnExit();

        File unalignedMd5File = new File("testdata/986_1_unaligned.bam.md5");
        unalignedMd5File.deleteOnExit();
        
        File metricsFile = new File("testdata/986_1_unaligned.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();

        assertEquals("e4d38f7fdf65cd7069631c72ab0f5103", CheckMd5.getBamMd5AfterRemovePGVersion(unalignedBamFile, "AlignmentFilter"));
  
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
        
        assertEquals("5082d4b82ec8815b7a621e50bfffa8c3", CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));
       
        File filteredHumanBamFile = new File("testdata/single_986_1_human.bam");
        filteredHumanBamFile.deleteOnExit();

        File humanMd5File = new File("testdata/single_986_1_human.bam.md5");
        humanMd5File.deleteOnExit();

        assertEquals("d3140987f7c9d5ff74031019449e8579", CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"));
  
        File unalignedBamFile = new File("testdata/single_986_1_unaligned.bam");
        unalignedBamFile.deleteOnExit();

        File unalignedMd5File = new File("testdata/single_986_1_unaligned.bam.md5");
        unalignedMd5File.deleteOnExit();
        
        File metricsFile = new File("testdata/single_986_1_unaligned.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();

        assertEquals("251dcbc63c0dac88a31f777dc379992c", CheckMd5.getBamMd5AfterRemovePGVersion(unalignedBamFile, "AlignmentFilter"));
  
    }

}
