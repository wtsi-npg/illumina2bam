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

import java.io.IOException;
import net.sf.picard.util.Log;


/**
 * This class is a reader of a clocs file
 * @author Guoying Qi
 */
public class CLocsFileReader extends IlluminaFileReader {
    
    private final Log log = Log.getInstance(CLocsFileReader.class);

    private final int EXPECTED_CLOCS_VERSION = 1;
    private final int BLOCK_SIZE = 25;
    private final int IMAGE_WIDTH = 2048;
    private final int BLOCKS_PER_LINE = (IMAGE_WIDTH + BLOCK_SIZE - 1) / BLOCK_SIZE;
    private int totalBlocks;
    private int currentBlock = 0;
    private int currentBlockUnreadClusters;
    private int currentTotalClusters = 0;

    /**
     * Constructor
     *
     * @param cLocsFileName clocs file name
     */
    public CLocsFileReader(String cLocsFileName) throws Exception {

        super(cLocsFileName);


        this.readFileHeader();


    }

    /**
     * read cLocs file header
     * @throws Exception
     */
    private void readFileHeader() throws Exception {

        //check file version number from first byte
        int clocsVersion = this.inputStream.readUnsignedByte();
        if (this.EXPECTED_CLOCS_VERSION != clocsVersion) {
            log.error("Unexpected version byte" + clocsVersion);
            throw new Exception("Unexpected version: " + clocsVersion);
        }

        //read blocksCount from next four bytes
        totalBlocks = this.readFourBytes(inputStream);
        log.info("Total blocks count " + this.getTotalBlocks());

        //read first block from next byte
        this.currentBlockUnreadClusters = inputStream.readUnsignedByte();
        this.currentBlock++;
    }

    /**
     * this method should mean has Next Block
     * Maybe no any cluster available anymore and return null when next method called
     * in
     * 
     * @return true if there is next cluster
     * but next method will return null and this method will return true as well when in last block but no cluster there
     */
    @Override
    public boolean hasNext() {

        return (this.getCurrentBlock() < this.getTotalBlocks()
                || ( this.getCurrentBlock() == this.getTotalBlocks() && this.currentBlockUnreadClusters > 0)
                ) ? true : false;
    }

    /**
     *
     * @return [x, y]
     */
    @Override
    public String[] next() {

        if (!this.hasNext()) {
           throw new RuntimeException("Try to read a block "
                    + getCurrentBlock()
                    + " out of range:"
                    + this.totalBlocks
                    + ". Current number of Clusters:"
                    + this.getCurrentTotalClusters()
                    );
        }

        try {
            while (this.currentBlockUnreadClusters-- == 0 && this.getCurrentBlock() < this.getTotalBlocks()) {

                this.currentBlockUnreadClusters = inputStream.readUnsignedByte();
                ++currentBlock;
            }

            if (this.currentBlockUnreadClusters < 0) {
                log.warn("There is no mroe block in " + this.getFileName() + ". Current block: " + this.getCurrentBlock());
                return null;
            }

            int dx = inputStream.readUnsignedByte();
            int dy = inputStream.readUnsignedByte();

            int x = 10 * BLOCK_SIZE * ((getCurrentBlock() - 1) % BLOCKS_PER_LINE) + dx + 1000;
            int y = 10 * BLOCK_SIZE * ((getCurrentBlock() - 1) / BLOCKS_PER_LINE) + dy + 1000;

            String[] pos = new String[2];
            
            pos[0] = Integer.toString(x);
            pos[1] = Integer.toString(y);

            this.currentTotalClusters++;

            return pos;

        } catch (IOException ex) {
            log.error(ex, "Problem to read clock file");
        }

        return null;
    }

    /**
     * @return the totalBlocks
     */
    public int getTotalBlocks() {
        return totalBlocks;
    }

    /**
     * @return the currentBlock
     */
    public int getCurrentBlock() {
        return currentBlock;
    }

    /**
     * @return the currentTotalClusters
     */
    public int getCurrentTotalClusters() {
        return currentTotalClusters;
    }

    public static void main(String[] args) throws Exception {

        String clocsFileName = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/L001/s_1_1101.clocs";
        if(args.length > 0  && args[0] != null){
            clocsFileName = args[0];
        }

        CLocsFileReader fr = new CLocsFileReader(clocsFileName);
        int count = 0;
        while (fr.hasNext()) {
            String[] pos = fr.next();
            count++;
            if (count % 100000 == 1 && pos != null ) {
                System.out.println(pos[0] + " " + pos[1]);
            }
        }
        System.err.println(fr.getCurrentTotalClusters());
        
        try{
           String[] next = fr.next();
        } catch (Exception ex){
           System.err.println(ex.getMessage());
        }

    }
}
