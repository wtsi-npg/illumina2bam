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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMRecord;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class AlignmentFilterMetric {
    
    private final Log log = Log.getInstance(AlignmentFilterMetric.class);
    
    private int numberAlignments;

    private int totalReads;
    
    private int readsCountUnaligned;
    
    private int [] readsCountPerRef;
    
    private int [][] chimericReadsCount;
    
    private int [] readsCountByAlignedNumForward;
    private int [] readsCountByAlignedNumReverse;
    
    /**
     * 
     * @param numberAlignments
     */
    public AlignmentFilterMetric(int numberAlignments){
        
        this.numberAlignments         = numberAlignments;

        chimericReadsCount            = new int[numberAlignments][numberAlignments];
        readsCountByAlignedNumForward = new int[numberAlignments+1];
        readsCountByAlignedNumReverse = new int[numberAlignments+1];
        
    }
    
    /**
     * 
     * @param recordList
     * @param pairedRecordList
     */
    public void checkNextReadsForChimera(List<SAMRecord> recordList,  List<SAMRecord> pairedRecordList){

        int [] alignmentByRef = this.checkAlignmentsByRef(recordList);
        int sumAlignments = this.sumOfArray(alignmentByRef);
        
        int [] alignmentByRefPaired = this.checkAlignmentsByRef(pairedRecordList);
        int sumAlignmentsPaired = this.sumOfArray(alignmentByRefPaired);
        
        if(sumAlignments == 1 && sumAlignmentsPaired == 1){
            
            int indexRef = this.indexAlignment(alignmentByRef);
            int indexRefPaired = this.indexAlignment(alignmentByRefPaired);
            
            if(indexRef != -1 && indexRefPaired != -1){
                
               getChimericReadsCount()[indexRef][indexRefPaired]++;
            }
            
            if(indexRef != indexRefPaired){
  
                for(int i= 0; i< this.numberAlignments; i++){
                    log.debug(recordList.get(i).format());
                    log.debug(pairedRecordList.get(i).format());
                }
            }
        }

        readsCountByAlignedNumForward[sumAlignments]++;
        
        if( !pairedRecordList.isEmpty() ){
            readsCountByAlignedNumReverse[sumAlignmentsPaired]++;
        }
        
    }
    
    private int sumOfArray (int [] array){
        
        int sum = 0;
        
        for(int e: array){
            sum += e;
        }
        return sum;
    }
    
    private int indexAlignment (int [] array){

        for ( int i=0; i < array.length; i++){
            if(array[i] == 1){
                return i;
            }
        }
        return -1;
    }
    

    private int[] checkAlignmentsByRef(List<SAMRecord> recordList){
        
        int [] alignmentsByRef = new int[recordList.size()];
        
        int count = 0;
        
        for(SAMRecord record: recordList){
            if( record.getReadUnmappedFlag() ){
                alignmentsByRef[count] = 0;
            }else{
                alignmentsByRef[count] = 1;
            }
            count++;
        }
        return alignmentsByRef;
    }
    
    /**
     * log information
     */
    public void output(){
        
        log.info("Total Reads: " + this.getTotalReads());
        log.info("Unaligned Reads: " + this.getReadsCountUnaligned());
        
        for(int c: this.getReadsCountPerRef()){
            log.info("Reads count per Ref: " + c);
        }
        
        
        log.info("Chimeric Reads Count:");
        for (int i = 0; i < this.getChimericReadsCount().length; i++){
            for(int j = 0; j<this.getChimericReadsCount()[0].length; j++){
                log.info("Forward " + (i+1) + " Reverse " + (j+1) + ": " + this.getChimericReadsCount()[i][j]);
            }
        }
        
        for (int i=0; i<=this.numberAlignments; i++){
            log.info("Aligned to " + i + " refs. F: " + this.readsCountByAlignedNumForward[i] + ", R: " + this.readsCountByAlignedNumReverse[i]);
        }
    }
    
    /**
     * 
     * @param outJson
     * @throws IOException
     */
    public void output(File outJson){
        
        this.output();
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(outJson, this);
        } catch (JsonGenerationException ex) {
            log.error(ex);
        } catch (JsonMappingException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        }
    }

    /**
     * @return the totalReads
     */
    public int getTotalReads() {
        return totalReads;
    }

    /**
     * @param totalReads the totalReads to set
     */
    public void setTotalReads(int totalReads) {
        this.totalReads = totalReads;
    }

    /**
     * @return the readsCountPerRef
     */
    public int[] getReadsCountPerRef() {
        return readsCountPerRef;
    }

    /**
     * @param readsCountPerRef the readsCountPerRef to set
     */
    public void setReadsCountPerRef(int[] readsCountPerRef) {
        this.readsCountPerRef = readsCountPerRef;
    }

    /**
     * @return the readsCountUnaligned
     */
    public int getReadsCountUnaligned() {
        return readsCountUnaligned;
    }

    /**
     * @param readsCountUnaligned the readsCountUnaligned to set
     */
    public void setReadsCountUnaligned(int readsCountUnaligned) {
        this.readsCountUnaligned = readsCountUnaligned;
    }

    /**
     * @return the chimericReadsCount
     */
    public int[][] getChimericReadsCount() {
        return chimericReadsCount;
    }

    /**
     * @param chimericReadsCount the chimericReadsCount to set
     */
    public void setChimericReadsCount(int[][] chimericReadsCount) {
        this.chimericReadsCount = chimericReadsCount;
    }

    /**
     * @return the readsCountByAlignedNum
     */
    public int[] getReadsCountByAlignedNumForward() {
        return readsCountByAlignedNumForward;
    }

    /**
     * @param readsCountByAlignedNum the readsCountByAlignedNum to set
     */
    public void setReadsCountByAlignedNumForward(int[] readsCountByAlignedNum) {
        this.readsCountByAlignedNumForward = readsCountByAlignedNum;
    }

    /**
     * @return the readsCountByAlignedNumReverse
     */
    public int[] getReadsCountByAlignedNumReverse() {
        return readsCountByAlignedNumReverse;
    }

    /**
     * @param readsCountByAlignedNumReverse the readsCountByAlignedNumReverse to set
     */
    public void setReadsCountByAlignedNumReverse(int[] readsCountByAlignedNumReverse) {
        this.readsCountByAlignedNumReverse = readsCountByAlignedNumReverse;
    }

    /**
     * @return the numberAlignments
     */
    public int getNumberAlignments() {
        return numberAlignments;
    }

    /**
     * @param numberAlignments the numberAlignments to set
     */
    public void setNumberAlignments(int numberAlignments) {
        this.numberAlignments = numberAlignments;
    }
}
