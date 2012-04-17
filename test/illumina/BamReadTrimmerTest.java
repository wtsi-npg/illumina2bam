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

import java.io.*;
import java.util.TimeZone;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * This is the test class for BamReadTrimmer
 * 
 * @author Guoying Qi
 */

public class BamReadTrimmerTest {
    
    BamReadTrimmer trimmer = new BamReadTrimmer();
    
    public BamReadTrimmerTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain");
        
        String[] args = {
            "I=testdata/bam/6210_8.sam",
            "O=testdata/6210_8_trimmed.bam",
            "POS=1",
            "LEN=3",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        trimmer.instanceMain(args);
        System.out.println(trimmer.getCommandLine());
 
        assertEquals(trimmer.getCommandLine(), "illumina.BamReadTrimmer "
                + "INPUT=testdata/bam/6210_8.sam "
                + "OUTPUT=testdata/6210_8_trimmed.bam "
                + "FIRST_POSITION_TO_TRIM=1 TRIM_LENGTH=3 "
                + "TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT "
                + "CREATE_MD5_FILE=true    ONLY_FORWARD_READ=true "
                + "SAVE_TRIM=true TRIM_BASE_TAG=rs TRIM_QUALITY_TAG=qs "
                + "VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 "
                + "MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"
              );

    }
    
    /**
     * Test trimming sam record methods
     */
    @Test
    public void testTrimmingSamRecord(){
        System.out.println("Testing trimming sam record");
        
        String bases     = "CCCTCCTACTACCACCAAAATTT";
        String qualities = "!998997<99DDDDD<>>><<><";
        
        SAMFileHeader header = new SAMFileHeader();        
        SAMRecord record = new SAMRecord(header);
        record.setReadNegativeStrandFlag(false);
        record.setReadString(bases);
        record.setBaseQualityString(qualities);
            
        trimmer.trimSAMRecord(record, 1, 5, true);
        
        assertEquals(record.getReadString(), "CTACTACCACCAAAATTT");
        assertEquals(record.getBaseQualityString(), "97<99DDDDD<>>><<><");
        
        assertEquals(record.getAttribute("rs"),"CCCTC");
        assertEquals(record.getAttribute("qs"),"!9989");
    }
    
    /**
     * Test trimming sam record methods
     */
    @Test
    public void testTrimmingReversedSamRecord(){
        System.out.println("Testing trimming reversed reads");
        
        String bases     = "CCCTCCTACTACCACCAAAATTT";
        String qualities = "!998997<99DDDDD<>>><<><";
        
        SAMFileHeader header = new SAMFileHeader();        
        SAMRecord record = new SAMRecord(header);
        record.setReadString(bases);
        record.setBaseQualityString(qualities);
        record.setReadNegativeStrandFlag(true);
        
        trimmer.trimSAMRecord(record, 1, 5, true);
        
        assertEquals(record.getReadString(), "CCCTCCTACTACCACCAA");
        assertEquals(record.getBaseQualityString(), "!998997<99DDDDD<>>");
        assertEquals(record.getAttribute("rs"),"AAATT");
        assertEquals(record.getAttribute("qs"),"<><<>");
        
    }
    
    /**
     * Test output bam MD5
     */
    @Test
    public void testOutputBam() throws FileNotFoundException, IOException {
        System.out.println("checking bam md5");
        File trimmedBamFile = new File("testdata/6210_8_trimmed.bam");
        trimmedBamFile.deleteOnExit();

        File md5File = new File("testdata/6210_8_trimmed.bam.md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals(md5, "0996da7e4a6a6ebcb89e943eea63c85d");

    }

}
