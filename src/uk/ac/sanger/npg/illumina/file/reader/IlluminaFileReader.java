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
package uk.ac.sanger.npg.illumina.file.reader;

import java.io.*;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import net.sf.picard.util.Log;


/**
 * This class tries to open a Illumina file into a data input stream. 
 * It is a base class for all file reader classes.
 * 
 * @author gq1@sanger.ac.uk
 */
public class IlluminaFileReader implements Iterator<Object>, Closeable {
    
    private final Log log = Log.getInstance(IlluminaFileReader.class);
    
    protected final String fileName;
    protected DataInputStream inputStream;

    /**
     *
     * @param fileName bcl, scl, clocs, locs, pos and filter etc Illumina file name
     * @throws FileNotFoundException, IOException
     */
    public IlluminaFileReader(String fileName) throws FileNotFoundException, IOException {

        this.fileName = fileName;
        this.openInputFile(fileName);
    }

    /**
     * check file, open it if it is valid
     * @param fileName
     * @throws Exception
     */
    private void openInputFile(String fileName) throws FileNotFoundException, IOException {
        /*
          fileName is not necessarily a complete path.
          If fileName ends with .gz, assume it is the path to a gzipped file.
          Otherwise, input path may or may not be gzipped and have .gz suffix.
         */
        if (fileName == null) {
            throw new IllegalArgumentException("File name must be given.");
        } else {
            boolean gzip = false;
            File file = new File(fileName);
            if (fileName.endsWith(".gz")) {
                gzip = true;
            } else if (!file.exists()) { 
                // uncompressed file not found, try with .gz suffix
                gzip = true;
                file = new File(fileName+".gz");
            }
            // check validity of File object and open relevant input stream
            if (!file.exists()) {
                throw new FileNotFoundException("Input does not exist: " 
                                                + fileName);
            } else if (file.isDirectory()) {
                throw new IllegalArgumentException("File name is a directory: " 
                                                   + fileName);
            } else if (!file.canRead()) {
                throw new FileNotFoundException("Input cannot be read: " 
                                                + fileName);
            } else {
                InputStream inputBase;
                if (gzip) {
                    // constructor may throw IOException
                    inputBase = new GZIPInputStream(new FileInputStream(file));
                } else {
                    inputBase = new FileInputStream(file);
                }
                this.inputStream = 
                    new DataInputStream(new BufferedInputStream(inputBase));
            }
        }
    }

    @Override
    public boolean hasNext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object next() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * close file input stream
     */
    @Override
    public void close() {

        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException ex) {
                log.error(ex, "Cannot close file");
            }
        }
    }

    /**
     * read four bytes from its input stream and convert to unsigned integer
     * @return an unsigned integer
     * @throws IOException
     */
    public int readFourBytes() throws IOException {
        return this.readFourBytes(this.inputStream);
    }

    /**
     * read four bytes from an input stream and convert to unsigned integer
     * @param inputStream
     * @return an unsigned integer
     * @throws IOException
     */
    public int readFourBytes(DataInputStream inputStream) throws IOException {

        int unsignedInt = 0;

        byte[] fourBytes = new byte[4];
        inputStream.read(fourBytes);
        for (int i = 0; i < 4; i++) {
            int intValue = (int) (fourBytes[i] & 0xFF);
            unsignedInt = unsignedInt + (intValue << 8 * i);
        }

        return unsignedInt;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }
}
