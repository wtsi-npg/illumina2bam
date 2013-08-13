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

import java.util.ArrayList;
import java.util.List;
import net.sf.picard.cmdline.Usage;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author gq1@sanger.ac.uk
 */
public class PicardCommandLineTest {
    

    /**
     * Test of getThisProgramRecord method, of class PicardCommandLine.
     */
    @Test
    public void testGetThisProgramRecord() {
        System.out.println("getThisProgramRecord");
        String programName = "TestProgram";
        String programDS = "This is a test program";
        PicardCommandLine instance = new PicardCommandLineImpl();
        String [] args = {};
        instance.instanceMain(args);
        SAMProgramRecord result = instance.getThisProgramRecord(programName, programDS);
        assertEquals(result.getProgramGroupId(), programName);
        assertEquals(result.getProgramName(), programName);
        assertEquals(result.getProgramVersion(), PicardCommandLine.version);
        assertEquals(result.getAttribute("DS"), programDS);
    }

    /**
     * Test of addProgramRecordToHead method, of class PicardCommandLine.
     */
    @Test
    public void testAddProgramRecordToHead() {
        System.out.println("addProgramRecordToHead");
        SAMFileHeader header = new SAMFileHeader();
        SAMProgramRecord programRecord = new SAMProgramRecord("test");
        PicardCommandLine instance = new PicardCommandLineImpl();
        instance.addProgramRecordToHead(header, programRecord);
        assertEquals(header.getProgramRecords().size(), 1);
        instance.addProgramRecordToHead(header, programRecord);
        assertEquals(header.getProgramRecords().size(), 2);
        assertNotNull(header.getProgramRecord("test_1"));
    }

    /**
     * Test of makeUniqueProgramId method, of class PicardCommandLine.
     */
    @Test
    public void testMakeUniqueProgramId() {
        System.out.println("makeUniqueProgramId");
        
        List<SAMProgramRecord> programList = new ArrayList<SAMProgramRecord>();
        SAMProgramRecord programRecord = new SAMProgramRecord("test");
        programList.add(programRecord);
        
        SAMProgramRecord programRecord1 = new SAMProgramRecord("test");
        
        PicardCommandLine instance = new PicardCommandLineImpl();

        SAMProgramRecord result = instance.makeUniqueProgramId(programList, programRecord1);
        assertEquals(result.getProgramGroupId(), "test_1");

    }

    public class PicardCommandLineImpl extends PicardCommandLine {

        @Usage(programVersion = version)
        public final String USAGE = this.getStandardUsagePreamble() + " test . ";

        @Override
        protected int doWork() {
            return 0;
        }
    }
}
