/*
 * This class is a reader of a clocs file
 */
package illumina;

import java.io.IOException;

import java.text.DecimalFormat;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Guoying Qi
 */
public class CLocsFileReader extends IlluminaFileReader {

    private final int EXPECTED_CLOCS_VERSION = 1;
    private final int BLOCK_SIZE = 25;
    private final int IMAGE_WIDTH = 2048;
    private final int BLOCKS_PER_LINE = (IMAGE_WIDTH + BLOCK_SIZE - 1) / BLOCK_SIZE;
    private final DecimalFormat myFormatter = new DecimalFormat("#0");
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
            Logger.getLogger(CLocsFileReader.class.getName()).log(Level.SEVERE, "Unexpected version byte {0}", clocsVersion);
            throw new Exception("Unexpected version: " + clocsVersion);
        }

        //read blocksCount from next four bytes
        totalBlocks = this.readFourBytes(inputStream);
        Logger.getLogger(CLocsFileReader.class.getName()).log(Level.INFO, "Total blocks count: {0}", this.getTotalBlocks());

        //read first block from next byte
        this.currentBlockUnreadClusters = inputStream.readUnsignedByte();
        this.currentBlock++;
    }

    /**
     * this method should mean has Next Block
     * Maybe no any cluster available anymore and return null when next method called
     * in
     * @return
     */
    @Override
    public boolean hasNext() {

        return (this.getCurrentBlock() < this.getTotalBlocks()) ? true : false;
    }

    /**
     *
     * @return [x, y]
     */
    @Override
    public String[] next() {

        if (this.getCurrentBlock() >= this.getTotalBlocks()) {
            Logger.getLogger(CLocsFileReader.class.getName()).log(Level.SEVERE, "Try to read a block out of range: {0}", getCurrentBlock());
            return null;
        }

        try {
            while (this.currentBlockUnreadClusters-- == 0 && this.getCurrentBlock() < this.getTotalBlocks()) {

                this.currentBlockUnreadClusters = inputStream.readUnsignedByte();
                ++currentBlock;
            }

            if (this.currentBlockUnreadClusters < 0) {
                Logger.getLogger(CLocsFileReader.class.getName()).log(Level.INFO, "There is no mroe block in the file {1}: {0} ", new Object[]{this.fileName, this.getCurrentBlock()});
                return null;
            }

            int dx = inputStream.readUnsignedByte();
            int dy = inputStream.readUnsignedByte();

            double x = BLOCK_SIZE * ((getCurrentBlock() - 1) % BLOCKS_PER_LINE) + dx / 10.0;
            double y = BLOCK_SIZE * ((getCurrentBlock() - 1) / BLOCKS_PER_LINE) + dy / 10.0;

            String[] pos = new String[2];
            //pos[0] = this.myFormatter.format(x);
            //pos[1] = this.myFormatter.format(y);
            pos[0] = this.myFormatter.format(1000 + x*10);
            pos[1] = this.myFormatter.format(1000 + y*10);
            this.currentTotalClusters++;

            return pos;

        } catch (IOException ex) {
            Logger.getLogger(CLocsFileReader.class.getName()).log(Level.SEVERE, null, ex);
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
            if (count % 100000 == 1) {
                System.out.println(pos[0] + " " + pos[1]);
            }
        }
        System.err.println(fr.getCurrentTotalClusters());

        String[] next = fr.next();

    }
}
