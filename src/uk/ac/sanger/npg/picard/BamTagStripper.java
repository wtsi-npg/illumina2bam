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
package uk.ac.sanger.npg.picard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.SAMTagAndValue;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class BamTagStripper extends PicardCommandLine {
    
    private final Log log = Log.getInstance(BamTagStripper.class);
    
    private final String programName = "BamTagStripper";
    
    private final String programDS = "Strip a list of tags in bam/sam record. "
            + "By default, any tags containing lowercase letters will be stripped and other tags will be kept. "
            + "A list of tags can be given to keep or strip";
   
    @Usage(programVersion= version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". "; 
 
    
    @Option(shortName= StandardOptionDefinitions.INPUT_SHORT_NAME, doc="The input SAM or BAM file to strip.")
    public File INPUT;
    
    @Option(shortName=StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc="The stripped ouput SAM or BAM file.")
    public File OUTPUT;

    @Option(shortName="KEEP", doc= "A list of tag containing lowercase letters to keep.")
    public final List<String> TAG_TO_KEEP = new ArrayList<String>();
    
    @Option(shortName="STRIP", doc= "A list of tag only containing uppercase letters to strip.")
    public final List<String> TAG_TO_STRIP = new ArrayList<String>();

    @Override
    protected int doWork() {

        log.info("Checking input and output files");
        IoUtil.assertFileIsReadable(this.INPUT);
        IoUtil.assertFileIsWritable(this.OUTPUT);
        
        log.info( "Open input file: " + INPUT.getName() );
        final SAMFileReader in  = new SAMFileReader(INPUT);
        
        final SAMFileHeader header = in.getFileHeader();
        final SAMFileHeader outputHeader = header.clone();
        this.addProgramRecordToHead(outputHeader, this.getThisProgramRecord(programName, programDS));
        
        log.info("Open output file with header: " + OUTPUT.getName());
        final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader,  true, OUTPUT);

        final HashMap<String, Boolean> tagToKeepHash = new HashMap<String,Boolean>();
        for(final String tag : this.TAG_TO_KEEP){
            tagToKeepHash.put(tag, true);
        }
        
        final HashMap<String,Boolean> tagToRemoveHash = new HashMap<String,Boolean>();
        for(final String tag : this.TAG_TO_STRIP){
            tagToRemoveHash.put(tag, true);
        }

        final Pattern upperCasePattern = Pattern.compile("^[A-Z][A-z0-9]$");
        
        final HashSet<String> strippedTagList = new HashSet<String>();
        
        log.info("Start to strip tags");
        for (final SAMRecord record : in) {
                List<SAMTagAndValue> tagList = record.getAttributes();
                for(final SAMTagAndValue tagAndValue : tagList){
                    String tag = tagAndValue.tag;
                    if( ( !upperCasePattern.matcher(tag).find() && tagToKeepHash.get(tag) == null )
                       || tagToRemoveHash.get(tag) != null
                      ){
                        record.setAttribute(tag, null);
                        strippedTagList.add(tag);
                    }
                }
                out.addAlignment(record);
        }
        
        out.close();
        log.info("Stripped tag list: " + strippedTagList.toString());
        log.info("Stripping finished, stripped file: " + this.OUTPUT);
        
        return 0;
    }
    
    /**
     * 
     * @param args
     */
    public static void main(final String[] args) {

        System.exit(new BamTagStripper().instanceMain(args));
    }
}
