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

    /*
    xahuman S=Y,MT
    TARGET = .bam
    EXCLUDED = xahuman.bam
    U = false

    EXCLUDE
    UNALIGN read1 read2 destination
    0		[Y|MT]	[Y|MT]	TARGET
    0		[Y|MT]	20		EXCLUDED
    0		20		[Y|MT]	EXCLUDED  
    0		[Y|MT]	*		TARGET
    0		*		[Y|MT]	TARGET
    0		20		20		EXCLUDED
    0		*		*		TARGET

    */

    @Test
        public void testxahuman() throws IOException {
        System.out.println("SplitBamByChromosomes instanceMain: xahuman test case, V=false, U=false");
        
        String[] splitPaths = {
            "testdata/10503_1_human_split_by_chromosome_excluded.sam", 
            "testdata/10503_1_human_split_by_chromosome_target.sam" };
        String[] args = {
            "I=testdata/bam/10503_1_fix_mate.sam",
            "X="+splitPaths[0],
            "T="+splitPaths[1],
            "U=false",
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
        
        assertEquals(5, expectedReadNamesInExcluded.length);
        assertEquals(5, expectedReadNamesInTarget.length);
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
                 assertEquals(5, readNamesInFile.toArray().length);
                 assertArrayEquals(expectedReadNamesInExcluded, readNames);
            }
            else {
                System.out.println(splitPaths[1]+"\t checking read groups in target");
                assertEquals(5, readNamesInFile.toArray().length);
                assertArrayEquals(expectedReadNamesInTarget, readNames);
            }
        }
        
    }
 
    /* 
    xahuman S=Y,MT
    TARGET = .bam
    EXCLUDED = xahuman.bam
    U = true

  EXCLUDE
    UNALIGN read1 read2 destination
    1		[Y|MT]	[Y|MT]	TARGET
    1		[Y|MT]	20		EXCLUDED
    1		20		[Y|MT]	EXCLUDED  
    1		[Y|MT]	*		EXCLUDED
    1		*		[Y|MT]	EXCLUDED
    1		20		20		EXCLUDED
    1		*		*		EXCLUDED

    */
    
    @Test
    public void testxahuman_exclude_unaligned() throws IOException {
    System.out.println("SplitBamByChromosomes instanceMain: xahuman test case, V=false, U=false");
    
    String[] splitPaths = {
        "testdata/10503_1_human_split_by_chromosome_excluded_no_unaligned.sam", 
        "testdata/10503_1_human_split_by_chromosome_target_no_unaligned.sam" };
    String[] args = {
        "I=testdata/bam/10503_1_fix_mate.sam",
        "X="+splitPaths[0],
        "T="+splitPaths[1],
        "U=true",
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
            "second_chimeric",
            "pair_unmapped",
            "first_unmapped", 
            "second_unmapped"
    };
    
    String[] expectedReadNamesInTarget = {
            "MT_MT",
            "y_and_y"		
    };
    
    assertEquals(8, expectedReadNamesInExcluded.length);
    assertEquals(2, expectedReadNamesInTarget.length);
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
                System.out.println("Found qname " + qname + " in file " + i );
            }
        }
        
        Collections.sort(readNamesInFile);        
        Object[] readNames = readNamesInFile.toArray();

        if (i == 0) {
             System.out.println(splitPaths[0]+"\t checking read groups in excluded");
             assertEquals(8, readNamesInFile.toArray().length);
             assertArrayEquals(expectedReadNamesInExcluded, readNames);
        }
        else {
            System.out.println(splitPaths[1]+"\t checking read groups in target");
            assertEquals(2, readNamesInFile.toArray().length);
            assertArrayEquals(expectedReadNamesInTarget, readNames);
        }
    }
    
}

    /*
 yhuman S=Y V=true
TARGET = .bam
EXCLUDED = yhuman.bam 

EXCLUDE
UNALIGN read1 read2 destination
0		Y		Y	EXCLUDED
0		Y		20	EXCLUDED
0		20		Y	EXCLUDED	
0		Y		*	EXCLUDED
0		*		Y	EXCLUDED     	
0		*		*	TARGET
0		20		20	TARGET
0		20		*	TARGET

1		Y		Y	EXCLUDED
1		Y		20	EXCLUDED
1		20		Y	EXCLUDED	
1		Y		*	EXCLUDED
1		*		Y	EXCLUDED     	
1		20		*	EXCLUDED
1		*		*	EXCLUDED
1		20		20	TARGET
     */
    
    @Test
    public void testyhuman() throws IOException {
    System.out.println("SplitBamByChromosomes instanceMain: yhuman test case with V=true");
    
    String[] splitPaths = {
        "testdata/10503_1_yhuman.sam",
        "testdata/10503_1.sam", };
    String[] args = {
        "I=testdata/bam/10503_1.sam",
        "X="+splitPaths[0],
        "T="+splitPaths[1],
        "S=Y",
        "U=false",
        "V=TRUE",
        "TMP_DIR=testdata/",
        "VALIDATION_STRINGENCY=SILENT"
    };
    splitter.instanceMain(args);
    System.out.println(splitter.getCommandLine());
    
    String[] expectedReadNamesInExcluded = {
            "first_chimeric",
            "second_chimeric",			 	
            "first_unmapped", 
            "second_unmapped",
            "y_and_y"
    };
    
    String[] expectedReadNamesInTarget = {
            "twenty_twenty", 
            "MT_MT", 
            "unmapped_other",
            "other_unmapped",
            "pair_unmapped"
    };
    
    assertEquals(5,expectedReadNamesInExcluded.length);
    assertEquals(5, expectedReadNamesInTarget.length);
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
            assertArrayEquals(expectedReadNamesInExcluded, readNames);
            assertEquals(5, readNamesInFile.toArray().length);
        }
        else {       
            System.out.println(splitPaths[0]+"\t checking read groups in target");
            assertArrayEquals(expectedReadNamesInTarget, readNames);
            assertEquals(5, readNamesInFile.toArray().length);
        }
    }
   
    
}

    public static void main(String[] args) throws IOException {
        SplitBamByChromosomesUnitTest test = new SplitBamByChromosomesUnitTest();
        
        test.testxahuman();
        test.testxahuman_exclude_unaligned();
        test.testyhuman();
    }

}
