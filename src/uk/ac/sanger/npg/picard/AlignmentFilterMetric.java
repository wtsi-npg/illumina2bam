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
import java.util.ArrayList;
import java.util.List;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class AlignmentFilterMetric {
    
    private final Log log = Log.getInstance(AlignmentFilterMetric.class);
    
    private String programName;
    
    private String programCommand;
    
    private String programVersion;
    
    private int numberAlignments;

    private int totalReads;
    
    private int readsCountUnaligned;
    
    private int [] readsCountPerRef;
    
    private int [][] chimericReadsCount;
    
    private int [] readsCountByAlignedNumForward;
    private int [] readsCountByAlignedNumReverse;
    
    private ArrayList<ArrayList<SQ>> refList;
    
    /**
     * 
     * @param numberAlignments
     */
    public AlignmentFilterMetric(int numberAlignments){
        
        this.numberAlignments         = numberAlignments;

        chimericReadsCount            = new int[numberAlignments][numberAlignments];
        readsCountByAlignedNumForward = new int[numberAlignments+1];
        readsCountByAlignedNumReverse = new int[numberAlignments+1];
        
        refList = new ArrayList<ArrayList<SQ>>(numberAlignments);
        
    }
    
    /**
     * 
     * @param recordList
     * @param pairedRecordList
     */
    public void checkNextReadsForChimera(ArrayList<ArrayList<SAMRecord>> recordList){

        int [] alignmentByRef = this.checkAlignmentsByRef(recordList,false);
        int sumAlignments = this.sumOfArray(alignmentByRef);
        
        int [] alignmentByRefPaired = this.checkAlignmentsByRef(recordList,true);
        int sumAlignmentsPaired = this.sumOfArray(alignmentByRefPaired);

        
        if(sumAlignments == 1 && sumAlignmentsPaired == 1){
            
            int indexRef = this.indexAlignment(alignmentByRef);
            int indexRefPaired = this.indexAlignment(alignmentByRefPaired);
            
            if(indexRef != -1 && indexRefPaired != -1){
                
               getChimericReadsCount()[indexRef][indexRefPaired]++;
            }
            
            if(indexRef != indexRefPaired){
                log.debug("We seem to have a problem: indexRef="+indexRef+"  indexRefPaired="+indexRefPaired);
            }
        }

        readsCountByAlignedNumForward[sumAlignments]++;
        readsCountByAlignedNumReverse[sumAlignmentsPaired]++;
        
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
    

    static private int[] checkAlignmentsByRef(ArrayList<ArrayList<SAMRecord>> recordList, boolean result){
        
        int [] alignmentsByRef = new int[recordList.size()];
        
        int count = 0;
        
        for (List<SAMRecord> recordSet: recordList) {
    
            int found = 0;
            for (SAMRecord record: recordSet) {
                if( (record.getReadPairedFlag() && record.getSecondOfPairFlag()) == result) {
                    if (!record.getReadUnmappedFlag()) {
                        found = 1;
                    }
                }
            }
            alignmentsByRef[count++] = found;
        }
        return alignmentsByRef;
    }
    
    /**
     * log information
     */
    public void output(){
        
        log.info("Total Reads: " + this.getTotalReads());
        log.info("Unaligned Reads: " + this.getReadsCountUnaligned());

        if (this.readsCountPerRef != null) {
            for (int c : this.getReadsCountPerRef()) {
                log.info("Reads Count per Ref: " + c);
            }
        }else{
            log.info("Reads Count per Ref Information is not available ");
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
    
    /**
     * 
     * @param sequenceDictionary
     */
    public void addRef(SAMSequenceDictionary sequenceDictionary){
        
        ArrayList<SQ> refSQList = new ArrayList<SQ>();
        for(SAMSequenceRecord samSequenceRecord : sequenceDictionary.getSequences()){
            SQ sq = new SQ(samSequenceRecord);
            refSQList.add(sq);
        }
        this.addRef(refSQList);
    }
    
    /**
     * 
     * @param refSQList
     */
    public void addRef(ArrayList<SQ> refSQList){
        this.getRefList().add(refSQList);        
    }

    /**
     * @return the refList
     */
    public ArrayList<ArrayList<SQ>> getRefList() {
        return refList;
    }

    /**
     * @param refList the refList to set
     */
    public void setRefList(ArrayList<ArrayList<SQ>> refList) {
        this.refList = refList;
    }

    /**
     * @return the programName
     */
    public String getProgramName() {
        return programName;
    }

    /**
     * @param programName the programName to set
     */
    public void setProgramName(String programName) {
        this.programName = programName;
    }

    /**
     * @return the programCommand
     */
    public String getProgramCommand() {
        return programCommand;
    }

    /**
     * @param programCommand the programCommand to set
     */
    public void setProgramCommand(String programCommand) {
        this.programCommand = programCommand;
    }

    /**
     * @return the programVersion
     */
    public String getProgramVersion() {
        return programVersion;
    }

    /**
     * @param programVersion the programVersion to set
     */
    public void setProgramVersion(String programVersion) {
        this.programVersion = programVersion;
    }
    
    /**
     * SQ record in SAM header
     */
    public static class SQ {
        private String sn;
        private String as;
        private String sp;
        private int ln;
        private String ur;

        /**
         * 
         * @param sn
         * @param as
         * @param sp
         * @param ln
         * @param ur
         */
        public SQ(String sn, String as, String sp, int ln, String ur){
            this.sn = sn;
            this.as = as;
            this.sp = sp;
            this.ln = ln;
            this.ur = ur;
        }
        
        /**
         * 
         * @param samSequenceRecord
         */
        public SQ(SAMSequenceRecord samSequenceRecord){
            this.sn = samSequenceRecord.getSequenceName();
            this.as = samSequenceRecord.getAssembly();
            this.sp = samSequenceRecord.getSpecies();
            this.ln = samSequenceRecord.getSequenceLength();
            this.ur = samSequenceRecord.getAttribute("UR");
        }

        /**
         * @return the sn
         */
        public String getSn() {
            return sn;
        }

        /**
         * @param sn the sn to set
         */
        public void setSn(String sn) {
            this.sn = sn;
        }

        /**
         * @return the as
         */
        public String getAs() {
            return as;
        }

        /**
         * @param as the as to set
         */
        public void setAs(String as) {
            this.as = as;
        }

        /**
         * @return the sp
         */
        public String getSp() {
            return sp;
        }

        /**
         * @param sp the sp to set
         */
        public void setSp(String sp) {
            this.sp = sp;
        }

        /**
         * @return the ln
         */
        public int getLn() {
            return ln;
        }

        /**
         * @param ln the ln to set
         */
        public void setLn(int ln) {
            this.ln = ln;
        }

        /**
         * @return the ur
         */
        public String getUr() {
            return ur;
        }

        /**
         * @param ur the ur to set
         */
        public void setUr(String ur) {
            this.ur = ur;
        }
    }
}
