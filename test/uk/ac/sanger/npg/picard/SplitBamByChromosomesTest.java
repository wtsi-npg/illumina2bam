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
    String NAME =  "SplitBamByChromosomes";

    public SplitBamByChromosomesTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    }

    @Test
        public void testMain() throws IOException {
        System.out.println("SplitBamByChromosomes instanceMain");
        
        String[] splitPaths = {
            "testdata/986_1_human_split_by_chromosome_excluded.sam", 
            "testdata/986_1_human_split_by_chromosome_target.sam" };
        String[] args = {
            "I=testdata/bam/986_1_human.sam",
            "X="+splitPaths[0],
            "T="+splitPaths[1],
            "S=1", "S=2",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };
        splitter.instanceMain(args);
        System.out.println(splitter.getCommandLine());
        String[] md5Expected = { 
            "d92bc3ec4509eaa873f62fe05ffee773",
            "8f4d23b2327ef9d0f0da4e5785cdd352"};
        for (int i=0; i<2; i++) {
            File splitFile = new File(splitPaths[i]);
            splitFile.deleteOnExit();
            assertTrue(splitFile.exists());
            String md5 = 
                CheckMd5.getBamMd5AfterRemovePGVersion(splitFile, NAME);
            System.out.println(splitPaths[i]+"\t"+md5);
            assertEquals(md5Expected[i], md5);
        }
    }

    public static void main(String[] args) throws IOException {
        SplitBamByChromosomesTest test = new SplitBamByChromosomesTest();
        test.testMain();
    }

}
