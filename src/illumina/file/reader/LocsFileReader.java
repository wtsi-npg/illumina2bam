
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
package illumina.file.reader;

import java.io.FileNotFoundException;
import java.io.IOException;
import net.sf.picard.util.Log;


/**
 * This class is a reader of a locs file
 * @author Guoying Qi
 */

public class LocsFileReader extends PositionFileReader {
    
    private final Log log = Log.getInstance(LocsFileReader.class);

    private int totalCluster;

    /**
     * Constructor
     *
     * @param locsFileName locs file name
     */
    public LocsFileReader(String locsFileName) throws FileNotFoundException, IOException {

        super(locsFileName);

        this.readFileHeader();
    }

    /**
     * read locs file header
     * @throws Exception
     */
    private void readFileHeader() throws IOException {
       // first 8 bytes are unused
       this.inputStream.skipBytes(8);
       // 4 bytes little endian
       this.totalCluster = this.readFourBytes(inputStream);
    }

    /**
     * 
     * @return true if there is next cluster
     */
    @Override
    public boolean hasNext() {

        return (this.currentTotalClusters < this.totalCluster) ? true : false;
    }

    /**
     * 
     * @return [x, y] as a string array
     */

    @Override
    public PositionFileReader.Position next() {

        if (!this.hasNext()) {
           throw new RuntimeException("No more cluster available."
                   + " Current number of clusters read: " + this.currentTotalClusters
                   + ". Total number of clusters: " + this.totalCluster);
        }

        try {
            float xFloat = Float.intBitsToFloat(this.readFourBytes());
            float yFloat = Float.intBitsToFloat(this.readFourBytes());
            
            int x = Math.round( 10 * xFloat + 1000 ) ;
            int y = Math.round( 10 * yFloat + 1000 ) ; 

            String[] pos = new String[2];
            
            pos[0] = Integer.toString(x);
            pos[1] = Integer.toString(y);

            this.currentTotalClusters++;

            return new PositionFileReader.Position(pos[0], pos[1]);

        } catch (IOException ex) {
            log.error(ex, "Problem to read locs file");
        }

        return null;
    }

    /**
     * 
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {

        String locsFileName = "testdata/111014_M00119_0028_AMS0001310-00300/Data/Intensities/L001/s_1_1.locs";
        if(args.length > 0  && args[0] != null){
            locsFileName = args[0];
        }

        LocsFileReader fr = new LocsFileReader(locsFileName);
        int count = 0;
        while (fr.hasNext()) {
            String[] pos = fr.next().toArray();
            count++;
            if (count % 10000 == 1 && pos != null ) {
                System.out.println(pos[0] + " " + pos[1]);
            }
        }
        System.err.println(fr.totalCluster);
        System.err.println(fr.currentTotalClusters);
        
        try{
           fr.next();
        } catch (Exception ex){
           System.err.println(ex.getMessage());
        }

    }

    /**
     * @return the totalCluster
     */
    public int getTotalCluster() {
        return totalCluster;
    }

}
