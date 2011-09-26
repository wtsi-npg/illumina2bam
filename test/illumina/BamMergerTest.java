
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
package illumina;

import java.util.TimeZone;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This is the test class for BamMerger
 * 
 * @author Guoying Qi
 */

public class BamMergerTest {
    
    BamMerger merger = new BamMerger();

    public BamMergerTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }
    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain");
        
        String[] args = {
            "ALIGNED=testdata/bam/6210_8_aligned.sam",
            "I=testdata/bam/6210_8.sam",
            "O=testdata/6210_8_merged.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        merger.instanceMain(args);   
        assertEquals(merger.getCommandLine(), "illumina.BamMerger ALIGNED_BAM=testdata/bam/6210_8_aligned.sam INPUT=testdata/bam/6210_8.sam OUTPUT=testdata/6210_8_merged.bam TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    ALIGNMENT_PROGRAM_ID=bwa KEEP_ALL_PG=false KEEP_EXTRA_UNMAPPED_READS=false VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"
              );

    }
        
    /**
     * Test output bam MD5
     */
    @Test
    public void testOutputBam() throws FileNotFoundException, IOException {
        System.out.println("checking output bam md5");
        File mergedBamFile = new File("testdata/6210_8_merged.bam");
        mergedBamFile.deleteOnExit();

        File md5File = new File("testdata/6210_8_merged.bam.md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals(md5, "f807b9572ad8f1a2aa44db4158fad887");
    }

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMainExtraReadsAtEnd() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain extra reads at end");
        
        String[] args = {
            "ALIGNED=testdata/bam/6210_8_aligned.sam",
            "I=testdata/bam/6210_8_extra_reads.sam",
            "O=testdata/6210_8_merged_extra_reads.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT",
            "KEEP=true"
        };

        merger.instanceMain(args);
        
        assertEquals(merger.getCommandLine(), "illumina.BamMerger ALIGNED_BAM=testdata/bam/6210_8_aligned.sam INPUT=testdata/bam/6210_8_extra_reads.sam OUTPUT=testdata/6210_8_merged_extra_reads.bam KEEP_EXTRA_UNMAPPED_READS=true TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    ALIGNMENT_PROGRAM_ID=bwa KEEP_ALL_PG=false VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        File mergedBamFile = new File("testdata/6210_8_merged_extra_reads.bam");
        mergedBamFile.deleteOnExit();

        File md5File = new File("testdata/6210_8_merged_extra_reads.bam.md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals(md5, "74020dfec4ae9029a18685d36fd1e6b5");
    }
    
    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMainExtraReadsAtEndNotKeep() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain extra reads at end, not keep extra reads");
        
        String[] args = {
            "ALIGNED=testdata/bam/6210_8_aligned.sam",
            "I=testdata/bam/6210_8_extra_reads.sam",
            "O=testdata/6210_8_merged_extra_reads_no_keep.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT",
        };

        merger.instanceMain(args);
        
        assertEquals(merger.getCommandLine(), "illumina.BamMerger ALIGNED_BAM=testdata/bam/6210_8_aligned.sam INPUT=testdata/bam/6210_8_extra_reads.sam OUTPUT=testdata/6210_8_merged_extra_reads_no_keep.bam TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    ALIGNMENT_PROGRAM_ID=bwa KEEP_ALL_PG=false KEEP_EXTRA_UNMAPPED_READS=false VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        File mergedBamFile = new File("testdata/6210_8_merged_extra_reads_no_keep.bam");
        mergedBamFile.deleteOnExit();

        File md5File = new File("testdata/6210_8_merged_extra_reads_no_keep.bam.md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals(md5, "a68d818d8e88da000f91265f3bf8bdb6");
    }
}
