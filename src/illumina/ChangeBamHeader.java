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
import java.util.Map;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMRecord;

/**
 *
 * @author Guoying Qi
 */
public class ChangeBamHeader extends Illumina2bamCommandLine {
    
    private final Log log = Log.getInstance(ChangeBamHeader.class);
    
    private final String programName = "ChangeBamHeader";
    
    private final String programDS = "Add extra PGs into bam header";
   
    @Usage(programVersion= version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". "; 
    
    @Option(shortName=StandardOptionDefinitions.INPUT_SHORT_NAME, doc="The input SAM or BAM file.")
    public File INPUT;
    
    @Option(shortName=StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc="The ouput SAM or BAM file. ")
    public File OUTPUT;
    
    @Option(doc="The extra PG with fields separated by tab for bam header including ID, PN, VN and CL etc, PP id will be reset.")
    public final List<String> PG = new ArrayList<String>();

    @Override
    protected int doWork() {
        
        this.log.info("Checking input and output file");
        IoUtil.assertFileIsReadable(INPUT);
        IoUtil.assertFileIsWritable(OUTPUT);

        log.info("Open input file: " + INPUT.getName());
        final SAMFileReader in  = new SAMFileReader(INPUT);
        
        final SAMFileHeader header = in.getFileHeader();
        final SAMFileHeader outputHeader = header.clone();
        List<SAMProgramRecord> pgList = header.getProgramRecords();

        for(String pg_fields: this.PG){
            SAMProgramRecord pg = this.getProgramRecordFromString(pg_fields);
            pg = this.makeUniqueProgramId(pgList, pg);
            this.addProgramRecordToHead(outputHeader, pg);
        }

        this.addProgramRecordToHead(outputHeader, this.getThisProgramRecord(programName, programDS));
        
        log.info("Open output file with header: " + OUTPUT.getName());
        final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader,  true, OUTPUT);
        
        log.info("Writing out all records");
        for (SAMRecord record : in) {
                out.addAlignment(record);
        }
        
        out.close();
        in.close();
        log.info("Changing header finished: " + this.OUTPUT);
        
        return 0;
    }
    
    /**
     * 
     * @param pg_fields
     * @return 
     */
    public SAMProgramRecord getProgramRecordFromString(String pg_fields){
        
        String []  fields = pg_fields.split("\t");
        
        if(fields.length == 0){
            throw new RuntimeException("There are no PG fields given: " + pg_fields);
        }
        
        HashMap<String, String> fieldsHash = new HashMap<String, String>();
        
        for ( String field : fields ) {
           String [] keyValue =  field.split(":", 2);
           if(keyValue.length != 2){
               throw new RuntimeException("PG field must be given in tag name and value in pair separated by :,  " + field );
           }
           
           fieldsHash.put(keyValue[0], keyValue[1]);
            
        }
        
        String id = fieldsHash.remove("ID");

        if(id==null){
            throw new RuntimeException("PG ID must be given: " + pg_fields);
        }
        
        SAMProgramRecord pgRecord = new SAMProgramRecord(id);
        for (Map.Entry<String, String> field : fieldsHash.entrySet()) {
            pgRecord.setAttribute (field.getKey(), field.getValue());
        }
        
        return pgRecord;
    }
    
    /**
     * 
     * @param argv 
     */
    public static void main(final String[] argv) {
        System.exit(new ChangeBamHeader().instanceMain(argv));
    }
}
