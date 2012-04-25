/*
 * Copyright (C) 2012 GRL
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
 * This is the test class for BamReadTrimmer
 * 
 * @author Guoying Qi
 */

public class BamTagStripperTest {
    
    BamTagStripper stripper = new BamTagStripper();
    
    public BamTagStripperTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain");
        
        String[] args = {
            "I=testdata/bam/7351_8#8.sam",
            "O=testdata/7351_8#8_stripped.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        stripper.instanceMain(args);
        System.out.println(stripper.getCommandLine());
        assertEquals(stripper.getCommandLine(), "illumina.BamTagStripper INPUT=testdata/bam/7351_8#8.sam OUTPUT=testdata/7351_8#8_stripped.bam TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");

    }
    
    /**
     * Test output bam MD5
     */
    @Test
    public void testOutputBam() throws FileNotFoundException, IOException {
        System.out.println("checking bam md5");
        String md5 = getMd5();
        assertEquals(md5, "141c0b21537f43c9b0c6acd981ec0e5a");
    }
    
    @Test
    public void testMainKeepCiStrippOQ() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMainKeepCiStrippOQ");
        
        String[] args = {
            "I=testdata/bam/7351_8#8.sam",
            "O=testdata/7351_8#8_stripped.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "KEEP=ci",
            "STRIP=OQ",
            "VALIDATION_STRINGENCY=SILENT"
        };

        stripper.instanceMain(args);
        System.out.println(stripper.getCommandLine());
        assertEquals(stripper.getCommandLine(), "illumina.BamTagStripper INPUT=testdata/bam/7351_8#8.sam OUTPUT=testdata/7351_8#8_stripped.bam TAG_TO_KEEP=[ci] TAG_TO_STRIP=[OQ] TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");

    }
    /**
     * Test output bam MD5
     */
    @Test
    public void testOutputBamKeepCiStrippOQ() throws FileNotFoundException, IOException {
        System.out.println("checking bam md5");
        String md5 = getMd5();
        assertEquals(md5, "466f52d961403ba3e5b79632c8a3aad0");
    }    
    
    private String getMd5() throws FileNotFoundException, IOException{
        File strippedBamFile = new File("testdata/7351_8#8_stripped.bam");
        strippedBamFile.delete();
        File md5File = new File("testdata/7351_8#8_stripped.bam.md5");        
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();
        md5File.delete();
        return md5;
    }

}
