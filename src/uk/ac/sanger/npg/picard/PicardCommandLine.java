
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

import java.util.HashMap;
import java.util.List;
import net.sf.picard.cmdline.CommandLineProgram;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;

/**
 * This is the base class for any command line program class
 * 
 * @author gq1@sanger.ac.uk
 */
public abstract class PicardCommandLine extends CommandLineProgram {
    
    public static final String version = "1.16";
    
    /**
     * Generate Program Record for this program itself
     * @param programName
     * @param programDS 
     * @return this program itself as Program Record
     */
    
    public SAMProgramRecord getThisProgramRecord(String programName, String programDS){        
        
        SAMProgramRecord programRecord = new SAMProgramRecord(programName);
        
        programRecord.setProgramName(programName);
        programRecord.setCommandLine(this.getCommandLine());
        // use ImplementationVersion from manifest by preference, fall back to class's version 
        String programVersion = this.getCommandLineParser().getVersion();
        if(programVersion == null || programVersion.length() == 0){
            programVersion = this.getProgramVersion();
        }
        programRecord.setProgramVersion(programVersion);
        programRecord.setAttribute("DS", programDS);
   
        return programRecord;
    }
    
    /**
     * 
     * @param header
     * @param programRecord 
     * @return programRecord with Id
     */
    public SAMProgramRecord addProgramRecordToHead(SAMFileHeader header, SAMProgramRecord programRecord){
        //TODO: check the new program ID does not exist in the old list
        List<SAMProgramRecord> programList = header.getProgramRecords();
        if(programList != null && ! programList.isEmpty() ){
            String previousProgramId =  programList.get(programList.size() - 1 ).getProgramGroupId();
            programRecord.setPreviousProgramGroupId(previousProgramId);
        }
        final SAMProgramRecord newProgramRecord = this.makeUniqueProgramId(programList, programRecord);
        header.addProgramRecord(newProgramRecord);
        return newProgramRecord;
    }
    
    public SAMProgramRecord makeUniqueProgramId(List<SAMProgramRecord> programList,  SAMProgramRecord programRecord){
        
        HashMap<String, Integer> programIdList = new HashMap<String, Integer>();
        for(SAMProgramRecord program : programList){
            programIdList.put(program.getProgramGroupId(), 1);
        }
        
        String programId = programRecord.getProgramGroupId();
        String newProgramId = programId;
        int count = 1;
        while(programIdList.get(newProgramId) != null ){
            newProgramId = programId + "_" + count;
            count++;
        }
        if(newProgramId.equalsIgnoreCase(programId)){
            return programRecord;
        }
        return new SAMProgramRecord(newProgramId, programRecord);
    }
    
}
