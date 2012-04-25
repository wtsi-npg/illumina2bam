/*
 * Copyright (C) 2012 GRL
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
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.*;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class BamQualityQuantisation extends Illumina2bamCommandLine {
    
    private final Log log = Log.getInstance(BamQualityQuantisation.class);
    
    private final String programName = "BamQualityQuantisation";
    
    private final String programDS = "Quantise the original Illumina canned qualities into reduced resolution scores"
            + " using bins 1->9=6,10->19=15,20->24=22,25->29=27,30->34=33,35->39=37,40->59=41";
    
    @Usage(programVersion=version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". "; 
    
    /**
     * input sam or bam file
     */
    @Option(shortName=StandardOptionDefinitions.INPUT_SHORT_NAME, doc="The input SAM or BAM file. ")
    public File INPUT;
    
    /**
     * output sam or bam file
     */
    @Option(shortName=StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc="The ouput SAM or BAM file. ")
    public File OUTPUT;
    
    /**
     * Quantise original quality scores from OQ tag in bam record and set the results as new qualities if true
     */
    @Option(shortName= "OQ", doc="Use original quality scores from OQ tag. ")
    public Boolean USE_OLD_QUALITY = false;


    @Override
    protected int doWork() {
        
        this.log.info("Checking input and output file");
        IoUtil.assertFileIsReadable(INPUT);
        IoUtil.assertFileIsWritable(OUTPUT); 
         
        log.info("Open input file: " + INPUT.getName());
        final SAMFileReader in  = new SAMFileReader(INPUT);
        
        final SAMFileHeader header = in.getFileHeader();
        final SAMFileHeader outputHeader = header.clone();
        this.addProgramRecordToHead(outputHeader, this.getThisProgramRecord(programName, programDS));
        
        log.info("Open output file with header: " + OUTPUT.getName());
        final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader, true, OUTPUT);
        
        log.info("Quantising records");
        for (SAMRecord record : in) {
            
            if(!this.USE_OLD_QUALITY){
               byte [] quals = record.getBaseQualities();
               record.setBaseQualities(this.quantiseQualities(quals));
            } else {
               Object oq = record.getAttribute("OQ");
               if(oq == null){
                    throw new RuntimeException("No OQ tag available for record " + record.getReadName());
               }else{
                  record.setBaseQualities(this.quantiseQualities( oq.toString() ));
               }
            }
            out.addAlignment(record);
        }

        in.close();
        out.close();

        log.info("Quantisation finished, quantised file: " + this.OUTPUT);
       
        return 0;    
    }
    
    /**
     * 
     * @param quals quality scores in string
     * @return quantised scores
     */
    public byte[] quantiseQualities(String quals){
        byte [] qualsInByte = SAMUtils.fastqToPhred(quals);
        byte [] newQualsInByte = this.quantiseQualities(qualsInByte);
 
        return newQualsInByte;        
    }
    
    /**
     * 
     * @param quals quality scores to quantise
     * @return quantised scores
     */
    public byte [] quantiseQualities(byte [] quals){

        for(int i=0; i<quals.length; i++){
            quals[i] = this.getQuantisedScore(quals[i]);
        }
        return quals;
    }
    
    /**
     * 
     * @param score
     * @return new score
     */
    private byte getQuantisedScore(byte score){
        
        byte newScore;
        switch (score) {
            case 0: 
                newScore = score;
                break;
            case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9:
                newScore = 6;
                break;
            case 10: case 11: case 12: case 13: case 14: case 15: case 16: case 17: case 18: case 19:
                newScore = 15;
                break;
            case 20: case 21: case 22: case 23: case 24:
                newScore = 22;
                break;
            case 25: case 26: case 27: case 28: case 29:
                newScore = 27;
                break;
            case 30: case 31: case 32: case 33: case 34:
                newScore = 33;
                break;
            case 35: case 36: case 37: case 38: case 39:
                newScore = 37;
                break;
            default:
                if(score > 39 && score < 60){
                    newScore = 41;
                }else {
                    throw new IllegalArgumentException("Invalid quality score: " + score);
                }
        }
        return newScore;
    }
    
    /**
     * 
     * @param argv 
     */
    public static void main(final String[] argv) {
        System.exit(new BamQualityQuantisation().instanceMain(argv));
    }
}
