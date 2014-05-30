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
import net.sf.picard.util.Log;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class PosFileReader extends PositionFileReader  {

    private final Log log = Log.getInstance(IlluminaFileReader.class);
    private BufferedReader fileReader;
    private int totalCluster;
    
    /**
     * 
     * @param fileName
     * @throws FileNotFoundException 
     */
    public PosFileReader(String fileName) throws FileNotFoundException, IOException {
        
        super(fileName);
        
        this.fileReader = new BufferedReader(new FileReader(this.fileName));
        
        this.countTotalClusters();
    }


    private void countTotalClusters(){
        try {
            LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(this.fileName));

            lineNumberReader.skip(Long.MAX_VALUE);
            this.totalCluster = lineNumberReader.getLineNumber();
            lineNumberReader.close();
            
        } catch (FileNotFoundException ex) {
            log.error(ex, "Pos file not exist");
        } catch (IOException ex) {
            log.error(ex, "problems to read pos file");
        }
    }

    /**
     * 
     * @return cluster position coordinates as an array
     */
    @Override
    public PositionFileReader.Position next() {
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
        return new PositionFileReader.Position(pos[0], pos[1]);
    }

    @Override
    public boolean hasNext() {
          return (this.currentTotalClusters < this.totalCluster) ? true : false;
    }

    @Override
    public void close() {
        super.close();
        try {
            this.fileReader.close();
        } catch (IOException ex) {
            log.error(ex, "problems to close pos file");
        }
    }
    
    /**
     * @return the totalCluster
     */
    public int getTotalCluster() {
        return totalCluster;
    }
    
    /**
     * 
     * @param args
     */
    public static void main (String [] args){
        PosFileReader posFileReader = null;
        try {
            posFileReader = new PosFileReader("testdata/110519_IL33_06284/Data/Intensities/s_8_0112_pos.txt");
        } catch (FileNotFoundException ex) {
           System.err.println(ex);
        } catch (IOException ex) {
           System.err.println(ex);
        } 
        for(int i =0; i< 353693; i++){
             PositionFileReader.Position pos = posFileReader.next();
             System.out.println(pos.x + " " + pos.y);
        }
    } 
}