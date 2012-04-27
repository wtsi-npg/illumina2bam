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

import java.io.FileReader;
import java.io.BufferedReader;
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
    public void testCheckBarcodeQualityMethod() throws IOException {
        
        System.out.println("test check barcode quality method");
        
        BamIndexDecoder decoder = new BamIndexDecoder();
        assertEquals("NNGATCTG", decoder.checkBarcodeQuality("CAGATCTG", "%#144=D@"));
    }

    /**
     * Test of instanceMain method and program record.
     */
    @Test
    public void testMainWithOutputBam() throws IOException {
        
        System.out.println("instanceMain - not split output by barcode");
        
        BamIndexDecoder decoder = new BamIndexDecoder();
        
        String outputName = "testdata/6383_8/6383_8";
        File outputDir = new File("testdata/6383_8");
        outputDir.mkdir();
        
        String[] args = {
            "I=testdata/bam/6383_8.sam",
            "O=" + outputName + ".sam" ,
            "BARCODE_FILE=testdata/decode/6383_8.tag",
            "METRICS_FILE=" + outputName + ".metrics",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT",
            "BARCODE_TAG_NAME=RT"
        };

        decoder.instanceMain(args);
        System.out.println(decoder.getCommandLine());
        assertEquals(decoder.getCommandLine(), "illumina.BamIndexDecoder INPUT=testdata/bam/6383_8.sam OUTPUT=testdata/6383_8/6383_8.sam BARCODE_TAG_NAME=RT BARCODE_FILE=testdata/decode/6383_8.tag METRICS_FILE=testdata/6383_8/6383_8.metrics TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    BARCODE_QUALITY_TAG_NAME=QT MAX_MISMATCHES=1 MIN_MISMATCH_DELTA=1 MAX_NO_CALLS=2 CONVERT_LOW_QUALITY_TO_NO_CALL=false MAX_LOW_QUALITY_TO_CONVERT=15 VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        File outputFile = new File(outputName + ".sam");
        File outputMetrics = new File(outputName + ".metrics");
        File outputMd5 = new File(outputName + ".sam.md5");
        
        BufferedReader md5Stream = new BufferedReader(new FileReader(outputMd5 ));
        String md5 = md5Stream.readLine();
        assertEquals(md5, "80c030cd06b2e003e8a2c1c8f7b00e66");
        
        outputFile.delete();
        outputMetrics.delete();
        outputMd5.delete();
        
        outputDir.deleteOnExit();
    }

    /**
     * Test of instanceMain method and program record to split output bam file
     */
    @Test
    public void testMainWithOutputDir() throws IOException {
        
        System.out.println("instanceMain - split output by tag");
        
        BamIndexDecoder decoder = new BamIndexDecoder();

        String outputName = "testdata/6383_8_split";
        File outputDir = new File("testdata/6383_8_split");
        outputDir.mkdir();
        
        String[] args = {
            "I=testdata/bam/6383_8.sam",
            "OUTPUT_DIR=" + outputName,
            "OUTPUT_PREFIX=6383_8",
            "OUTPUT_FORMAT=bam",            
            "BARCODE_FILE=testdata/decode/6383_8.tag",
            "METRICS_FILE=" + outputName + "/6383_8.metrics",
            "CREATE_MD5_FILE=true",
            "TMP_DIR=testdata/",
            "VALIDATION_STRINGENCY=SILENT",
            "BARCODE_TAG_NAME=RT",
            "BARCODE_QUALITY_TAG_NAME=QT",
            "CONVERT_LOW_QUALITY_TO_NO_CALL=true"
        };

        decoder.instanceMain(args);
        System.out.println(decoder.getCommandLine());
        assertEquals(decoder.getCommandLine(), "illumina.BamIndexDecoder INPUT=testdata/bam/6383_8.sam OUTPUT_DIR=testdata/6383_8_split OUTPUT_PREFIX=6383_8 OUTPUT_FORMAT=bam BARCODE_TAG_NAME=RT BARCODE_QUALITY_TAG_NAME=QT BARCODE_FILE=testdata/decode/6383_8.tag METRICS_FILE=testdata/6383_8_split/6383_8.metrics CONVERT_LOW_QUALITY_TO_NO_CALL=true TMP_DIR=[testdata] VALIDATION_STRINGENCY=SILENT CREATE_MD5_FILE=true    MAX_MISMATCHES=1 MIN_MISMATCH_DELTA=1 MAX_NO_CALLS=2 MAX_LOW_QUALITY_TO_CONVERT=15 VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false");
        
        
        File outputMetrics = new File(outputName + "/6383_8.metrics");
        outputMetrics.delete();
        String [] md5s = {"360494d01907aa3335076e4dde5f929f", "04ee98a45692a0763c0d4d8cdf44ab91", "8c98cfb67951fdacb3959c9f100b5ddb"};
        for (int i=0;i<3;i++){
            File outputFile = new File(outputName + "/6383_8#" + i + ".bam");
            outputFile.delete();
            File outputMd5 = new File(outputName + "/6383_8#" + i + ".bam.md5");
            BufferedReader md5Stream = new BufferedReader(new FileReader(outputMd5 ));
            String md5 = md5Stream.readLine();
            assertEquals(md5, md5s[i]);            
            outputMd5.delete();
        }
        
        outputDir.deleteOnExit();
    }
}
