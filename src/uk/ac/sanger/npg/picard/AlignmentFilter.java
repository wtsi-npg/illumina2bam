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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.*;

/**
 *
 * @author Guoying Qi
 */
public class AlignmentFilter extends Illumina2bamCommandLine {

    private final Log log = Log.getInstance(AlignmentFilter.class);

    private final String programName = "AlignmentFilter";

    private final String programDS = "Give a list of SAM/BAM files with the same set of records and in the same order "
                                   + "but aligned with different references, "
                                   + "split reads into different files according to alignments. "
                                   + "You have option to put unaligned reads into one of output files or a separate file";

    @Usage(programVersion= version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". ";


    @Option(shortName="IN", doc="The input SAM or BAM file with alignment.",minElements=1, optional= false)
    public final List<File> INPUT_ALIGNMENT = new ArrayList<File>();

    @Option(shortName="OUT",
            doc="The ouput SAM or BAM file. "
              + "It should have the same number of files as the input.", minElements=1, optional= false)
    public final List<File> OUTPUT_ALIGNMENT = new ArrayList<File>();

    @Option(shortName="UNALIGNED",
            doc="The ouput SAM or BAM file for unaligned. "
               + "If not given, the unaligned reads will put into the last output file.", optional= true)
    public File OUTPUT_UNALIGNED;
    
    @Option(shortName="METRICS",
            doc="Metrics file name", optional= true)
    public File METRICS_FILE;

    @Override
    protected int doWork() {

        log.info("Checking input files");
        if(this.INPUT_ALIGNMENT.isEmpty()){
            throw new RuntimeException("NO ANY INPUT ALIGNMENT File given!");
        }
        for(File file : INPUT_ALIGNMENT){
            IoUtil.assertFileIsReadable(file);
        }


        log.info("Checking output files");
        if( this.OUTPUT_ALIGNMENT.size() != this.INPUT_ALIGNMENT.size() ){
            throw new RuntimeException("The number of input and output file should be the same!");
        }
        for(File file : OUTPUT_ALIGNMENT){
            IoUtil.assertFileIsWritable(file);
        }

        if(this.OUTPUT_UNALIGNED != null ){
            
            IoUtil.assertFileIsWritable(this.OUTPUT_UNALIGNED);
            
            if(this.METRICS_FILE == null){
                this.METRICS_FILE = new File(this.OUTPUT_UNALIGNED.getAbsoluteFile() + "_alignment_filter_metrics.json");
            }
        }

        if(this.METRICS_FILE == null){
            
            this.METRICS_FILE = new File(this.OUTPUT_ALIGNMENT.get(this.OUTPUT_ALIGNMENT.size() -1 ).getAbsoluteFile() + "_alignment_filter_metrics.json");
        }
        IoUtil.assertFileIsWritable(METRICS_FILE);

        log.info("Open input files");
        final List<SAMFileReader> inputReaderList  = new ArrayList<SAMFileReader>();
        for(File file : INPUT_ALIGNMENT){
             final SAMFileReader reader = new SAMFileReader(file);
             inputReaderList.add(reader);
        }


        log.info("Open output files with headers");
        final List<SAMFileWriter> outputWriterList  = new ArrayList<SAMFileWriter>();
        int outputCount = 0;
        for(File outFile : OUTPUT_ALIGNMENT){
           final SAMFileHeader outputHeader = inputReaderList.get(outputCount).getFileHeader().clone();
           outputHeader.setSortOrder(SAMFileHeader.SortOrder.unsorted);
           this.addProgramRecordToHead(outputHeader, this.getThisProgramRecord(programName, programDS));
           final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader,  true, outFile);
           outputWriterList.add(out);
           outputCount++;
        }

        SAMFileWriter outputWriterUnaligned = null;
        if(this.OUTPUT_UNALIGNED != null ){
            final SAMFileHeader outputHeader = inputReaderList.get( outputCount - 1 ).getFileHeader().clone();
            outputHeader.setSequenceDictionary(new SAMSequenceDictionary());
            outputHeader.setSortOrder(SAMFileHeader.SortOrder.unsorted);
            outputHeader.addProgramRecord(this.getThisProgramRecord(programName, programDS));
            outputWriterUnaligned = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader,  true, this.OUTPUT_UNALIGNED);
        }

        log.info("Starting read and writing records");
        List<SAMRecordIterator> inputReaderIteratorList = new ArrayList<SAMRecordIterator>();
        for(SAMFileReader reader : inputReaderList){
            SAMRecordIterator iterator = reader.iterator();
            inputReaderIteratorList.add(iterator);
        }

        int numInput = this.INPUT_ALIGNMENT.size();        
        AlignmentFilterMetric metrics = new AlignmentFilterMetric(numInput);
 
        int totalReads = 0;
        int readsCountUnaligned = 0;
        int [] readsCountPerRef = new int [numInput];
        
