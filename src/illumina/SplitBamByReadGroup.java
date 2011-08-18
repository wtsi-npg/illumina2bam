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
import java.util.HashMap;
import java.util.List;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;

/**
 *
 * @author Guoying Qi
 */
public class SplitBamByReadGroup extends Illumina2bamCommandLine {
    
    private final Log log = Log.getInstance(BamMerger.class);
    
    private final String programName = "SplitBamByReadGroup";
    
    private final String programDS = "Split a BAM file into multiple BAM files based on ReadGroup. "
            + "Headers are a copy of the original file, removing @RGs where IDs match with the other ReadGroup IDs";
   
    @Usage(programVersion= version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". "; 
       
    @Option(shortName= StandardOptionDefinitions.INPUT_SHORT_NAME, doc="The input SAM or BAM file with multiple read groups.")
    public File INPUT;
    
    @Option(shortName= "O", doc="The prefix for output bam/sam file.")
    public String OUTPUT_PREFIX;
    
    @Option(shortName= "TRIM", doc="The common RG name head to trim from the output file name.", optional=true)
    public String OUTPUT_COMMON_RG_HEAD_TO_TRIM;

    @Override
    protected int doWork() {
        
        log.info("Checking input file");
        IoUtil.assertFileIsReadable(INPUT);
        

        log.info("Open input file to split: " + INPUT.getName());
        final SAMFileReader in  = new SAMFileReader(INPUT);
        
        final SAMFileHeader header = in.getFileHeader();
        final List<SAMReadGroupRecord> readGroupList = header.getReadGroups();
        
        if(readGroupList.isEmpty()){
            log.error("There is no Read Group information in the input file: " + INPUT);
            return 1;
        }

        
        log.info("Open a list of output file based on the read groups in the input header");        
        HashMap<String, SAMFileWriter> outputFileList = new HashMap<String, SAMFileWriter>();
    
        for(SAMReadGroupRecord rg: readGroupList){
            
            String id = rg.getId();
            String idForFileName = id;
            if(this.OUTPUT_COMMON_RG_HEAD_TO_TRIM != null){
                if(!idForFileName.startsWith(this.OUTPUT_COMMON_RG_HEAD_TO_TRIM)){
                   log.error("Read goup ID not starting with the given common prefix");                     
                }
                idForFileName = idForFileName.replaceFirst(this.OUTPUT_COMMON_RG_HEAD_TO_TRIM, "");
            }
            
            String filename = this.OUTPUT_PREFIX + idForFileName + ".";
            if(in.isBinary()){
                filename += "bam";
            }else{
                filename += "sam";
            }            
            File outputFile = new File(filename);
            IoUtil.assertFileIsWritable( outputFile );
            
            SAMFileHeader newHeader = header.clone();
            List<SAMReadGroupRecord> newRGList = new ArrayList<SAMReadGroupRecord>();
            newRGList.add(rg);
            
            newHeader.setReadGroups(newRGList);

            this.addProgramRecordToHead(newHeader, this.getThisProgramRecord(programName, programDS));
            
            final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(newHeader,  true, outputFile);
            
            outputFileList.put(id, out);
        }
        
        
        log.info("Spliting the input file now");
        for(SAMRecord rec: in){
            String rgId = (String) rec.getAttribute("RG");
            
            if( rgId == null ){
                throw new RuntimeException("Record without read group id: " + rec.getReadName());
            }
            
            SAMFileWriter out = outputFileList.get(rgId);
            if( out == null ){
                throw new RuntimeException("Read group id does not exist in the header: " + rgId +  " in record " + rec.getReadName());
            }
            outputFileList.get(rgId).addAlignment(rec);
        }
        
        
        log.info("Close the output files");
        for(SAMFileWriter out : outputFileList.values()){
            out.close();
        }
        
        log.info("Splitting finished");
        
        return 0;
    }
    
    public static void main(final String[] args) {
        
        System.exit(new SplitBamByReadGroup().instanceMain(args));
    }   
}