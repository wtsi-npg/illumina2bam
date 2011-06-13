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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import net.sf.picard.util.Log;

/**
 *
 * @author Guoying Qi
 */
public class PosFileReader implements Closeable {

    private final Log log = Log.getInstance(IlluminaFileReader.class);
    private final String fileName;
    private BufferedReader fileReader;
    protected int currentTotalClusters;
    
    /**
     * 
     * @param fileName
     * @throws FileNotFoundException 
     */
    public PosFileReader(String fileName) throws FileNotFoundException{
        this.fileName = fileName;
        this.fileReader = new BufferedReader(new FileReader(this.fileName));
    }

    /**
     * 
     * @return cluster position coordinates as an array
     */
    public String [] next() {
        String [] pos = new String [2];
        try {
            String nextLine = this.fileReader.readLine();
            if(nextLine == null){
                this.log.error("There is no more cluster in this pos file");
                return null;
            }
            String [] coordinates = nextLine.split(" ");
            if(coordinates.length != 2){
                throw new RuntimeException("A line in pos file wrong format: " + nextLine);
            }
            for (int i= 0; i<2; i++){
                double tempCoor = Math.round( Double.parseDouble(coordinates[i]) * 10.0 );
                pos [i] = Integer.toString ( (int)tempCoor + 1000 );
            }
            this.currentTotalClusters++;
        } catch (IOException ex) {
            log.error(ex, "Problem to read pos file: " + ex);
        }
        return pos;
    }


    @Override
    public void close() throws IOException {
        this.fileReader.close();
    }

    /**
     * @return the currentTotalClusters
     */
    public int getCurrentTotalClusters() {
        return currentTotalClusters;
    }
    
    public static void main (String [] args){
        PosFileReader posFileReader = null;
        try {
            posFileReader = new PosFileReader("testdata/110519_IL33_06284/Data/Intensities/s_8_0112_pos.txt");
        } catch (FileNotFoundException ex) {
           System.err.println(ex);
        }
        for(int i =0; i< 353693; i++){
            String [] pos = posFileReader.next();
            //System.out.println(pos[0] + " " + pos[1]);
        }
    }
    
}
