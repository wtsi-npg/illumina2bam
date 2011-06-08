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
 *
 * This class is a reader of a filter file
 * 
 * @author Guoying Qi
 */
public class FilterFileReader extends IlluminaFileReader {
    
    private final Log log = Log.getInstance(FilterFileReader.class);
    
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
            log.error("The first four bytes are not zero: " + emptyBytes);
            throw new Exception("The first four bytes in filter file are not empty");
        }

        //next four bytes should be version and greater or equal to the expected
        int version = this.readFourBytes(inputStream);
        if (version != this.EXPECTED_FILTER_VERSION) {
            log.error("Unexpected version byte: " + version);
            throw new Exception("Unexpected version number in filter file");
        }

        //next four bytes should be the total number of clusters
        this.totalClusters = this.readFourBytes(inputStream);
        log.info("The total number of clusters: " + this.getTotalClusters());
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
                log.warn("There is no mroe cluster in Filter file after cluster " + this.getCurrentCluster() + " in file " + this.fileName);
                return null;
            }

            this.currentCluster++;
            nextByte = nextByte & 0x1;
            if (nextByte == 1) {
                this.currentPFClusters++;
            }

            return new Integer(nextByte);

        } catch (IOException ex) {
            log.error(ex, "Problems to read filter file");
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

        String filterFileName = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls/L001/s_1_1101.filter";
        if(args.length > 0  && args[0] != null){
            filterFileName = args[0];
        }

        FilterFileReader filter = new FilterFileReader(filterFileName);

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
