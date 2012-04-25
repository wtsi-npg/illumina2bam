
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

import java.io.*;
import java.util.TimeZone;
import net.sf.samtools.SAMUtils;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
/**
 * This is the test class for BamQualityQuantisation
 * 
 * @author gq1@sanger.ac.uk
 */

public class BamQualityQuantisationTest {
    
    BamQualityQuantisation qualObj = new BamQualityQuantisation();

    public BamQualityQuantisationTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }
    
    @Test
    public void testQuantiseQualities(){
        
        System.out.println("quantiseQualities");
        
        String qualityString = "B@FHGJIEFGHIJ0-7-?C!,";
        byte[] quals = SAMUtils.fastqToPhred(qualityString);
        
        byte [] newQual = qualObj.quantiseQualities(quals);
        byte [] expected = {33,33,37,37,37,41,41,37,37,37,37,41,41,15,15,22,15,33,33,0,15};
        assertArrayEquals(newQual, expected);
        
        newQual = qualObj.quantiseQualities(qualityString);
        assertArrayEquals(newQual, expected);
    }
    
    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        
        System.out.println("instanceMain");
        
        String[] args = {
            "I=testdata/bam/6210_8.sam",
            "O=testdata/6210_8_squashed.bam",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        qualObj.instanceMain(args);   
        assertEquals(qualObj.getCommandLine(), "illumina.BamQualityQuantisation INPUT=testdata/bam/6210_8.sam OUTPUT=testdata/6210_8_squashed.bam TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    USE_OLD_QUALITY=false VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");

    }
        
    /**
     * Test output bam MD5
     */
    @Test
    public void testOutputBam() throws FileNotFoundException, IOException {
        
        System.out.println("checking output bam md5");
        
        File squashedBamFile = new File("testdata/6210_8_squashed.bam");
        squashedBamFile.deleteOnExit();

        File md5File = new File("testdata/6210_8_squashed.bam.md5");
        md5File.deleteOnExit();
        BufferedReader md5Stream = new BufferedReader(new FileReader(md5File));
        String md5 = md5Stream.readLine();

        assertEquals(md5, "5fe494967954b163c8671f27178616ef");
    }
 
}
