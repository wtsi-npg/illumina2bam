
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

        String tmpdir = "testdata/test1";
        
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
        filteredBamFile.deleteOnExit();

        File md5File = new File(tmpdir  + "/986_1.bam.md5");  
        md5File.deleteOnExit();
        
        assertEquals("f208e0111a850c91228480ae20eab2eb", CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));
           
        File filteredHumanBamFile = new File(tmpdir  + "/986_1_human.bam");
        filteredHumanBamFile.deleteOnExit();
        
        File humanMd5File = new File(tmpdir  + "/986_1_human.bam.md5");  
        humanMd5File.deleteOnExit();
        
        assertEquals("12eb49d278ca7030fa4852090f54db7a", CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"));
        
        File metricsFile = new File(tmpdir  + "/986_1_human.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();
  
    }
    /**
     * Test of instanceMain method and program record to separate unmapped reads into one file.
     */
    @Test
    public void testMainUnMappedFile() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain with unmapped file");

        String tmpdir = "testdata/test2";
        
        String[] args = {
            "IN=testdata/bam/986_1.sam",
            "IN=testdata/bam/986_1_human_unmapped_with_ref.sam",
            "OUT=" + tmpdir + "/986_1.bam",
            "OUT=" + tmpdir + "/986_1_human.bam",
            "OUTPUT_UNALIGNED=" + tmpdir + "/986_1_unaligned.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=" + tmpdir + "/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        filter.instanceMain(args);
        assertEquals(
          filter.getCommandLine(),
          "uk.ac.sanger.npg.picard.AlignmentFilter INPUT_ALIGNMENT=[testdata/bam/986_1.sam, testdata/bam/986_1_human_unmapped_with_ref.sam] " +
          "OUTPUT_ALIGNMENT=[" + tmpdir + "/986_1.bam, " +  tmpdir + "/986_1_human.bam] OUTPUT_UNALIGNED=" + tmpdir + "/986_1_unaligned.bam " +
          "TMP_DIR=[" + tmpdir + "] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        File filteredBamFile = new File(tmpdir + "/986_1.bam");  
        filteredBamFile.deleteOnExit();

        File md5File = new File(tmpdir  + "/986_1.bam.md5");  
        md5File.deleteOnExit();
        
        assertEquals("edd7db004ab7c49a488c0f3ed14eb770", CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));
       
        File filteredHumanBamFile = new File(tmpdir + "/986_1_human.bam");      
        filteredHumanBamFile.deleteOnExit();

        File humanMd5File = new File(tmpdir  + "/986_1_human.bam.md5");  
        humanMd5File.deleteOnExit();
         
        assertEquals("edd7db004ab7c49a488c0f3ed14eb770",CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));
  
        File unalignedBamFile = new File(tmpdir + "/986_1_unaligned.bam");
        unalignedBamFile.deleteOnExit();

        File unalignedMd5File = new File(tmpdir + "/986_1_unaligned.bam.md5");
        unalignedMd5File.deleteOnExit();
          
        File metricsFile = new File(tmpdir + "/986_1_unaligned.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();
 
    }
    /**
     * Test of instanceMain method and program record to separate unmapped reads into one file for single read data.
     */
    @Test
    public void testSingleReadMainUnMappedFile() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain with single read unmapped file");
        
        String tmpdir = "testdata/test3";
        
        String[] args = {
            "IN=testdata/bam/single_986_1.sam",
            "IN=testdata/bam/single_986_1_human_unmapped_with_ref.sam",
            "OUT=" + tmpdir + "/single_986_1.bam",
            "OUT=" + tmpdir + "/single_986_1_human.bam",
            "OUTPUT_UNALIGNED=" + tmpdir + "/single_986_1_unaligned.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=" + tmpdir + "/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        filter.instanceMain(args);
        assertEquals(
          filter.getCommandLine(),
          "uk.ac.sanger.npg.picard.AlignmentFilter " +
          "INPUT_ALIGNMENT=[testdata/bam/single_986_1.sam, testdata/bam/single_986_1_human_unmapped_with_ref.sam] " +
          "OUTPUT_ALIGNMENT=[" + tmpdir + "/single_986_1.bam, " + tmpdir + "/single_986_1_human.bam] " +
          "OUTPUT_UNALIGNED=" + tmpdir + "/single_986_1_unaligned.bam TMP_DIR=[" + tmpdir + "] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        File filteredBamFile = new File(tmpdir + "/single_986_1.bam");
        filteredBamFile.deleteOnExit();

        File md5File = new File(tmpdir  + "/single_986_1.bam.md5");  
        md5File.deleteOnExit();
        
        assertEquals("55f0a25bec8ad386896f29cd2411da89", CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));
       
        File filteredHumanBamFile = new File(tmpdir + "/single_986_1_human.bam");
        filteredHumanBamFile.deleteOnExit();

        File humanMd5File = new File(tmpdir + "/single_986_1_human.bam.md5");
        humanMd5File.deleteOnExit();
        
        assertEquals("4a946d11b021ea265ea8042a53705590", CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"));
  
        File unalignedBamFile = new File(tmpdir + "/single_986_1_unaligned.bam");
        unalignedBamFile.deleteOnExit();
     
        File unalignedMd5File = new File(tmpdir + "/single_986_1_unaligned.bam.md5");
        unalignedMd5File.deleteOnExit();
      
        File metricsFile = new File(tmpdir + "/single_986_1_unaligned.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();
        
        assertEquals("d398ee75d56355332831cc2b862fe6ca", CheckMd5.getBamMd5AfterRemovePGVersion(unalignedBamFile, "AlignmentFilter"));
        
    }

}
