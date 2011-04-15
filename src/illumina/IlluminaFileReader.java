/*
 * This class try to open a Illumina file into a data input stream
 */
package illumina;

import java.io.Closeable;
import java.util.Iterator;

import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.File;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Guoying Qi
 */
public class IlluminaFileReader implements Iterator<Object>, Closeable {

    protected final String fileName;
    protected DataInputStream inputStream;

    /**
     *
     * @param fileName bcl, clocs and filter etc illumina file name
     * @throws Exception
     */
    public IlluminaFileReader(String fileName) throws Exception {

        this.fileName = fileName;
        this.openInputFile(fileName);
    }

    /**
     * check file, open it if it is valid
     * @param fileName
     * @throws Exception
     */
    private void openInputFile(String fileName) throws Exception {

        if (fileName == null) {
            throw new IllegalArgumentException("File name must be given.");
        } else {
            File file = new File(fileName);
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist: " + fileName);
            } else if (file.isDirectory()) {
                throw new IllegalArgumentException("File name is a directory: " + fileName);
            } else if (!file.canRead()) {
                throw new FileNotFoundException("File cannot be read: " + fileName);
            } else {
                this.inputStream = new DataInputStream(new FileInputStream(file));
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
                Logger.getLogger(IlluminaFileReader.class.getName()).log(Level.SEVERE, "Cannot close file", ex);
            }
        }
    }

    /**
     * read four bytes from input stream and convert to unsigned integer
     * @return an unsigned integer
     * @throws IOException
     */
    public int readFourBytes() throws IOException {
        return this.readFourBytes(this.inputStream);
    }

    /**
     * read four bytes from input stream and convert to unsigned integer
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
}
