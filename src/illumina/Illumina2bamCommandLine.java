
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

import java.util.List;
import net.sf.picard.cmdline.CommandLineProgram;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;

/**
 *
 * @author Guoying Qi
 */
public abstract class Illumina2bamCommandLine extends CommandLineProgram {
    
    public static final String version = "0.02";
    
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
        programRecord.setProgramVersion(this.getProgramVersion());
        programRecord.setAttribute("DS", programDS);
   
        return programRecord;
    }
    
    /**
     * 
     * @param header
     * @param programRecord 
     */
    public void addProgramRecordToHead(SAMFileHeader header, SAMProgramRecord programRecord){
        //TODO: check the new program ID does not exist in the old list
        List<SAMProgramRecord> programList = header.getProgramRecords();
        if(programList != null && ! programList.isEmpty() ){
            String previousProgramId =  programList.get(programList.size() - 1 ).getProgramGroupId();
            programRecord.setPreviousProgramGroupId(previousProgramId);
        }        
        header.addProgramRecord(programRecord);
    }
    
}
