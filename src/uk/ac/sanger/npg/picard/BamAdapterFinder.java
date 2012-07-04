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
package uk.ac.sanger.npg.picard;

import java.io.File;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.SequenceUtil;

/**
 * Class to read matching forward/reverse BAM records and identify
 * short inserts by looking for overlap between forward read and
 * reverse-complemented reverse read. Note the offset of the overlap
 * with a new tag in the output BAM file. The records for a given read
 * pair are expected to be consecutive in the BAM file.
 *
 * @author Tom Skelly
 */
public class BamAdapterFinder extends PicardCommandLine {

    private final Log log = Log.getInstance(BamAdapterFinder.class);

    private final String programName = "BamAdapterFinder";
    private final String programDS = "Find short inserts by finding overlapping forward/reverse reads. Note position with a tag.";
    @Usage(programVersion = version)
    public final String USAGE = this.getStandardUsagePreamble() + this.programDS + ". ";

    @Option(shortName = StandardOptionDefinitions.INPUT_SHORT_NAME, doc = "The input SAM or BAM file to trim.")
    public File INPUT;
    @Option(shortName = StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc = "The output file after trimming.")
    public File OUTPUT;
    @Option(shortName = "XS", doc = "Tag to be used for adapter length.", optional = true)
    public String ADAPTER_LENGTH_TAG = "xs";
    @Option(shortName = "XM", doc = "Tag to be used for adapter match check (boolean).", optional = true)
    public String ADAPTER_MATCH_TAG = "xm";
    @Option(shortName = "MO", doc = "Minimum read overlap to look for.", optional = true)
    public int MIN_OVERLAP = 32;
    @Option(shortName = "PM", doc = "Maximum number of mismatches allowed in overlap.", optional = true)
    public double PCT_MISMATCHES = 10.0;
    @Option(shortName = "AM", doc = "Number of adapter bases that need to match.", optional = true)
    public int ADAPTER_MATCH = 12;

    private static final int NONE_FOUND = -1;     // "no match found" return from matchSAMRecords
    private static final int ARRAY_SIZE = 10000;

    @Override
    protected int doWork() {

        this.log.info("Checking input and output file");
        IoUtil.assertFileIsReadable(INPUT);
        IoUtil.assertFileIsWritable(OUTPUT);

        this.log.info("Open input file: " + INPUT.getName());
        final SAMFileReader in = new SAMFileReader(INPUT);

        final SAMFileHeader header = in.getFileHeader();
        final SAMFileHeader outputHeader = header.clone();
        this.addProgramRecordToHead(outputHeader, this.getThisProgramRecord(programName, programDS));

        this.log.info("Open output file with header: " + OUTPUT.getName());
        final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader, true, OUTPUT);

        long totPairs = 0;
        long[] overlaps = new long[ARRAY_SIZE];    // indexed by offset, sized to be far larger
        long[] adapters = new long[ARRAY_SIZE];

        boolean first = true;
        SAMRecord record_1 = null;

        this.log.info("Reading records in pairs");
        for (SAMRecord record : in) {

            if (first) {
                ++totPairs;
                record_1 = record;
                first = false;
            } else {
                
                int offset = matchSAMRecords(record_1, record);
                if (offset != NONE_FOUND) {

                    ++overlaps[offset];

                    record_1.setAttribute(ADAPTER_LENGTH_TAG, offset);
                    record.setAttribute(ADAPTER_LENGTH_TAG, offset);

                    if (checkAdapter(record_1, record, offset)) {
                        ++adapters[offset];
                        record_1.setAttribute(ADAPTER_MATCH_TAG, 1);
                        record.setAttribute(ADAPTER_MATCH_TAG, 1);
                    }

                }

                out.addAlignment(record_1);
                out.addAlignment(record);

                first = true;

            }

        }

        out.close();

        if (totPairs == 0) {                            // avoids zero-divide later on
            this.log.info("ERROR: input file was empty.");
        } else {
        
            this.log.info("Adapter processing finished, annotated file: " + this.OUTPUT);

            int totOverlaps = 0;
            int totAdapters = 0;

            for (int ix=0; ix<ARRAY_SIZE; ++ix) {
                if (overlaps[ix] > 0) {                 // overlaps==0 implies adapters==0
                    this.log.info(String.format("%3d  %7d  %7d", ix, overlaps[ix], adapters[ix]));
                    totOverlaps += overlaps[ix];
                    totAdapters += adapters[ix];
                }
            }

            double pctOverlaps = (double)totOverlaps/(double)totPairs*100.0;
            double pctAdapters = (double)totAdapters/(double)totPairs*100.0;

            this.log.info(String.format("Processed %7d read pairs", totPairs));
            this.log.info(String.format("Found     %7d overlaps (%.1f%%)", totOverlaps, pctOverlaps));
            this.log.info(String.format("Found     %7d adapters (%.1f%%)", totAdapters, pctAdapters));

        }
        
