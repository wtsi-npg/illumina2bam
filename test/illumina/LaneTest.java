/*
 * this is the test class for Lane
 *
 */
package illumina;

import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gq1
 */
public class LaneTest {

    private static String intensityDir = "testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities";
    private static int laneNumber = 1;
    
    private static String id = "HS13_6000";
    private static int[] cycleRangeRead1 = {1, 2};
    private static int[] cycleRangeRead2 = {51, 52};
    private static int[] cycleRangeIndex = {50, 50};
    private static int[] tileList = {1001};
    private static Lane lane;

    public LaneTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {

        HashMap<String, int[]> cycleRangeByRead = new HashMap<String, int[]>(3);
        cycleRangeByRead.put("read1", cycleRangeRead1);
        cycleRangeByRead.put("read2", cycleRangeRead2);
        cycleRangeByRead.put("readIndex", cycleRangeIndex);

        lane = new Lane(intensityDir, laneNumber, true, true);
        lane.setCycleRangeByRead(cycleRangeByRead);
        lane.setId(id);
        lane.setTileList(tileList);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        lane = null;
    }

    @Test
    public void processTilesOK(){
        assertNotNull(lane);
    }
}
