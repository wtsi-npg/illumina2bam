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
 * This class is a reader of a bcl file
 * 
 */
package illumina;

import java.io.EOFException;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Guoying Qi
 */
public class BCLFileReader extends IlluminaFileReader {

    private final byte[] BASE_ARRAY = {65, 67, 71, 84}; //A C G T
    private final byte UNKNOWN_BASE = 78;
    private int currentCluster = 0;
    private int totalClusters = 0;

    //define current illumina quality score range
    private final byte MAX_QUALITY_SCORE = 60;
    private final byte MIN_QUALITY_SCORE = 0;

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
     *  get base and quality for next cluster
     * @return byte [] base byte for the first element and quality byte as the second element
     *
     */
    @Override
    public byte[] next() {

        try {

            byte nextBase;
            try{
                nextBase = this.inputStream.readByte();
            } catch( EOFException ex) {
                //end of the file
                Logger.getLogger(BCLFileReader.class.getName()).log(Level.SEVERE,
                        "There is no mroe cluster in BCL file after cluster {1}: {0}\n{2} ",
                        new Object[]{this.fileName, this.getCurrentCluster(), ex}
                );
                return null;
            }

            //last two bits are base index
            int baseIndex =  nextBase & 0x3;

            //the rest base are quality
            byte qul = ( byte) ( (nextBase & 0xFC) >> 2 ) ;
            if(qul < this.MIN_QUALITY_SCORE || qul >this.MAX_QUALITY_SCORE ){
               throw new IllegalArgumentException("Invalid quality score: "
                       + qul + " in bcl file " + this.fileName
                       + " in position " + this.getCurrentCluster());
            }

            //convert base to char or unknow
            byte base = (qul != 0) ? this.BASE_ARRAY[baseIndex] : this.UNKNOWN_BASE;

            byte [] currentClusterPair = new byte[2];
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
            byte [] cluster = bcl.next();
            if (count % 100000 == 1 || count == 2609912) {
                System.out.println(count + "-" + (char)cluster[0] + "-" + (char)(cluster[1] + 64) );//base and qseq quality score
            }
        }

        System.out.println(bcl.getCurrentCluster());

        byte [] cluster = bcl.next();
        if (cluster == null) {
            System.out.println("no more cluster");
        }

    }
}