        return 0;

    }

    public int matchSAMRecords(final SAMRecord record_1, final SAMRecord record_2) {

        /* Given a matching pair of reads, find cases where there is
           an overlap of at least N bases between read 1 and the
           reverse- complement of read 2.  This indicates that a short
           insert was completely sequenced in both directions, and
           sequencing then continued into the adapter -- like so
           (bottom line is r.c.read 2):
                                                                            offset=10
                                                                            |
                                                                            V
           TTAATGTCTTATGATGTTGTGTGCCTGCTGGCATTTGTTAAACAAAATCATTGATTAACAATCATAGATCGGAAG
 CTTCCGATCTTTAATGTCTTATGATGTTGTGTGCCTGCTGGCATTTGTTAAACAAAATCATTGATTAACAATCAT

           Note that the returned offset is equivalent to the length
           of the adapter sequence. I.e., it's counted backwards from
           the end of the read.

        */

        String name_1 = record_1.getReadName();
        String name_2 = record_2.getReadName();

        if (name_1.compareTo(name_2) != 0) {
            throw new RuntimeException("Read " + name_1 + " and " + name_2 + " do not match");
        }

        if ( ! record_1.getFirstOfPairFlag()) {            // method also checks that read is paired
            throw new RuntimeException("Read " + name_1 + " is not first read of pair");
        }
        if ( ! record_2.getSecondOfPairFlag()) {
            throw new RuntimeException("Read " + name_2 + " is not second read of pair");
        }

        /* We want to compare the forward sense of the forward read with the
         * reverse-complement of the reverse read. Depending on how they are
         * stored in the BAM, we may have to r.c. either, both, or neither of
         * them.
         */

        byte[] bases_1 = (byte[])record_1.getReadBases().clone();    // clone to preserve original
        if (record_1.getReadNegativeStrandFlag()) {
            SequenceUtil.reverseComplement(bases_1);
        }

        byte[] saseb_2 = (byte[])record_2.getReadBases().clone();    // 'saseb' == bases, backwards
        if ( ! record_2.getReadNegativeStrandFlag()) {    // if it's already r.c., leave it alone
            SequenceUtil.reverseComplement(saseb_2);
        }

        final int readLength = bases_1.length;
        int ret = NONE_FOUND;

        for (int offset=1;              // slide the reads against each other
             offset<=readLength-MIN_OVERLAP && ret==NONE_FOUND;
             ++offset) {

            int overlapSize = readLength - offset;
            int allowedMismatches = (int)((double)overlapSize*PCT_MISMATCHES/100.0);
            int mismatches = 0;

            for (int ix=0;              // check each position for match
                 ix<overlapSize && mismatches<=allowedMismatches;
                 ++ix) {

                if (bases_1[ix] != saseb_2[offset+ix]) {
                    ++mismatches;
                }

            }

            if (mismatches <= allowedMismatches) {
                ret = offset;          // breaks slider loop
            }

        }

       return ret;

    }

    public boolean checkAdapter (final SAMRecord record_1, final SAMRecord record_2, final int offset) {

        /* We think there is an adapter 'offset' bases from the end of the read.
         * Check that the first ADAPTER_MATCH bases of both presumed adapters
         * match. (That is true for the paired-end adapters in use today, for
         * ADAPTER_MATCH<=13. YMMV.)
         */

        /* Here it's different: We want to process the forward sense of both
         * reads, so that the adapter is at the far end of the read. If either
         * read has been stored reversed, un-reverse it.
         */

        byte[] bases_1 = (byte[])record_1.getReadBases().clone();    // clone to preserve original
        if (record_1.getReadNegativeStrandFlag()) {
            SequenceUtil.reverseComplement(bases_1);
        }

        byte[] bases_2 = (byte[])record_2.getReadBases().clone();
        if (record_2.getReadNegativeStrandFlag()) {
            SequenceUtil.reverseComplement(bases_2);
        }

        final int readLength = bases_1.length;
        final int start = readLength - offset;
        final int stop = Math.min(readLength, start+ADAPTER_MATCH);

        boolean ret = true;

        for (int ix=start; ix<stop; ++ix) {
            if (bases_1[ix] != bases_2[ix]) {
                ret = false;
                break;
            }

        }

        return ret;

    }

    /**
     * example: INPUT=testdata/bam/6210_8.sam OUTPUT=testdata/6210_8_findadapters.bam
     * ADAPTER_LENGTH_TAG=ms ADAPTER_MATCH_TAG=xm MIN_OVERLAP=32 PCT_MISMATCHES=10.0
     * ADAPTER_MATCH=12
     * VALIDATION_STRINGENCY=SILENT
     *
     * @param args
     */
    public static void main(final String[] args) {
        System.exit(new BamAdapterFinder().instanceMain(args));
    }

}
