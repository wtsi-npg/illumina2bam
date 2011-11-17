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

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.util.TimeZone;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This is the test class for ChangeBamHeader
 * 
 * @author Guoying Qi
 */

public class ChangeBamHeaderTest {
    
    ChangeBamHeader changer = new ChangeBamHeader();
    
    public ChangeBamHeaderTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }
    
    /**
     * PG must be given in tag name and value in pair and separated by :
     */
    @Test(expected = RuntimeException.class)
    public void testGetProgramRecordFromString1(){
        changer.getProgramRecordFromString("ID samtools");
    }
    
    /**
     * PG must be given with ID tag and value
     */
    @Test(expected = RuntimeException.class)
    public void testGetProgramRecordFromString2(){
        changer.getProgramRecordFromString("PN:samtools;VN:0.17");
    } 

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain");
        
        String[] args = {
            "I=testdata/bam/6210_8.sam",
            "O=testdata/6210_8_header_changed.bam",
            "PG=ID:samtools_sorting;PN:samtools;VN:0.1.12a (r862);CL:samtools sort",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT",
            "LB=newLB",
            "SM=newSM",
            "DS=newDS",               
        };

        changer.instanceMain(args);
        
        System.out.println(changer.getCommandLine());
        assertEquals(changer.getCommandLine(),
                "illumina.ChangeBamHeader INPUT=testdata/bam/6210_8.sam OUTPUT=testdata/6210_8_header_changed.bam PG=[ID:samtools_sorting;PN:samtools;VN:0.1.12a (r862);CL:samtools sort] SAMPLE=newSM LIBRARY=newLB DESCRIPTION=newDS TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"
                    );
    }
    
    /**
     * Test output bam MD5
     */
    @Test
    public void testOutputBam() throws FileNotFoundException, IOException {
        System.out.println("checking bam md5");
        File newBamFile = new File("testdata/6210_8_header_changed.bam");
        newBamFile.deleteOnExit();

        File md5File = new File("testdata/6210_8_header_changed.bam.md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals(md5, "8acdf8b4f327994a0c1dc1f387f359dd");
    }
}
