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

package illumina;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gq1@sanger.ac.uk
 */

public class BamIndexDecoderTest {
    
    public BamIndexDecoderTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }
    
    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMainWithOutputBam() throws IOException {
        
        System.out.println("instanceMain");
        
        BamIndexDecoder decoder = new BamIndexDecoder();
        
        File outputDir = File.createTempFile( "6383_8_", "bam", new File("testdata/") );
        outputDir.delete();
        outputDir.mkdir();
        
        String outputName = "testdata/" + outputDir.getName() + "/6383_8";
        
        String[] args = {
            "I=testdata/bam/6383_8.sam",
            "O=" + outputName + ".bam" ,
            "BARCODE_FILE=testdata/decode/6383_8.tag",
            "METRICS_FILE=" + outputName + ".metrics",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        decoder.instanceMain(args);
        System.out.println(decoder.getCommandLine());
        assertEquals(decoder.getCommandLine(), "illumina.BamIndexDecoder "
                + "INPUT=testdata/bam/6383_8.sam "
                + "OUTPUT=" + outputName + ".bam "
                + "BARCODE_FILE=testdata/decode/6383_8.tag "
                + "METRICS_FILE="+ outputName + ".metrics "
                + "TMP_DIR=testdata VALIDATION_STRINGENCY=SILENT "
                + "CREATE_MD5_FILE=true    "
                + "BARCODE_TAG_NAME=RT MAX_MISMATCHES=1 MIN_MISMATCH_DELTA=1 MAX_NO_CALLS=2 "
                + "VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        File outputFile = new File(outputName + ".bam");
        File outputMetrics = new File(outputName + ".metrics");
        File outputMd5 = new File(outputName + ".bam.md5");
        outputFile.delete();
        outputMetrics.delete();
        outputMd5.delete(); 
        outputDir.delete();
    }

    /**
     * Test of instanceMain method and program record to split output bam file
     */
    @Test
    public void testMainWithOutputDir() throws IOException {
        
        System.out.println("instanceMain");
        
        BamIndexDecoder decoder = new BamIndexDecoder();
        
        File outputDir = File.createTempFile( "6383_8_split_", "bam", new File("testdata/") );
        outputDir.delete();
        outputDir.mkdir();
        
        String outputName = "testdata/" + outputDir.getName(); 
        
        String[] args = {
            "I=testdata/bam/6383_8.sam",
            "OUTPUT_DIR=" + outputName,
            "OUTPUT_PREFIX=6383_8",
            "OUTPUT_FORMAT=bam",            
            "BARCODE_FILE=testdata/decode/6383_8.tag",
            "METRICS_FILE=" + outputName + "/6383_8.metrics",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT"
        };

        decoder.instanceMain(args);
        System.out.println(decoder.getCommandLine());
        assertEquals(decoder.getCommandLine(), "illumina.BamIndexDecoder "
                + "INPUT=testdata/bam/6383_8.sam "
                + "OUTPUT_DIR=" + outputName + " "
                + "OUTPUT_PREFIX=6383_8 "
                + "OUTPUT_FORMAT=bam "
                + "BARCODE_FILE=testdata/decode/6383_8.tag "
                + "METRICS_FILE=" + outputName + "/6383_8.metrics "
                + "TMP_DIR=testdata VALIDATION_STRINGENCY=SILENT "
                + "CREATE_MD5_FILE=true    BARCODE_TAG_NAME=RT "
                + "MAX_MISMATCHES=1 MIN_MISMATCH_DELTA=1 MAX_NO_CALLS=2 "
                + "VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false"
         );
        
        
        File outputMetrics = new File(outputName + "/6383_8.metrics");
        outputMetrics.delete();
        
        for (int i=0;i<3;i++){
            File outputFile = new File(outputName + "/6383_8#" + i + ".bam");
            outputFile.delete();
            File outputMd5 = new File(outputName + "/6383_8#" + i + ".bam.md5");
            outputMd5.delete();
        }
         outputDir.delete();
    }
}
