
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

/**
 * illumina2bam util class
 *
 * @author Guoying Qi
 */
public final class Illumina2bamUtils {
    
     /**
     *
     * @param array
     * @return fastq quality string from phred qual byte array
     */
    public static String convertPhredQualByteArrayToFastqString(byte [] array){
        StringBuilder builder = new StringBuilder(array.length);
        for (byte b : array){
            builder.append( (char) (b + 33) );
        }
        return builder.toString();
    }

    /**
     * 
     * @param array
     * @return string from a byte array
     */
    public static String convertByteArrayToString(byte [] array){
        StringBuilder builder = new StringBuilder(array.length);
        for (byte b : array){
            builder.append((char) b);
        }
        return builder.toString();
    }
    
}
