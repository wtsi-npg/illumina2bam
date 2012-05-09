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
package uk.ac.sanger.npg.picard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TimeZone;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import uk.ac.sanger.npg.bam.util.CheckMd5;

/**
 * This is the test class for BamReadTrimmer
 * 
 * @author gq1@sanger.ac.uk
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
        assertEquals(stripper.getCommandLine(), "uk.ac.sanger.npg.picard.BamTagStripper INPUT=testdata/bam/7351_8#8.sam OUTPUT=testdata/7351_8#8_stripped.bam TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");

    }
    
    /**
     * Test output bam MD5
     */
    @Test
    public void testOutputBam() throws FileNotFoundException, IOException {
        System.out.println("checking bam md5");
        File strippedBamFile = new File("testdata/7351_8#8_stripped.bam");
        File md5File = new File("testdata/7351_8#8_stripped.bam.md5");        
        md5File.delete();
        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(strippedBamFile, "BamTagStripper"), "9d8e2754e815109d0cc1a235e805451f");
        strippedBamFile.delete();
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
        assertEquals(stripper.getCommandLine(), "uk.ac.sanger.npg.picard.BamTagStripper INPUT=testdata/bam/7351_8#8.sam OUTPUT=testdata/7351_8#8_stripped.bam TAG_TO_KEEP=[ci] TAG_TO_STRIP=[OQ] TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");

    }
    /**
     * Test output bam MD5
     */
    @Test
    public void testOutputBamKeepCiStrippOQ() throws FileNotFoundException, IOException {
        System.out.println("checking bam md5");
        File strippedBamFile = new File("testdata/7351_8#8_stripped.bam");
        File md5File = new File("testdata/7351_8#8_stripped.bam.md5");        
        md5File.delete();
        assertEquals(CheckMd5.getBamMd5AfterRemovePGVersion(strippedBamFile, "BamTagStripper"), "e745e525e75d6f4a0427e07fcdeec02e");
        strippedBamFile.delete();
    }    


}
