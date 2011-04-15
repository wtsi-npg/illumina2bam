/*
 * this class is a reader of a filter file
 */
package illumina;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Guoying Qi
 */
public class FilterFileReader extends IlluminaFileReader {

    private final int EXPECTED_FILTER_VERSION = 3;
    private int currentCluster = 0;
    private int totalClusters = 0;
    private int currentPFClusters = 0;

    /**
     *
     * @param filterFileName filter file name
     * @throws Exception
     */
    public FilterFileReader(String filterFileName) throws Exception {

        super(filterFileName);
        this.readFileHeader();
    }

    /**
     *
     * @throws IOException
     */
    private void readFileHeader() throws Exception {

        //fisrt four bytes are empty and should be zero, backward compatibility
        int emptyBytes = this.readFourBytes(inputStream);
        if (emptyBytes != 0) {
            Logger.getLogger(FilterFileReader.class.getName()).log(Level.SEVERE, "The first four bytes are not zero: {0}", emptyBytes);
            throw new Exception("The first four bytes in filter file are not empty");
        }

        //next four bytes should be version and greater or equal to the expected
        int version = this.readFourBytes(inputStream);
        if (version != this.EXPECTED_FILTER_VERSION) {
            Logger.getLogger(FilterFileReader.class.getName()).log(Level.SEVERE, "Unexpected version byte {0}", version);
            throw new Exception("Unexpected version number in filter file");
        }

        //next four bytes should be the total number of clusters
        this.totalClusters = this.readFourBytes(inputStream);
        Logger.getLogger(FilterFileReader.class.getName()).log(Level.INFO, "The total number of clusters: {0}", this.getTotalClusters());
    }

    @Override
    public boolean hasNext() {

        return (this.getCurrentCluster() < this.getTotalClusters()) ? true : false;
    }

    @Override
    public Object next() {

        try {
            int nextByte = this.inputStream.read();

            if (nextByte == -1) {
                Logger.getLogger(FilterFileReader.class.getName()).log(Level.SEVERE, "There is no mroe cluster in BCL file after cluster {1}: {0} ", new Object[]{this.fileName, this.getCurrentCluster()});
                return null;
            }

            this.currentCluster++;
            nextByte = nextByte & 0x1;
            if (nextByte == 1) {
                this.currentPFClusters++;
            }

            return new Integer(nextByte);

        } catch (IOException ex) {
            Logger.getLogger(FilterFileReader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * @return the currentCluster
     */
    public int getCurrentCluster() {
        return currentCluster;
    }

    /**
     * @return the totalClusters
     */
    public int getTotalClusters() {
        return totalClusters;
    }

    /**
     * @return the currentPFClusters
     */
    public int getCurrentPFClusters() {
        return currentPFClusters;
    }

    public static void main(String args[]) throws Exception {

        FilterFileReader filter = new FilterFileReader("/nfs/users/nfs_g/gq1/illumina2bam/testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/s_1_1101.filter");

        int numberPFCluster = 0;
        while (filter.hasNext()) {
            int nextCluster = (Integer) filter.next();

            if (nextCluster == 1) {
                numberPFCluster++;
            }
        }
        System.out.println(numberPFCluster);
        System.out.println(filter.getCurrentCluster());
        System.out.println(filter.getCurrentPFClusters());

        filter.next();
    }
}
