/*
 * 
 */

package illumina;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.picard.cmdline.CommandLineProgram;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.Usage;
import net.sf.samtools.SAMFileWriter;

import net.sf.picard.io.IoUtil;

/**
 *
 * @author Guoying Qi
 */
public class Illumina2bam extends CommandLineProgram {
    
    @Usage public final String USAGE ="Covert Illumina BCL to BAM or SAM file";

    @Option(shortName="I", doc="Illumina intensities diretory including config xml file and clocs files under lane directory")
    public String INTENSITY_DIR;

    @Option(shortName="B", doc="Illumina basecalls diretory including config xml file, and filter files, bcl and scl files under lane cycle directory")
    public String BASECALLS_DIR;
    
    @Option(shortName="L", doc="Lane number")
    public Integer LANE;

    @Option(shortName="O", doc="Output file name")
    public File OUTPUT;

    @Option(shortName="S", doc="Sample name", optional=true)
    public String SAMPLE_ALIAS;

    @Option(shortName="RG", doc="Read group name, default 1", optional=true)
    public String READ_GROUP_ID = "1";

    @Option(shortName="LB", doc="Library name", optional=true)
    public String LIBRARY_NAME;

    @Option(shortName="SC", doc="Sequence center name, default SC for Sanger Center", optional=true)
    public String SEQUENCING_CENTER = "SC";
    
    @Option(shortName="E2", doc="Including second base call or not, default false", optional=true)
    public boolean GENERATE_SECONDARY_BASE_CALLS = false;

    @Option(shortName="PF", doc="Filter cluster or not, default true", optional=true)
    public boolean PF_FILTER = true;


    @Override
    protected int doWork() {

        IoUtil.assertFileIsWritable(OUTPUT);

        Lane lane = new Lane(this.INTENSITY_DIR, this.BASECALLS_DIR, this.LANE, this.GENERATE_SECONDARY_BASE_CALLS, this.PF_FILTER, OUTPUT);

        try {
            lane.readConfigs();
        } catch (Exception ex) {
            Logger.getLogger(Illumina2bam.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }

        SAMFileWriter outBam = lane.generateOutputSamStream();
        try {
            lane.processTiles(outBam);
        } catch (Exception ex) {
            Logger.getLogger(Illumina2bam.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }

        outBam.close();

        return 0;
    }

    /** Stock main method. */
    public static void main(final String[] args) {
        System.exit(new Illumina2bam().instanceMain(args));
    }
}
