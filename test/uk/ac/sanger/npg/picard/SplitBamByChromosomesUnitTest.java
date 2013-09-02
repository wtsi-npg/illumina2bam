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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import net.sf.picard.io.IoUtil;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import uk.ac.sanger.npg.bam.util.CheckMd5;

/*
  Test class for SplitBamByChromosomes
*/

/**
 *
 * @author kt6@sanger.ac.uk
 * 
 */

public class SplitBamByChromosomesUnitTest {

    SplitBamByChromosomes splitter = new SplitBamByChromosomes();
	String NAME =  "SplitBamByChromosomes";

    public SplitBamByChromosomesUnitTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    }

    @Test
        public void testMain() throws IOException {
        System.out.println("SplitBamByChromosomes instanceMain: xahuman test case");
        
        String[] splitPaths = {
	        "testdata/10503_1_human_split_by_chromosome_excluded.sam", 
	        "testdata/10503_1_human_split_by_chromosome_target.sam" };
        String[] args = {
	        "I=testdata/bam/10503_1.sam",
	        "X="+splitPaths[0],
	        "T="+splitPaths[1],
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };
        splitter.instanceMain(args);
        System.out.println(splitter.getCommandLine());
     
        
        String[] expectedReadNamesInExcluded = {
        		"first_chimeric",		
        		"twenty_twenty",
        		"unmapped_other",
        		"other_unmapped",
        		"second_chimeric"
        };
        
        String[] expectedReadNamesInTarget = {
        		"MT_MT",
        		"y_and_y",
        		"pair_unmapped",
           		"first_unmapped", 
        		"second_unmapped"
        };
        
        assertEquals(expectedReadNamesInExcluded.length, 5);
        assertEquals(expectedReadNamesInTarget.length, 5);
        Arrays.sort(expectedReadNamesInExcluded);
        Arrays.sort(expectedReadNamesInTarget);
        
        for (int i=0; i<2; i++) {
        	
	        File splitFile = new File(splitPaths[i]);
	        splitFile.deleteOnExit();
	        assertTrue(splitFile.exists());
         
	        IoUtil.assertFileIsReadable(splitFile);
	        final SAMFileReader check = new SAMFileReader(splitFile);
	        
	        ArrayList<String> readNamesInFile = new ArrayList<String>();
	        for (SAMRecord rec: check){
	        	String qname = rec.getReadName();
	        
	        	if (!readNamesInFile.contains(qname)){
	        		readNamesInFile.add(qname);
	        	}
	        }
	        
	        Collections.sort(readNamesInFile);        
	        Object[] readNames = readNamesInFile.toArray();

	        if (i == 0) {
	        	 System.out.println(splitPaths[0]+"\t checking read groups in excluded");
	        	 assertEquals(readNamesInFile.toArray().length, 5);
	             assertArrayEquals(expectedReadNamesInExcluded, readNames);
	        }
	        else {
	            System.out.println(splitPaths[1]+"\t checking read groups in target");
	            assertEquals(readNamesInFile.toArray().length, 5);
	            assertArrayEquals(expectedReadNamesInTarget, readNames);
	        }
        }
        
    }

    public static void main(String[] args) throws IOException {
        SplitBamByChromosomesTest test = new SplitBamByChromosomesTest();
        test.testMain();
    }

}