        while(inputReaderIteratorList.get(0).hasNext()){
            
            totalReads++;

            List<SAMRecord> recordList = new ArrayList<SAMRecord>();
            List<SAMRecord> pairedRecordList = new ArrayList<SAMRecord>();
            boolean isPairedRead = false;

            for(SAMRecordIterator inputReaderIterator : inputReaderIteratorList){

                if (inputReaderIterator.hasNext()) {

                    SAMRecord tempRecord = inputReaderIterator.next();
                    recordList.add(tempRecord);

                    boolean tempPaired = tempRecord.getReadPairedFlag();
                    if( tempPaired ){
                        isPairedRead = true;
                    }

                    if ( tempPaired && inputReaderIterator.hasNext() ) {
                        SAMRecord tempPairedRecord = inputReaderIterator.next();
                        pairedRecordList.add(tempPairedRecord);
                    }

                }else{

                    throw new RuntimeException("The input files don't have the same set of records");
                }

            }
            
            metrics.checkNextReadsForChimera(recordList, pairedRecordList);

            int firstAlignedIndex = this.checkOneRecord(recordList, pairedRecordList, isPairedRead);

            SAMFileWriter tempOut;

            if(firstAlignedIndex != -1 ){
                tempOut = outputWriterList.get(firstAlignedIndex);
                readsCountPerRef[firstAlignedIndex]++;
            }else if(outputWriterUnaligned != null){
                tempOut = outputWriterUnaligned;
                firstAlignedIndex = outputCount -1;
                readsCountUnaligned++;
            }else{
                firstAlignedIndex = outputCount -1;
                tempOut = outputWriterList.get(firstAlignedIndex);
                readsCountUnaligned++;
            }

            SAMRecord samRecord = recordList.get(firstAlignedIndex);
            this.removeAlignmentsFromUnalignedRecord(samRecord);
            tempOut.addAlignment(samRecord);

            if(isPairedRead){
                samRecord = pairedRecordList.get(firstAlignedIndex);
                this.removeAlignmentsFromUnalignedRecord(samRecord);
                tempOut.addAlignment(samRecord);
            }
        }

        log.info("Closing all the files");
        for(SAMFileReader reader : inputReaderList){
            reader.close();
        }
        for(SAMFileWriter writer : outputWriterList){
            writer.close();
        }
        if(this.OUTPUT_UNALIGNED != null ){
           outputWriterUnaligned.close();
        }
        metrics.setReadsCountPerRef(readsCountPerRef);
        metrics.setTotalReads(totalReads);
        metrics.setReadsCountUnaligned(readsCountUnaligned);
        
        metrics.output( this.METRICS_FILE );
       
        return 0;
    }

    private void removeAlignmentsFromUnalignedRecord (SAMRecord samRecord){

        if(!samRecord.getReadUnmappedFlag()){
            return;
        }

        samRecord.setReferenceIndex(-1);
        samRecord.setAlignmentStart(0);
        samRecord.setMateReferenceIndex(-1);
        samRecord.setMateAlignmentStart(0);
        samRecord.setCigar(new Cigar());
        samRecord.setAttribute("MD", null);
        samRecord.setInferredInsertSize(0);
        samRecord.setMappingQuality(0);
    }


    public int checkOneRecord ( List<SAMRecord> recordList,  List<SAMRecord> pairedRecordList, boolean isPairedRead){

        if( isPairedRead && ( recordList.size() != pairedRecordList.size() ) ){

            throw new RuntimeException("Paired information is not correct in all input files for read: "
                     + recordList.get(0).getReadName()
                    + " " + recordList.get(0).getFlags());
        }

        int firstAlignedIndex = -1;

        int count = 0;
        String firstReadName = null;
        boolean firstPairedFlag = false;
        boolean firstFirstReadFlag = false;

        for(SAMRecord record : recordList ){

            String readName = record.getReadName();
            boolean pairedFlag = record.getReadPairedFlag();
            boolean firstReadFlag = record.getFirstOfPairFlag();

            if(count == 0){
                firstReadName = readName;
                firstPairedFlag = pairedFlag;
                firstFirstReadFlag = firstReadFlag;
            }

            if(!readName.equals(firstReadName) || firstPairedFlag != pairedFlag || firstFirstReadFlag != firstReadFlag ){

                throw new RuntimeException("Reads in input files are not the same: " + firstReadName +  " " + readName);

            }


            boolean unmappedFlag = record.getReadUnmappedFlag();
            boolean mateUnmappedFlag = record.getMateUnmappedFlag();

            boolean aligned = !unmappedFlag;

            SAMRecord record2;
            if(isPairedRead){

               record2 = pairedRecordList.get(count);
               String readName2 = record2.getReadName();
               boolean pairedFlag2 = record2.getReadPairedFlag();
               boolean firstReadFlag2 = record2.getFirstOfPairFlag();
               if( !readName2.equals(readName) || pairedFlag != pairedFlag2 || firstReadFlag == firstReadFlag2 ){
                    throw new RuntimeException("Paired reads are not together: " + readName + " " + readName2);
               }

               boolean unmappedFlag2 = record2.getReadUnmappedFlag();
               boolean mateUnmappedFlag2 = record2.getMateUnmappedFlag();

               if( mateUnmappedFlag != unmappedFlag2 || unmappedFlag != mateUnmappedFlag2 ){
                   throw new RuntimeException("Paired read alignment information not correct: " + readName);
               }

               aligned = aligned || !unmappedFlag2;
            }
            if(firstAlignedIndex == -1 && aligned ){
                firstAlignedIndex = count;
            }

            count++;
        }

        return firstAlignedIndex;

    }
    
    public static void main(final String[] args) {

        System.exit(new AlignmentFilter().instanceMain(args));
    }
}
