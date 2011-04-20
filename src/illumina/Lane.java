/*
 * process a laneNumber
 */

package illumina;

import java.util.HashMap;
import net.sf.samtools.SAMFileWriter;

/**
 *
 * @author Guoying Qi
 */
public class Lane {

    //fields must be given
    private final String intensityDir;
    private final int laneNumber;

    private final boolean includeSecondCall;
    private final boolean pfFilter;


    //get from config file
    private String id;
    private HashMap<String, int[]> cycleRangeByRead;
    private int [] tileList;    

    public Lane(String intensityDir,
                int laneNumber,
                boolean secondCall,
                boolean pfFilter){

        this.intensityDir      = intensityDir;
        this.laneNumber        = laneNumber;
        this.includeSecondCall = secondCall;
        this.pfFilter          = pfFilter;
    }

    public void processTiles(SAMFileWriter outputSam) throws Exception{
        for(int tileNumber : this.tileList){
            Tile tile = new Tile(intensityDir, id, laneNumber, tileNumber,
                                 cycleRangeByRead,
                                 this.includeSecondCall, this.pfFilter);
            tile.openBaseCallFiles();
            tile.processTile(outputSam);
            tile.closeBaseCallFiles();
        }
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param cycleRangeByRead the cycleRangeByRead to set
     */
    public void setCycleRangeByRead(HashMap<String, int[]> cycleRangeByRead) {
        this.cycleRangeByRead = cycleRangeByRead;
    }

    /**
     * @param tileList the tileList to set
     */
    public void setTileList(int[] tileList) {
        this.tileList = tileList;
    }

}
