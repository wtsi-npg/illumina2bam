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
import java.util.TimeZone;
import net.sf.samtools.SAMFileReader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import uk.ac.sanger.npg.bam.util.CheckMd5;

/*
  Test class for SplitBamByChromosomes
*/

/**
 *
 * @author ib5@sanger.ac.uk
 * 
 */

public class SplitBamByChromosomesTest {

    SplitBamByChromosomes splitter = new SplitBamByChromosomes();

    public SplitBamByChromosomesTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    }

    @Test
        public void testMain() throws IOException {
        System.out.println("SplitBamByChromosomes instanceMain");
        
        String[] args = {
            "I=testdata/bam/986_1_human.sam",
            "O=testdata/986_1_human_split_by_chromosome",
	    "S=1,2:5",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        splitter.instanceMain(args);
        System.out.println(splitter.getCommandLine());
        String[] splitPaths = { "testdata/986_1_human_split_by_chromosome_000.sam", 
                                "testdata/986_1_human_split_by_chromosome_001.sam", 
                                "testdata/986_1_human_split_by_chromosome_002.sam" };
        String[] md5Expected = { "c32641667d464fa50457ff29cb5e0c15",
                                 "25dfcd9f58accd05a9eab4a8270a6eca",
                                 "14235534b5c6eddb4a11285228e0a57a"};

        for (int i=0; i<3; i++) {
            File splitFile = new File(splitPaths[i]);
            splitFile.deleteOnExit();
            assertTrue(splitFile.exists());
            String md5 = CheckMd5.getBamMd5AfterRemovePGVersion(splitFile, "SplitBamByChromosomes");
            System.out.println(splitPaths[i]+"\t"+md5);
            assertEquals(md5, md5Expected[i]);
        }
    }

    public static void main(String[] args) throws IOException {
        SplitBamByChromosomesTest test = new SplitBamByChromosomesTest();
        test.testMain();
    }

}