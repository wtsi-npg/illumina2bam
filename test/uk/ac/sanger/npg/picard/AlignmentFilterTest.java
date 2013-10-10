
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

        String outputName = "testdata/986_1";
       
        File outputDir = new File("testdata/986_1");
        outputDir.mkdir();
        
        String[] args = {
            "IN=testdata/bam/986_1.sam",
            "IN=testdata/bam/986_1_human.sam",
            "OUT=" + outputName + "/986_1.bam",
            "OUT=" + outputName + "/986_1_human.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=" + outputName,
            "VALIDATION_STRINGENCY=SILENT"
        };

        filter.instanceMain(args);
        assertEquals(
        		filter.getCommandLine(),
        		"uk.ac.sanger.npg.picard.AlignmentFilter INPUT_ALIGNMENT=[testdata/bam/986_1.sam, testdata/bam/986_1_human.sam] OUTPUT_ALIGNMENT=[" +
        		outputName + "/986_1.bam, " +
        		outputName + "/986_1_human.bam] TMP_DIR=[" + 
        		outputName + "] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"
        );

        File filteredBamFile = new File(outputName  + "/986_1.bam");

        File md5File = new File(outputName  + "/986_1.bam.md5");  

        assertEquals("5a2b83e09460a2763f8399933fb7011a", CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));

        File filteredHumanBamFile = new File(outputName  + "/986_1_human.bam");

        File humanMd5File = new File(outputName  + "/986_1_human.bam.md5");  

        assertEquals("e0998aac4b5788b61485b46625f52ab4", CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"));

        File metricsFile = new File(outputName  + "/986_1_human.bam_alignment_filter_metrics.json");
        
        metricsFile.delete();
        filteredBamFile.delete();
        filteredHumanBamFile.delete();
        md5File.delete();
        humanMd5File.delete();
        
        outputDir.deleteOnExit();
        
    }
    /**
     * Test of instanceMain method and program record to separate unmapped reads into one file.
     */
    @Test
    public void testMainUnMappedFile() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain with unmapped file");
        String outputName = "testdata/986_1_unmapped";
        
        File outputDir = new File("testdata/986_1_unmapped");
        outputDir.mkdir();
        
        String[] args = {
            "IN=testdata/bam/986_1.sam",
            "IN=testdata/bam/986_1_human_unmapped_with_ref.sam",
            "OUT=" + outputName + "/986_1.bam",
            "OUT=" + outputName + "/986_1_human.bam",
            "OUTPUT_UNALIGNED=" + outputName + "/986_1_unaligned.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=" + outputName + "/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        filter.instanceMain(args);
        assertEquals(
          filter.getCommandLine(),
          "uk.ac.sanger.npg.picard.AlignmentFilter INPUT_ALIGNMENT=[testdata/bam/986_1.sam, testdata/bam/986_1_human_unmapped_with_ref.sam] " +
          "OUTPUT_ALIGNMENT=[" + outputName + "/986_1.bam, " +  outputName + "/986_1_human.bam] OUTPUT_UNALIGNED=" + outputName + "/986_1_unaligned.bam " +
          "TMP_DIR=[" + outputName + "] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        File filteredBamFile = new File(outputName + "/986_1.bam");  
       
        File md5File = new File(outputName  + "/986_1.bam.md5");  
        
        assertEquals("77f2212b089c0d848f7406ffc37faeb2", CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));
       
        File filteredHumanBamFile = new File(outputName + "/986_1_human.bam");      
        
        File humanMd5File = new File(outputName  + "/986_1_human.bam.md5");  
         
        assertEquals("d2447853ffacffff1ad9ea4d28511f92",CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"));
  
        File unalignedBamFile = new File(outputName + "/986_1_unaligned.bam");
 
        File unalignedMd5File = new File(outputName + "/986_1_unaligned.bam.md5");
          
        File metricsFile = new File(outputName + "/986_1_unaligned.bam_alignment_filter_metrics.json");
        
        metricsFile.delete();
        filteredBamFile.delete();
        md5File.delete();
        filteredHumanBamFile.delete();
        humanMd5File.delete();
        unalignedBamFile.delete();
        unalignedMd5File.delete();
        outputDir.deleteOnExit();
    }
    /**
     * Test of instanceMain method and program record to separate unmapped reads into one file for single read data.
     */
    @Test
    public void testSingleReadMainUnMappedFile() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain with single read unmapped file");
        
        String tmpdir = "testdata";
        
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
        
        assertEquals("5082d4b82ec8815b7a621e50bfffa8c3", CheckMd5.getBamMd5AfterRemovePGVersion(filteredBamFile, "AlignmentFilter"));
       
        File filteredHumanBamFile = new File(tmpdir + "/single_986_1_human.bam");
        filteredHumanBamFile.deleteOnExit();

        File humanMd5File = new File(tmpdir + "/single_986_1_human.bam.md5");
        humanMd5File.deleteOnExit();
        
        assertEquals("d3140987f7c9d5ff74031019449e8579", CheckMd5.getBamMd5AfterRemovePGVersion(filteredHumanBamFile, "AlignmentFilter"));
  
        File unalignedBamFile = new File(tmpdir + "/single_986_1_unaligned.bam");
        unalignedBamFile.deleteOnExit();
     
        File unalignedMd5File = new File(tmpdir + "/single_986_1_unaligned.bam.md5");
        unalignedMd5File.deleteOnExit();
      
        File metricsFile = new File(tmpdir + "/single_986_1_unaligned.bam_alignment_filter_metrics.json");
        metricsFile.deleteOnExit();
        
        assertEquals("251dcbc63c0dac88a31f777dc379992c", CheckMd5.getBamMd5AfterRemovePGVersion(unalignedBamFile, "AlignmentFilter"));
        
    }

}
