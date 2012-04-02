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

import java.util.ArrayList;
import java.util.List;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author gq1
 */
public class AlignmentFilterMetricTest {
    
    public AlignmentFilterMetricTest() {
    }

    /**
     * Test of checkNextReadsForChimera method, of class AlignmentFilterMetric.
     */
    @Test
    public void testCheckNextReadsForChimera() {
        System.out.println("checkNextReadsForChimera");
        
        SAMFileHeader header = new SAMFileHeader();
        SAMRecord record1 = new SAMRecord(header);
        record1.setReadUnmappedFlag(true);
        
        SAMRecord record2 = new SAMRecord(header);
        record2.setReadUnmappedFlag(false);
        
        List<SAMRecord> recordList = new ArrayList<SAMRecord>();
        List<SAMRecord> pairedRecordList = new ArrayList<SAMRecord>();
        
        recordList.add(record1);
        recordList.add(record2);
        
        pairedRecordList.add(record2);
        pairedRecordList.add(record1);
        
        int[][] chiremeraReadsCount = new int[2][2];
        
        AlignmentFilterMetric instance = new AlignmentFilterMetric(2);
        instance.setChimericReadsCount(chiremeraReadsCount);
        
        instance.checkNextReadsForChimera(recordList, pairedRecordList);
        
        assertEquals(chiremeraReadsCount[1][0], 1);
        
        int [] expectForward = {0, 1, 0};        
        assertArrayEquals( instance.getReadsCountByAlignedNumForward(),expectForward ) ;      
        assertArrayEquals( instance.getReadsCountByAlignedNumReverse(),expectForward ) ;
    }
}
