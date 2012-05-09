
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

package uk.ac.sanger.npg.bam.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class BamUtilsTest {

    /**
     * Test of convertPhredQualByteArrayToFastqString method, of class BamUtils.
     */
    @Test
    public void testConvertPhredQualByteArrayToFastqString() {
        System.out.println("convertPhredQualByteArrayToFastqString");
        byte[] array = {0, 25, 34, 45};
        String expResult = "!:CN";
        String result = BamUtils.convertPhredQualByteArrayToFastqString(array);
        assertEquals(expResult, result);
    }

    /**
     * Test of convertByteArrayToString method, of class BamUtils.
     */
    @Test
    public void testConvertByteArrayToString() {
        System.out.println("convertByteArrayToString");
        byte[] array = {50, 36, 46};
        String expResult = "2$.";
        String result = BamUtils.convertByteArrayToString(array);
       assertEquals(expResult, result);
    }
}
