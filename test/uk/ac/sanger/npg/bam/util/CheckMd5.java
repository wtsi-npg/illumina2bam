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
 * This is the test class for Illumina2bam
 *
 */
package uk.ac.sanger.npg.bam.util;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.*;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class CheckMd5 {
        

    /**
     *
     * @param programIdToRemoveVersion
     * @return md5 value
     */
    public static String getBamMd5AfterRemovePGVersion(File inputFile, String programIdToRemoveVersion) {
        
        SAMFileWriterFactory.setDefaultCreateMd5File(true);

        File outputFile = null;
        try {
            outputFile = File.createTempFile("removeVersion", ".bam");
            outputFile.deleteOnExit();
            Logger.getLogger(CheckMd5.class.getName()).log(Level.INFO, "Output after removing PG version: {0}", outputFile.getPath());
        } catch (IOException ex) {
            Logger.getLogger(CheckMd5.class.getName()).log(Level.SEVERE, null, ex);
        }

        removeVersionNumberFromBamHeader(inputFile, outputFile, programIdToRemoveVersion);

        File md5File = new File(outputFile.getPath() + ".md5");
        md5File.deleteOnExit();

        BufferedReader md5Stream = null;
        String md5AfterRemovePgVersion = null;
        try {
            md5Stream = new BufferedReader(new FileReader(md5File));
            md5AfterRemovePgVersion = md5Stream.readLine();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CheckMd5.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CheckMd5.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                md5Stream.close();
            } catch (IOException ex) {
                Logger.getLogger(CheckMd5.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return md5AfterRemovePgVersion;
    }

    public static void removeVersionNumberFromBamHeader(File input, File outputFile, String programIdToRemoveVersion) {

        Logger.getLogger(CheckMd5.class.getName()).log(Level.INFO, "Input file for check md5: {0}", input.getPath());

        final SAMFileReader in = new SAMFileReader(input);

        final SAMFileHeader header = in.getFileHeader();
        final SAMFileHeader outputHeader = header.clone();
        SAMProgramRecord pg = outputHeader.getProgramRecord(programIdToRemoveVersion);
        if (pg != null) {
            pg.setProgramVersion(null);
        }

        final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader, true, outputFile);

        for (final SAMRecord record : in) {

            out.addAlignment(record);
        }

        out.close();
        in.close();
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        File input = new File("testdata/bam/986_1.sam");
        System.out.println(CheckMd5.getBamMd5AfterRemovePGVersion(input, "BamMerger"));
    }
}
