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
 */

package uk.ac.sanger.npg.picard;

import java.io.File;
import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.Test;
import uk.ac.sanger.npg.picard.IndexDecoder.NamedBarcode;

/**
 * Test Class for IndexDecoder
 * 
 * @author gq1@sanger.ac.uk
 */
public class IndexDecoderTest {

    @Test
    public void testParseBarcodeString(){
        
        System.out.println("checking parseBarcodeString method");
        
        IndexDecoder decoder = new IndexDecoder();
        
        ArrayList<String> messages = new ArrayList<String>();
        ArrayList<String> barcodeString = new ArrayList<String>(2);
        barcodeString.add("ATCACGTT");
        barcodeString.add("ATCAC");
        
        ArrayList<NamedBarcode> barcodeList = decoder.parseBarcodeString(messages, barcodeString);
        assertEquals(messages.size(), 1);
        assertEquals(messages.get(0), "Barcode ATCAC has different length from first barcode.");

        barcodeString.remove(1);
        barcodeString.add("TATCTGGA");
        messages.clear();
        barcodeList = decoder.parseBarcodeString(messages, barcodeString);
        assertEquals(messages.size(), 0);
        assertEquals(barcodeList.get(1).barcode, "TATCTGGA");
        assertEquals(barcodeList.get(0).barcodeName, "");
        assertEquals(barcodeList.get(0).sampleName, "");
        assertEquals(barcodeList.get(1).libraryName, "");
        assertEquals(barcodeList.get(1).description, "");
    }    
 
    @Test
    public void testParseBarcodeFile(){
        
        System.out.println("checking parseBarcodeFile method");
        
        IndexDecoder decoder = new IndexDecoder();
        
        ArrayList<String> messages = new ArrayList<String>();
        File barcodeFile = new File("testdata/decode/lane_1.tag");
        
        ArrayList<NamedBarcode> barcodeList = decoder.parseBarcodeFile(messages, barcodeFile);
        assertEquals(barcodeList.size(), 2);
        assertTrue(messages.isEmpty());

        assertEquals(barcodeList.get(0).barcode, "ATCACGTT");
        assertEquals(barcodeList.get(1).barcodeName, "2");
        assertEquals(barcodeList.get(1).libraryName, "testlib2");
        assertEquals(barcodeList.get(1).sampleName, "testsample2");
        assertEquals(barcodeList.get(0).sampleName, "testsample1");
        assertEquals(barcodeList.get(1).description, "study2");
    }

}
