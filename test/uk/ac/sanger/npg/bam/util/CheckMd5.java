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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
     * @param inputFile 
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

    /**
     * 
     * @param input
     * @param outputFile
     * @param programIdToRemoveVersion
     */
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
     * @param file
     * @return
     */
    public static String getFileMd5(File file){
        
        String result = "";
        
        try {
            MessageDigest complete = MessageDigest.getInstance("MD5");
            
            InputStream fis =  new FileInputStream(file);
            
            int numRead;
            byte[] buffer = new byte[1024];

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            fis.close();
            
            byte [] b = complete.digest();

            for (int i=0; i < b.length; i++) {
                result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
            }
            
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CheckMd5.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CheckMd5.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;

    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        File input = new File("testdata/bam/986_1.sam");
        //ad163f4cb85d689f0d66076c32ed1743
        System.out.println(CheckMd5.getBamMd5AfterRemovePGVersion(input, "BamMerger"));        
        //ba5eeeb8aed0e6b87f9f56b110e8b36b
        System.out.println(CheckMd5.getFileMd5(input));     
    }
}
