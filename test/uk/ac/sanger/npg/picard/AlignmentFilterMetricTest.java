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

package uk.ac.sanger.npg.picard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import static org.junit.Assert.*;
import org.junit.Test;
import uk.ac.sanger.npg.bam.util.CheckMd5;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class AlignmentFilterMetricTest {
    
    public AlignmentFilterMetricTest() { }

    /**
     * Test of checkNextReadsForChimera method, of class AlignmentFilterMetric.
     */
    @Test
    public void testCheckNextReadsForChimera() throws IOException {
        System.out.println("checkNextReadsForChimera");
        
        SAMFileHeader header = new SAMFileHeader();
        SAMRecord sam_unmapped = new SAMRecord(header);
        sam_unmapped.setReadUnmappedFlag(true);
        
        SAMRecord sam_mapped = new SAMRecord(header);
        sam_mapped.setReadUnmappedFlag(false);

        SAMRecord paired_unmapped = new SAMRecord(header);
        paired_unmapped.setReadUnmappedFlag(true);
        paired_unmapped.setReadPairedFlag(true);
        paired_unmapped.setSecondOfPairFlag(true);
        
        SAMRecord paired_mapped = new SAMRecord(header);
        paired_mapped.setReadUnmappedFlag(false);
        paired_mapped.setReadPairedFlag(true);
        paired_mapped.setSecondOfPairFlag(true);

        ArrayList<SAMRecord> recordSet = new ArrayList<SAMRecord>();
        ArrayList<SAMRecord> pairedRecordSet = new ArrayList<SAMRecord>();
        ArrayList<ArrayList<SAMRecord>> recordList = new ArrayList<ArrayList<SAMRecord>>();

        recordSet.add(sam_unmapped); 
        recordSet.add(paired_mapped); 
        pairedRecordSet.add(sam_mapped); 
        pairedRecordSet.add(paired_unmapped); 
        
        recordList.add(recordSet);
        recordList.add(pairedRecordSet);

        AlignmentFilterMetric instance = new AlignmentFilterMetric(2);      
        instance.checkNextReadsForChimera(recordList);
        
        int[][] chiremeraReadsCount = instance.getChimericReadsCount();
        assertEquals(chiremeraReadsCount[1][0], 1);
        
        int [] expect = {0, 1, 0};        
        assertArrayEquals( instance.getReadsCountByAlignedNumForward(),expect );      
        assertArrayEquals( instance.getReadsCountByAlignedNumReverse(),expect );

        File metricsJson = File.createTempFile("alignmentFilterMetrics", ".json", new File("testdata"));
        System.out.println("Output metrics: " + metricsJson.getPath());
        instance.output(metricsJson);
        assertEquals(CheckMd5.getFileMd5(metricsJson), "c0c744ac2e560ec515c33222fbe7603d");
        metricsJson.deleteOnExit();
    }
    
    /**
     * Test of checkNextReadsForChimera method, of class AlignmentFilterMetric.
     */
    @Test
    public void testSetsMethods() throws IOException {    
     
        AlignmentFilterMetric instance = new AlignmentFilterMetric(2);
        
        instance.setProgramName("AlignmentFilter");
        instance.setProgramVersion("1.00");
        instance.setProgramCommand("test command");
        
        instance.setTotalReads(100);
        
        int [] readsCountPerRef = {10, 50};        
        instance.setReadsCountPerRef(readsCountPerRef);
        
        instance.setReadsCountUnaligned(40);
        
        ArrayList<AlignmentFilterMetric.SQ> ref1 = new ArrayList<AlignmentFilterMetric.SQ>();
        ref1.add(new AlignmentFilterMetric.SQ("seq1_1", "as1_1", "sp1_1", 400, "ur1_1")); 
        instance.addRef(ref1);
        
        ArrayList<AlignmentFilterMetric.SQ> ref2 = new ArrayList<AlignmentFilterMetric.SQ>();
        ref2.add(new AlignmentFilterMetric.SQ("seq2_1", "as2_1", "sp2_1", 500, "ur2_1"));
        ref2.add(new AlignmentFilterMetric.SQ("seq2_2", "as2_2", null, 600, null));
        instance.addRef(ref2);
        
        SAMSequenceDictionary sequenceDictionary = new SAMSequenceDictionary();
        SAMSequenceRecord ref3_1 = new SAMSequenceRecord("seq3_1", 700);
        sequenceDictionary.addSequence(ref3_1);
        instance.addRef(sequenceDictionary);
       
        File metricsJson = File.createTempFile("alignmentFilterMetricsSets", ".json", new File("testdata"));
        System.out.println("Output metrics: " + metricsJson.getPath());
        instance.output(metricsJson);
        assertEquals(CheckMd5.getFileMd5(metricsJson), "27c570d2fd8fbc098b7c219b3883e4be");
        metricsJson.deleteOnExit();
    }
}
