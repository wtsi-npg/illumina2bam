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
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

/**
 *
 * @author Guoying Qi
 */
public class AlignmentFilter extends Illumina2bamCommandLine {
    
    private final Log log = Log.getInstance(AlignmentFilter.class);
    
    private final String programName = "AlignmentFiler";
    
    private final String programDS = "Give a list of SAM/BAM files with the same set of records "
                                   + "but aligned with different references, "
                                   + "split reads into different files according to alignments. "
                                   + "You have option to put unaligned reads into one of output files";
   
    @Usage(programVersion= version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". "; 
 
    
    @Option(shortName="IN",
            doc="The input SAM or BAM file with alignment. "
              + "The main input file with most of head informaiton should be at the first.")
    public final List<File> INPUT_ALIGNMENT = new ArrayList<File>();
    
    @Option(shortName="OUT",
            doc="The ouput SAM or BAM file. "
              + "It should have the same number of file as the input.")
    public final List<File> OUTPUT_ALIGNMENT = new ArrayList<File>();

    @Option(shortName="UNALIGNED",
            doc="The ouput SAM or BAM file for reads not alignment. "
               + "If not given, the unaligned reads will put into the last output file.", optional= true)
    public File OUTPUT_UNALIGNED;

    @Override
    protected int doWork() {

        log.info("Checking input and out files");
        if(this.INPUT_ALIGNMENT.isEmpty()){
            throw new RuntimeException("NO ANY INPUT ALIGNMENT File given!");
        }
        for(File file : INPUT_ALIGNMENT){
            IoUtil.assertFileIsReadable(file);
        }

        if( this.OUTPUT_ALIGNMENT.size() != this.INPUT_ALIGNMENT.size() ){
            throw new RuntimeException("The number of input and output file should be the same!");
        }
        for(File file : OUTPUT_ALIGNMENT){
            IoUtil.assertFileIsWritable(file);
        }
        
        if(this.OUTPUT_UNALIGNED != null ){
            IoUtil.assertFileIsWritable(this.OUTPUT_UNALIGNED);
        }

        
        log.info("Open input files");
        final List<SAMFileReader> inputReaderList  = new ArrayList<SAMFileReader>();
        for(File file : INPUT_ALIGNMENT){
             final SAMFileReader reader = new SAMFileReader(file);
             inputReaderList.add(reader);
        }
        
        
        log.info("Open output files");
        final List<SAMFileWriter> outputWriterList  = new ArrayList<SAMFileWriter>();
        int outputCount = 0;
        for(File outFile : OUTPUT_ALIGNMENT){
           final SAMFileHeader outputHeader = inputReaderList.get(outputCount).getFileHeader().clone();
           outputHeader.addProgramRecord(this.getThisProgramRecord(programName, programDS));
           final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader,  true, outFile);
           outputWriterList.add(out);
           outputCount++;
        }

        SAMFileWriter outputWriterUnaligned = null;
        if(this.OUTPUT_UNALIGNED != null ){
            final SAMFileHeader outputHeader = inputReaderList.get(0).getFileHeader().clone();
            outputHeader.setSequenceDictionary(null);
            outputHeader.addProgramRecord(this.getThisProgramRecord(programName, programDS));
            outputWriterUnaligned = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader,  true, this.OUTPUT_UNALIGNED);
        }

        log.info("Starting read and wring files");
        List<SAMRecordIterator> iteratorList = new ArrayList<SAMRecordIterator>();
        for(SAMFileReader reader : inputReaderList){
            SAMRecordIterator iterator = reader.iterator();
            iteratorList.add(iterator);
        }

        while(iteratorList.get(0).hasNext()){

            List<SAMRecord> recordList = new ArrayList<SAMRecord>();
            List<SAMRecord> pairedRecordList = new ArrayList<SAMRecord>();
            
            for(SAMRecordIterator iterator : iteratorList){
                
                SAMRecord tempRecord = iterator.next();
                recordList.add(tempRecord);
                if(tempRecord.getReadPairedFlag()){
                    SAMRecord tempPairedRecord = iterator.next();
                    pairedRecordList.add(tempPairedRecord);
                }
            }
        }

        return 0;
    }


    public static void main(final String[] args) {
        
        System.exit(new AlignmentFilter().instanceMain(args));
    }
}
