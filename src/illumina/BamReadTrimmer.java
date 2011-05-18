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
 */

package illumina;

import java.io.File;
import net.sf.picard.cmdline.CommandLineProgram;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.util.Log;

/**
 * The class to strip part of a read (fixed position) - typically a prefix of the forward read,
 * and optionally place this and its quality in BAM tags.
 * 
 * @author Guoying Qi
 */

public class BamReadTrimmer extends CommandLineProgram {
    
    private final Log log = Log.getInstance(Illumina2bam.class);
    
    private final String programName = "bamReadTrimmer";
    
    private final String programDS = "Strip part of a read in fixed positionos, optionally place this and its quality in BAM tags";
    
    @Usage(programVersion="0.01")
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". ";
    
    @Option(shortName= StandardOptionDefinitions.INPUT_SHORT_NAME, doc="The input SAM or BAM file to trim.")
    public File INPUT;

    @Option(shortName=StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc="The output file after trimming.")
    public File OUTPUT;

    @Option(shortName="FORWARD", doc="Just trim the forward read if true.")
    public Boolean ONLY_FORWARD_READ = true;
    
    @Option(shortName="POS", doc="First position to be trimmed.")
    public Integer FIRST_POSITION_TO_TRIM;
    
    @Option(shortName="LEN", doc="The lenght to be trimmed.")
    public Integer TRIM_LENGTH;
    
    @Option(shortName="SAVE", doc="Timmed bases to be saved?", optional=true)
    public Boolean SAVE_TRIM = true;
    
    @Option(shortName="RS", doc="Tag name to be used for timmed bases.", optional=true)
    public String TRIM_BASE_TAG = "rs";
    
    @Option(shortName="QS", doc="Tag name to be used for timmed qualities.", optional=true)
    public String TRIM_QUALITY_TAG = "qs";   
    

    @Override
    protected int doWork() {
        
        return 0;
    }
    
    /**
     * 
     * @param args 
     */
    public static void main(final String[] args) {
        
        System.exit(new BamReadTrimmer().instanceMain(args));
    }
}
