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

/**
 * This class try to open a Illumina position file including clocs, locs or pos file into a data input stream
 * 
 * @author Guoying Qi
 */
public abstract class PositionFileReader extends IlluminaFileReader {
    
    protected int currentTotalClusters;

    /**
     * 
     * @param fileName
     * @throws FileNotFoundException 
     */
    public PositionFileReader(String fileName) throws FileNotFoundException {

        super(fileName);
    }
    
    @Override
    public abstract Position next(); 
    
    /**
     * @return the currentTotalClusters
     */
    public int getCurrentTotalClusters() {
        return currentTotalClusters;
    }
    
    public static class Position {
        public String x;
        public String y;
        
        public Position(String x, String y){
            this.x = x;
            this.y = y;
        }
        public String [] toArray() {
            String [] array = {x, y};
            return array;
        }
    }
}
