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
 * This is the main class to covert Illumina BCL file to BAM
 *
 */

package illumina;

import java.io.File;
import java.util.Date;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.Usage;
import net.sf.samtools.SAMFileWriter;

import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMReadGroupRecord;

/**
 *
 * @author Guoying Qi
 */
public class Illumina2bam extends Illumina2bamCommandLine {
    
    private final Log log = Log.getInstance(Illumina2bam.class);
    
    private final String programName = "illumina2bam";
    private final String programDS = "Convert Illumina BCL to BAM or SAM file";
    
    @Usage(programVersion=version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". ";
    
    @Option(shortName="I", doc="Illumina intensities directory including config xml file, and clocs files under lane directory.")
    public File INTENSITY_DIR;

    @Option(shortName="B", doc="Illumina basecalls directory including config xml file, and filter files, bcl, maybe scl files under lane cycle directory, using BaseCalls directory under intensities if not given. ", optional=true)
    public File BASECALLS_DIR;
    
    @Option(shortName="L", doc="Lane number.")
    public Integer LANE;

    @Option(shortName="O", doc="Output file name.")
    public File OUTPUT;

    @Option(shortName="E2", doc="Including second base call or not, default false.", optional=true)
    public boolean GENERATE_SECONDARY_BASE_CALLS = false;

    @Option(shortName="PF", doc="Filter cluster or not, default true.", optional=true)
    public boolean PF_FILTER = true;

    @Option(shortName="RG", doc="ID used to link RG header record with RG tag in SAM record, default 1.", optional=true)
    public String READ_GROUP_ID = "1";

    @Option(shortName="SM", doc="The name of the sequenced sample, using library name if not given.", optional=true)
    public String SAMPLE_ALIAS;

    @Option(shortName="LB", doc="The name of the sequenced library, default unknown.", optional=true)
    public String LIBRARY_NAME = "unknown";

    @Option(shortName="ST", doc="The name of the study.", optional=true)
    public String STUDY_NAME;

    @Option(shortName="PU", doc="The platform unit, using runfolder name plus lane number if not given.", optional=true)
    public String PLATFORM_UNIT;

    @Option(doc="The start date of the run, read from config file if not given.", optional=true)
    public Date RUN_START_DATE;

    @Option(shortName="SC", doc="Sequence center name, default SC for Sanger Center.", optional=true)
    public String SEQUENCING_CENTER = "SC";

    @Option(doc="The name of the sequencing technology that produced the read, default ILLUMINA.", optional=true)
    public String PLATFORM = "ILLUMINA";

    @Option(doc="If set, this is the first tile to be processed (for debugging).  Note that tiles are not processed in numerical order.",
    optional = true)
    public Integer FIRST_TILE;
    
    @Option(doc="If set, process no more than this many tiles (for debugging).", optional=true)
    public Integer TILE_LIMIT;
    
    //TODO: add command option to add ci tag
    
    //TODO: add command option to overwrite cycle range per read   
    

    @Override
    protected int doWork() {

        IoUtil.assertFileIsWritable(OUTPUT);
        
        if(this.BASECALLS_DIR == null){
            
          this.BASECALLS_DIR = new File(INTENSITY_DIR.getAbsoluteFile() + File.separator + "BaseCalls");
          log.info("BaseCalls directory not given, using " + this.BASECALLS_DIR);
        }

        Lane lane = new Lane(this.INTENSITY_DIR.getAbsolutePath(), this.BASECALLS_DIR.getAbsolutePath(), this.LANE, this.GENERATE_SECONDARY_BASE_CALLS, this.PF_FILTER, OUTPUT);

        try {
            log.info("Reading config xml files");
            lane.readConfigs();
        } catch (Exception ex) {
            log.error("Problems to read config xml file: " + ex.getMessage());
            return 1;
        }
        
        log.info("Generating illumina2bam program record");
        lane.setIllumina2bamProgram(this.getThisProgramRecord(this.programName, this.programDS));
        
        
        log.info("Generating read group record");
        String runfolderConfig = lane.getRunfolderConfig();
        String platformUnitConfig = null;
        if(runfolderConfig != null){
            platformUnitConfig = runfolderConfig + "_" + this.LANE;
        }        
        Date runDateConfig   = lane.getRunDateConfig();        
        lane.setReadGroup(this.generateSamReadGroupRecord(platformUnitConfig, runDateConfig));

        if( this.FIRST_TILE != null ){
            log.info("Trying to limit the number tiles from " + this.FIRST_TILE);
            if(this.TILE_LIMIT != null){
                log.info("Only process " + this.TILE_LIMIT + " tiles");
            }
            lane.reduceTileList(this.FIRST_TILE, this.TILE_LIMIT);
        }

        log.info("Generationg bam or sam file output stream with header");
        SAMFileWriter outBam = lane.generateOutputSamStream();
        
        log.info("Writing Basecall files to bam");
        try {
            lane.processTiles(outBam);
        } catch (Exception ex) {
            log.error( "Problems to process tiles " + ex.getMessage() );
            return 1;
        }

        outBam.close();
        
        log.info("BAM or SAM file generated: " + this.OUTPUT);

        return 0;
    }


    /**
     * Generate read group record
     * 
     * @param platformUnitConfig default platform unit from configure XML, which will be used if not given from command line, and could be null
     * @param runDateConfig default run date from configure XML, which will be used if not given from command line, and could be null
     * @return read group record for BAM header
     */
    public SAMReadGroupRecord generateSamReadGroupRecord(String platformUnitConfig, Date runDateConfig){
        
        SAMReadGroupRecord readGroup = new SAMReadGroupRecord(this.READ_GROUP_ID);
        
        readGroup.setLibrary(this.LIBRARY_NAME);
        
        if(this.SAMPLE_ALIAS == null){
           readGroup.setSample(this.LIBRARY_NAME);
        }else{
            readGroup.setSample(this.SAMPLE_ALIAS);
        }
        
        if( this.STUDY_NAME != null ){
            readGroup.setDescription("Study " + this.STUDY_NAME);
        }
        
        if(this.PLATFORM_UNIT != null){
            readGroup.setPlatformUnit(this.PLATFORM_UNIT);
        }else if(platformUnitConfig !=null){
            readGroup.setPlatformUnit(platformUnitConfig);
        }
        
        if(this.RUN_START_DATE != null){
            readGroup.setRunDate(RUN_START_DATE);
        }else if(runDateConfig != null){
            readGroup.setRunDate(runDateConfig);
        }
                
        readGroup.setPlatform(this.PLATFORM);
        
        readGroup.setSequencingCenter(this.SEQUENCING_CENTER);
        
        return readGroup;
    }
    
    /**
     * 
     * @param args example INTENSITY_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities BASECALLS_DIR=testdata/110323_HS13_06000_B_B039WABXX/Data/Intensities/BaseCalls LANE=1 OUTPUT=testdata/6000_1.sam  VALIDATION_STRINGENCY=STRICT CREATE_INDEX=false CREATE_MD5_FILE=true FIRST_TILE=1101 COMPRESSION_LEVEL=1 TILE_LIMIT=1
     */
    public static void main(final String[] args) {
        
        System.exit(new Illumina2bam().instanceMain(args));
    }
}
