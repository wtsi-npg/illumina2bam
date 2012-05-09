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
 * The test class for SCLFileReader
 * 
 */
package uk.ac.sanger.npg.illumina.file.reader;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class SCLFileReaderTest {

    private static String testSCLFile = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/s_1_1101.scl";
    private static SCLFileReader sclFileReader;

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Create a scl file reader");
        sclFileReader = new SCLFileReader(testSCLFile);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("Close the scl file reader");
        sclFileReader.close();
    }

    @Test
    public void checkBCLHeaderOK() {
        System.out.println("Read the scl file header");
        assertEquals(sclFileReader.getTotalClusters(), 2609912);
        assertEquals(sclFileReader.getCurrentCluster(), 0);
        assertTrue(sclFileReader.hasNext());
    }

    @Test
    public void checkNextFirstClusterOK() {
        System.out.println("Read the first clsuter from scl file");
        char cluster = sclFileReader.next();
        assertEquals(cluster, 'A');
        assertEquals(sclFileReader.getCurrentCluster(), 1);
        assertTrue(sclFileReader.hasNext());
    }

    @Test
    public void checkNextMiddleClusterOK() {
        System.out.println("Read more clusters and check 307th cluster");
        for (int i = 0; i < 305; i++) {
            sclFileReader.next();
        }
        char cluster = sclFileReader.next();
        assertEquals(cluster, 'T');

        assertEquals(sclFileReader.getCurrentCluster(), 307);
        assertEquals(sclFileReader.getTotalClusters(), 2609912);
        assertTrue(sclFileReader.hasNext());
    }

    @Test
    public void checkNextLastClusterOK() {

        System.out.println("Read more clusters till the last one");
        
        char cluster = ' ';

        while (sclFileReader.hasNext()) {
            cluster = sclFileReader.next();
        }
        assertEquals(cluster, 'C');
        assertEquals(sclFileReader.getCurrentCluster(), 2609912);
        assertEquals(sclFileReader.getTotalClusters(), 2609912);
        assertFalse(sclFileReader.hasNext());
        assertNull(sclFileReader.next());
    }
}
