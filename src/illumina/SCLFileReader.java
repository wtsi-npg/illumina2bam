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

import java.io.IOException;
import net.sf.picard.util.Log;

/**
 * This class is a reader of a scl file
 * 
 * @author Guoying Qi
 */
public class SCLFileReader extends IlluminaFileReader {
    
    private final Log log = Log.getInstance(SCLFileReader.class);
    
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
        //TODO: stop read all bases to memory if using too much memory
        this.readBases();

    }

    /**
     * read file header to get total cluster number of this file
     * @throws IOException
     */
    private void readFileHeader() throws IOException {

        //first four bytes - unsigned 32bits little endian integer
        this.totalClusters = this.readFourBytes(inputStream);
        log.debug("The total number of clusters: " + this.getTotalClusters() + " in " + this.fileName);
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
            log.error("The file does not have the required number of clusters: " + this.getTotalClusters());
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
     * @return true if there is next cluster
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
            log.error("The required cluster out of range: " + this.getCurrentCluster());
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

        String sclFileName = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/C1.1/s_1_1101.scl";
        if(args.length > 0  && args[0] != null){
            sclFileName = args[0];
        }

        SCLFileReader scl = new SCLFileReader(sclFileName);

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
