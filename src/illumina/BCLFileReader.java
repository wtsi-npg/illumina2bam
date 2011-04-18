/*
 * This class is a reader of a bcl file
 */
package illumina;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Guoying Qi
 */
public class BCLFileReader extends IlluminaFileReader {

    private final char[] BASE_ARRAY = {'A', 'C', 'G', 'T'};
    private final char UNKNOWN_BASE = '.';
    private final int UNKNOWN_BASE_QUALITY = 66;
    private int currentCluster = 0;
    private int totalClusters = 0;

    /**
     * constructor to generate bcl file input stream
     *  and read the number of clusters
     *
     * @param bclFileName bcl file name
     * @throws Exception
     */
    public BCLFileReader(String bclFileName) throws Exception {

        super(bclFileName);
        this.readFileHeader();
    }

    /**
     * read total number of clusters from header
     * @throws IOException
     */
    private void readFileHeader() throws IOException {

        //first four bytes - unsigned 32bits little endian integer
        this.totalClusters = this.readFourBytes();
        Logger.getLogger(BCLFileReader.class.getName()).log(Level.INFO, "The total number of clusters: {0} in file {1}", new Object[]{this.getTotalClusters(), this.fileName});
    }

    /**
     * check any more clusters in the file stream
     * @return
     */
    @Override
    public boolean hasNext() {

        return (this.getCurrentCluster() < this.getTotalClusters()) ? true : false;
    }

    /**
     * get base and quality for next cluster
     * @return char[], base for the first element and quality as the second element
     */
    @Override
    public char[] next() {

        try {
            int nextBase = this.inputStream.read();

            //end of the file
            if (nextBase == -1) {
                Logger.getLogger(BCLFileReader.class.getName()).log(Level.SEVERE, "There is no mroe cluster in BCL file after cluster {1}: {0} ", new Object[]{this.fileName, this.getCurrentCluster()});
                return null;
            }

            char base, qul;
            char[] currentClusterPair = new char[2];

            //last two bits are base
            int baseInt = nextBase & 0x3;
            //the rest base are quality
            int qulInt = (nextBase & 0xFC) >> 2;

            //convert base to char or unknow
            base = (qulInt != 0) ? this.BASE_ARRAY[baseInt] : this.UNKNOWN_BASE;

            //convert quality score
            qulInt = (qulInt != 0) ? (qulInt + 64) : UNKNOWN_BASE_QUALITY;
            qul = (char) qulInt;

            currentClusterPair[0] = base;
            currentClusterPair[1] = qul;

            this.currentCluster++;
            return currentClusterPair;

        } catch (IOException ex) {
            Logger.getLogger(BCLFileReader.class.getName()).log(Level.SEVERE, "There is problems to read the file: {0}", ex);
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

    public static void main(String args[]) throws Exception {

        String bclFileName = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/s_1_1101.bcl";
        if(args.length > 0  && args[0] != null){
            bclFileName = args[0];
        }
        BCLFileReader bcl = new BCLFileReader(bclFileName);

        System.out.println(bcl.getTotalClusters());

        int count = 0;
        while (bcl.hasNext()) {
            count++;
            char[] cluster = bcl.next();
            if (count % 100000 == 1 || count == 2609912) {
                System.out.println(count + "-" + cluster[0] + "-" + cluster[1]);
            }
        }

        System.out.println(bcl.getCurrentCluster());

        char[] cluster = bcl.next();
        if (cluster == null) {
            System.out.println("no more cluster");
        }

    }
}
