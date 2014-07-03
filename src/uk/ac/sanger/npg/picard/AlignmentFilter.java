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
import java.util.ArrayList;
import java.util.List;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.*;

/**
 *
 * @author gq1@sanger.ac.uk
 */

public class AlignmentFilter extends PicardCommandLine {

/**
 * Exception to be thrown on finding template(name) grouped alignment records from 
 * different input files are either in a different order to the first file, or
 * missing from the first file.
 */
public class RecordMissingOrOutOfOrder extends RuntimeException {}

/*
 * A wrapper around any SAMRecordIterator to support a peek() method
 */
static private class SAMRecordPeekableIterator implements SAMRecordIterator{
    private SAMRecordIterator si = null;
    private SAMRecord nextRecord = null;

    public SAMRecordPeekableIterator(SAMRecordIterator i) {
        si = i;
        if (si.hasNext()) { nextRecord = si.next(); }
    }

    public SAMRecord next() {
        SAMRecord tmpRecord = nextRecord;
        if (si.hasNext()) { nextRecord = si.next(); }
        else              { nextRecord = null; }
        return null == tmpRecord ? si.next() : tmpRecord ; //using si.next to throw appropriate exception
    }

    public boolean hasNext() {
        return (nextRecord != null);
    }

    public SAMRecordIterator assertSorted(SAMFileHeader.SortOrder order) {
        si.assertSorted(order);
        return this;
    }

    public void close() { si.close(); }

    public void remove() { throw new UnsupportedOperationException(); }

    public SAMRecord peek() {
        return nextRecord;
    }

}


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

        int numInput = this.INPUT_ALIGNMENT.size();        
        AlignmentFilterMetric metrics = new AlignmentFilterMetric(numInput);
        metrics.setProgramName(programName);
        metrics.setProgramCommand(this.getCommandLine());
        metrics.setProgramVersion(this.getProgramVersion());

        log.info("Open output files with headers");
        final List<SAMFileWriter> outputWriterList  = new ArrayList<SAMFileWriter>();
        int outputCount = 0;
        
        for(File outFile : OUTPUT_ALIGNMENT){
           
           SAMFileHeader header = inputReaderList.get(outputCount).getFileHeader();
           SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();
           metrics.addRef(sequenceDictionary);

           final SAMFileHeader outputHeader = header.clone();           
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
        List<SAMRecordPeekableIterator> inputReaderIteratorList = new ArrayList<SAMRecordPeekableIterator>();
        for(SAMFileReader reader : inputReaderList){
            SAMRecordPeekableIterator iterator = new SAMRecordPeekableIterator(reader.iterator());
            inputReaderIteratorList.add(iterator);
        }
 
        int totalReads = 0;
        int readsCountUnaligned = 0;
        int [] readsCountPerRef = new int [numInput];
        
        /*
         * Loop until we have read all records from all input files
         */
        
        while(inputReaderIteratorList.get(0).hasNext()){
            totalReads++;

            ArrayList<ArrayList<SAMRecord>> recordList = new ArrayList<ArrayList<SAMRecord>>();

            /*
             * read the next set of records from each file in turn
             * A 'set' of records is made of consecutive records with the same name
             * This may be one record, or two if paired, or more if there are secondary or supplementary alignments
             *
             */
            String name = inputReaderIteratorList.get(0).peek().getReadName();
            for(SAMRecordPeekableIterator inputReaderIterator : inputReaderIteratorList){

                ArrayList<SAMRecord> recordSet = new ArrayList<SAMRecord>();

                // while the name does not change, add to record set
                while (inputReaderIterator.hasNext() && inputReaderIterator.peek().getReadName().equals(name)) {
                    recordSet.add(inputReaderIterator.next());
                }

                recordList.add(recordSet);

            }
            
            metrics.checkNextReadsForChimera(recordList);

            int firstAlignedIndex = this.checkOneRecord(recordList);
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

            ArrayList<SAMRecord> recordSet = recordList.get(firstAlignedIndex);
            for (SAMRecord sam : recordSet) {
                this.removeAlignmentsFromUnalignedRecord(sam);
                tempOut.addAlignment(sam);
            }

        }

        for(SAMRecordPeekableIterator inputReaderIterator : inputReaderIteratorList){
            if(inputReaderIterator.hasNext()){ throw new RecordMissingOrOutOfOrder(); }
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


    /**
     * Find the first aligned read in the record list
     * A read is treated as 'aligned' if any of the reads in a set are aligned
     * Returns an index into the readlist, or -1 if no aligned reads found
     *
     * @param recordList
     * @param pairedRecordList
     * @param isPairedRead
     * @return
     */
    public int checkOneRecord (ArrayList<ArrayList<SAMRecord>> recordList){
        int firstAlignedIndex = 0;

        // Look at all of the record sets
        for(ArrayList<SAMRecord> recordSet : recordList ){
            // for each set, see if any of the records are aligned
            for (SAMRecord sam : recordSet) {
                if (!sam.getReadUnmappedFlag()) {
                    return firstAlignedIndex; // found an aligned record in the set!
                }
            }
            firstAlignedIndex++;
        }

        return -1; // no aligned records found
    }
    
    /**
     * 
     * @param args
     */
    public static void main(final String[] args) {

        System.exit(new AlignmentFilter().instanceMain(args));
    }
}
