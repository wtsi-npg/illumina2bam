/*
 * This class is a reader of a scl file
 */
package illumina;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Guoying Qi
 */
public class SCLFileReader extends IlluminaFileReader {

    private final char[] BASE_ARRAY = {'A', 'C', 'G', 'T'};
    private char[] bases;
    private int currentCluster = 0;
    private int totalClusters = 0;

    /**
     * constructor to generate scl file input stream,
     * and read the number of clusters
     * and read all bases to an array
     *
     * @param sclFileName scl file name
     * @throws Exception
     */
    public SCLFileReader(String sclFileName) throws Exception {

        super(sclFileName);

        this.readFileHeader();
        this.readBases();

    }

    /**
     * read file header to get total cluster number of this file
     * @throws IOException
     */
    private void readFileHeader() throws IOException {

        //first four bytes - unsigned 32bits little endian integer
        this.totalClusters = this.readFourBytes(inputStream);
        Logger.getLogger(SCLFileReader.class.getName()).log(Level.INFO, "The total number of clusters: {0} in file {1}", new Object[]{this.getTotalClusters(), this.fileName});
    }

    /**
     * read all bases into an array
     * @throws IOException
     */
    private void readBases() throws Exception {

        this.bases = new char[this.getTotalClusters()];

        int numberBaseBytes = (int) Math.ceil(this.getTotalClusters() / 4.0);
        byte[] baseBytes = new byte[numberBaseBytes];

        int read = this.inputStream.read(baseBytes);

        if (read == -1) {
            Logger.getLogger(SCLFileReader.class.getName()).log(Level.SEVERE, "The file does not have the required number of clusters: {0}", this.getTotalClusters());
            throw new Exception("The file does not have the required number of clusters");
        }

        this.close();
        this.inputStream = null;

        for (int b = 0; b < this.getTotalClusters(); b++) {
            int i = b / 4;
            int j = b % 4;
            switch (j) {
                case 0:
                    bases[b] = BASE_ARRAY[(baseBytes[i] >> 6) & 3];
                    break;
                case 1:
                    bases[b] = BASE_ARRAY[(baseBytes[i] >> 4) & 3];
                    break;
                case 2:
                    bases[b] = BASE_ARRAY[(baseBytes[i] >> 2) & 3];
                    break;
                case 3:
                    bases[b] = BASE_ARRAY[(baseBytes[i]) & 3];
                    break;
            }

        }
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
     *
     * @return next second base call
     */
    @Override
    public Character next() {

        if (!this.hasNext()) {
            Logger.getLogger(SCLFileReader.class.getName()).log(Level.SEVERE, "The required cluster out of range: {0}", this.getCurrentCluster());
            return null;
        }

        return this.bases[currentCluster++];
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

        SCLFileReader scl = new SCLFileReader("/nfs/users/nfs_g/gq1/illumina2bam/testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/s_1_1101.scl");

        System.out.println(scl.getTotalClusters());

        char[] bases = scl.bases;
        int count = 0;
        for (char b : bases) {
            count++;
            if ((count % 1000 == 1) || (count == 2609912)) {
                System.out.println(b);
            }
        }
        System.out.println(count);

        System.out.println("------------");

        count = 0;
        while (scl.hasNext()) {
            count++;
            char b = scl.next();
            if ((count % 10000 == 1) || (count == 2609912)) {
                System.out.println(b);
            }
        }
        System.out.println(count);

        scl.next();

    }
}
