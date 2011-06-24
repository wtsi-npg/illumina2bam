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

import net.sf.picard.util.TabbedTextFileWithHeaderParser;
import net.sf.picard.metrics.MetricBase;
import net.sf.picard.metrics.MetricsFile;
import net.sf.samtools.util.SequenceUtil;
import net.sf.samtools.util.StringUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.picard.illumina.parser.IlluminaReadData;
import net.sf.picard.util.Log;

/**
 * This class was separated from Picard ExtractIlluminaBarcodes class,
 * which originally only decode the reads in qseq files.
 * 
 * In Sanger we store barcode reads in BAM file as RT tag.
 * Using this separated class, we can decode BAM file using the same mechanism.
 * 
 * For qseq file decoding:
 * 
 * Determine the bar code for each read in an Illumina lane.
 * For each tile, a file is written to the basecalls directory of the form s_<lane>_<tile>_barcode.txt.
 * An output file contains a line for each read in the tile, aligned with the regular basecall output
 * The output file contains the following tab-separated columns:
 * - read subsequence at bar code position
 * - Y or N indicating if there was a bar code match
 * - matched bar code sequence (empty if read did not match one of the barcodes
 * - number of matches
 * - number of mismatches).
 * 
 * For bam file decoding:
 * 
 * Each read in BAM file will be marked in its read name and read group,
 * There is an option to output bam file by tag.
 *
 * @author alecw@broadinstitute.org
 * @author gq1@sanger.ac.uk
 */

public class IndexDecoder {

    private final Log log = Log.getInstance(ExtractIlluminaBarcodes.class);
    
    private int maxMismatches = 1;
    private int minMismatchDelta = 1;
    private int maxNoCalls = 2;
    
    private File inputBarcodeFile;
    private List<String> barcodeStringList;
    
    private int barcodeLength;
    private List<NamedBarcode> namedBarcodes = new ArrayList<NamedBarcode>();
    
    private List<BarcodeMetric> barcodeMetrics = new ArrayList<BarcodeMetric>();
    private BarcodeMetric noMatchBarcodeMetric; 

    /**
     * default constructor
     */
    public IndexDecoder(){
       
    }
    
    /**
     * Constructor from a file with bar code list
     * 
     * @param inputBarcodeFile 
     */
    public IndexDecoder(File inputBarcodeFile){
        this.inputBarcodeFile = inputBarcodeFile;        
    }

    /**
     * constructor from a list of barcodes
     * 
     * @param barcodeStringList 
     */
    public IndexDecoder(List<String> barcodeStringList){
        this.barcodeStringList = barcodeStringList;        
    }

    /**
     * check input bar codes and initial bar codes object and metrics list
     * 
     * @param messages 
     */
    public void prepareDecode(ArrayList<String> messages){

        if(this.inputBarcodeFile != null ){
            log.info("Barcode list file: " + inputBarcodeFile.getAbsolutePath());
            this.namedBarcodes = this.parseBarcodeFile(messages, inputBarcodeFile);
        }else if(this.barcodeStringList != null){        
            log.info("The barcode list file is not given and a list of barcodes from command will be used");
            this.namedBarcodes = this.parseBarcodeString(messages, this.barcodeStringList);
        }
        
        if (getNamedBarcodes().isEmpty()) {
            messages.add("No barcodes have been specified.");
            return;
        }
        
        this.barcodeLength = getNamedBarcodes().get(0).barcode.length();

        for (final NamedBarcode namedBarcode : getNamedBarcodes()) {
            barcodeMetrics.add(new BarcodeMetric(namedBarcode));
        }

        // Create BarcodeMetric for counting reads that don't match any barcode
        final StringBuilder noMatchBarcode = new StringBuilder(getBarcodeLength());
        for (int i = 0; i < getBarcodeLength(); ++i) {
            noMatchBarcode.append('N');
        }

        noMatchBarcodeMetric = new BarcodeMetric(new NamedBarcode(noMatchBarcode.toString()));
    }

    public BarcodeMatch extractBarcode(String barcodeRead, boolean isPf){
        final BarcodeMatch match = findBestBarcode(barcodeRead, isPf);
        return match;
    } 

    /**
     * Assign barcodes for a single tile's qseq file
     * @param ird
     * @param writer
     * @throws IOException 
     */
    public void extractBarcode(final IlluminaReadData ird, BufferedWriter writer ) throws IOException {
        final String barcodeSubsequence = StringUtil.bytesToString(ird.getBarcodeRead().getBases());
        final boolean passingFilter = ird.isPf();
        final BarcodeMatch match = findBestBarcode(barcodeSubsequence, passingFilter);

        final String yOrN = (match.matched ? "Y" : "N");
       
        writer.write(StringUtil.join("\t", barcodeSubsequence, yOrN, match.barcode,
                                     String.valueOf(match.mismatches),
                                     String.valueOf(match.mismatchesToSecondBest)));
        writer.newLine();
    }

    /**
     * 
     * @param metrics
     * @param metricsFile 
     */
    public void writeMetrics(final MetricsFile<BarcodeMetric, Integer> metrics, File metricsFile) {

        // Finish metrics tallying.
        int totalReads = noMatchBarcodeMetric.READS;
        int totalPfReads = noMatchBarcodeMetric.PF_READS;
        int totalPfReadsAssigned = 0;
        for (final BarcodeMetric barcodeMetric : barcodeMetrics) {
            totalReads += barcodeMetric.READS;
            totalPfReads += barcodeMetric.PF_READS;
            totalPfReadsAssigned += barcodeMetric.PF_READS;
        }

        if (totalReads > 0) {
            noMatchBarcodeMetric.PCT_MATCHES = noMatchBarcodeMetric.READS/(double)totalReads;
            double bestPctOfAllBarcodeMatches = 0;
            for (final BarcodeMetric barcodeMetric : barcodeMetrics) {
                barcodeMetric.PCT_MATCHES = barcodeMetric.READS/(double)totalReads;
                if (barcodeMetric.PCT_MATCHES > bestPctOfAllBarcodeMatches) {
                    bestPctOfAllBarcodeMatches = barcodeMetric.PCT_MATCHES;
                }
            }
            if (bestPctOfAllBarcodeMatches > 0) {
                noMatchBarcodeMetric.RATIO_THIS_BARCODE_TO_BEST_BARCODE_PCT =
                        noMatchBarcodeMetric.PCT_MATCHES/bestPctOfAllBarcodeMatches;
                for (final BarcodeMetric barcodeMetric : barcodeMetrics) {
                    barcodeMetric.RATIO_THIS_BARCODE_TO_BEST_BARCODE_PCT =
                            barcodeMetric.PCT_MATCHES/bestPctOfAllBarcodeMatches;
                }
            }
        }

        if (totalPfReads > 0) {
            noMatchBarcodeMetric.PF_PCT_MATCHES = noMatchBarcodeMetric.PF_READS/(double)totalPfReads;
            double bestPctOfAllBarcodeMatches = 0;
            for (final BarcodeMetric barcodeMetric : barcodeMetrics) {
                barcodeMetric.PF_PCT_MATCHES = barcodeMetric.PF_READS/(double)totalPfReads;
                if (barcodeMetric.PF_PCT_MATCHES > bestPctOfAllBarcodeMatches) {
                    bestPctOfAllBarcodeMatches = barcodeMetric.PF_PCT_MATCHES;
                }
            }
            if (bestPctOfAllBarcodeMatches > 0) {
                noMatchBarcodeMetric.PF_RATIO_THIS_BARCODE_TO_BEST_BARCODE_PCT =
                        noMatchBarcodeMetric.PF_PCT_MATCHES/bestPctOfAllBarcodeMatches;
                for (final BarcodeMetric barcodeMetric : barcodeMetrics) {
                    barcodeMetric.PF_RATIO_THIS_BARCODE_TO_BEST_BARCODE_PCT =
                            barcodeMetric.PF_PCT_MATCHES/bestPctOfAllBarcodeMatches;
                }
            }
        }

        // Calculate the normalized matches
        if (totalPfReadsAssigned > 0) {
            final double mean = (double) totalPfReadsAssigned / (double) barcodeMetrics.size();
            for (final BarcodeMetric m : barcodeMetrics) {
                m.PF_NORMALIZED_MATCHES = m.PF_READS  / mean;
            }
        }

        for (final BarcodeMetric barcodeMetric : barcodeMetrics) {
            metrics.addMetric(barcodeMetric);
        }
        metrics.addMetric(noMatchBarcodeMetric);

        metrics.write(metricsFile);

    }

    /**
     * Find the best barcode match for the given read sequence, and accumulate metrics
     * @param readSubsequence portion of read containing barcode
     * @param passingFilter PF flag for the current read
     * @return perfect barcode string, if there was a match within tolerance, or null if not.
     */
    private BarcodeMatch findBestBarcode(final String readSubsequence, final boolean passingFilter) {
        BarcodeMetric bestBarcodeMetric = null;
        int numMismatchesInBestBarcode = readSubsequence.length();
        int numMismatchesInSecondBestBarcode = readSubsequence.length();

        final byte[] readBytes = net.sf.samtools.util.StringUtil.stringToBytes(readSubsequence);
        int numNoCalls = 0;
        for (final byte b : readBytes) if (SequenceUtil.isNoCall(b)) ++numNoCalls;


        for (final BarcodeMetric barcodeMetric : barcodeMetrics) {
            
            final int numMismatches = countMismatches(barcodeMetric.barcodeBytes, readBytes);
            if (numMismatches < numMismatchesInBestBarcode) {
                if (bestBarcodeMetric != null) {
                    numMismatchesInSecondBestBarcode = numMismatchesInBestBarcode;
                }
                numMismatchesInBestBarcode = numMismatches;
                bestBarcodeMetric = barcodeMetric;
            } else if (numMismatches < numMismatchesInSecondBestBarcode) {
                numMismatchesInSecondBestBarcode = numMismatches;
            }
        }

        final boolean matched = bestBarcodeMetric != null &&
                numNoCalls <= this.maxNoCalls &&
                numMismatchesInBestBarcode <= this.maxMismatches &&
                numMismatchesInSecondBestBarcode - numMismatchesInBestBarcode >= this.minMismatchDelta;

        final BarcodeMatch match = new BarcodeMatch();

        if (numNoCalls + numMismatchesInBestBarcode < readSubsequence.length()) {
            match.mismatches = numMismatchesInBestBarcode;
            match.mismatchesToSecondBest = numMismatchesInSecondBestBarcode;
            match.barcode = bestBarcodeMetric.BARCODE.toLowerCase();
        }
        else {
            match.mismatches = readSubsequence.length();
            match.mismatchesToSecondBest = readSubsequence.length();
            match.barcode = "";
        }

        if (matched) {
            ++bestBarcodeMetric.READS;
            if (passingFilter) {
                ++bestBarcodeMetric.PF_READS;
            }
            if (numMismatchesInBestBarcode == 0) {
                ++bestBarcodeMetric.PERFECT_MATCHES;
                if (passingFilter) {
                    ++bestBarcodeMetric.PF_PERFECT_MATCHES;
                }
            } else if (numMismatchesInBestBarcode == 1) {
                ++bestBarcodeMetric.ONE_MISMATCH_MATCHES;
                if (passingFilter) {
                    ++bestBarcodeMetric.PF_ONE_MISMATCH_MATCHES;
                }
            }

            match.matched = true;
            match.barcode = bestBarcodeMetric.BARCODE;
        }
        else {
            ++noMatchBarcodeMetric.READS;
            if (passingFilter) {
                ++noMatchBarcodeMetric.PF_READS;
            }
        
        }
        return match;
    }
    
    /**
     * Compare barcode sequence to bases from read
     * @return how many bases did not match
     */
    private int countMismatches(final byte[] barcodeBytes, final byte[] readSubsequence) {
        int numMismatches = 0;
        for (int i = 0; i < barcodeBytes.length; ++i) {
            if (!SequenceUtil.isNoCall(readSubsequence[i]) && !SequenceUtil.basesEqual(barcodeBytes[i], readSubsequence[i])) {
                ++numMismatches;
            }
        }
        return numMismatches;
    }
   
    /**
     * check the list of input bar codes.
     * 
     * @param messages
     * @param barcodeStringList
     * @return 
     */
    public ArrayList<NamedBarcode> parseBarcodeString(ArrayList<String> messages, List<String> barcodeStringList) {
        
        ArrayList<NamedBarcode> namedBarcodeList = new ArrayList<NamedBarcode>();
        
        Set<String> barcodes = new HashSet<String>();
        
        int barcodeLengthLocal = barcodeStringList.get(0).length();
        
        for (final String barcode : barcodeStringList) {
      
            if (barcode.length() != barcodeLengthLocal) {
                messages.add("Barcode " + barcode + " has different length from first barcode.");
            }
            if (barcodes.contains(barcode)) {
                messages.add("Barcode " + barcode + " specified more than once.");
            }
            barcodes.add(barcode);
            NamedBarcode namedBarcode = new NamedBarcode(barcode);
            namedBarcodeList.add(namedBarcode);
        }
        return namedBarcodeList;
    }


    private static final String BARCODE_SEQUENCE_COLUMN = "barcode_sequence";
    private static final String BARCODE_NAME_COLUMN = "barcode_name";
    private static final String LIBRARY_NAME_COLUMN = "library_name";
    private static final String SAMPLE_NAME_COLUMN = "sample_name";
    private static final String DESCRIPTION_COLUMN = "description";
    
    /**
     * read and check input bar code file
     * 
     * @param messages
     * @param barcodeFile
     * @return 
     */
    
    public ArrayList<NamedBarcode> parseBarcodeFile(ArrayList<String> messages, File barcodeFile) {
    
        final TabbedTextFileWithHeaderParser barcodesParser = new TabbedTextFileWithHeaderParser(barcodeFile);
        if (!barcodesParser.hasColumn(BARCODE_SEQUENCE_COLUMN)) {
            messages.add(barcodeFile + " does not have " + BARCODE_SEQUENCE_COLUMN + " column header");
            return null;
        }
        
        boolean hasBarcodeName = barcodesParser.hasColumn(BARCODE_NAME_COLUMN);
        boolean hasLibraryName = barcodesParser.hasColumn(LIBRARY_NAME_COLUMN);
        boolean hasSampleName = barcodesParser.hasColumn(SAMPLE_NAME_COLUMN);
        boolean hasDescription = barcodesParser.hasColumn(DESCRIPTION_COLUMN);
        
        int barcodeLengthLocal = 0;
        Set<String> barcodes = new HashSet<String>();
        ArrayList<NamedBarcode> namedBarcodeList = new ArrayList<NamedBarcode>();
        
        for (final TabbedTextFileWithHeaderParser.Row row : barcodesParser) {
            final String barcode = row.getField(BARCODE_SEQUENCE_COLUMN);
            if (barcodeLengthLocal == 0) barcodeLengthLocal = barcode.length();
            if (barcode.length() != barcodeLengthLocal) {
                messages.add("Barcode " + barcode + " has different length from first barcode.");
            }
            if (barcodes.contains(barcode)) {
                messages.add("Barcode " + barcode + " specified more than once in " + barcodeFile );
            }
            barcodes.add(barcode);
            final String barcodeName = (hasBarcodeName? row.getField(BARCODE_NAME_COLUMN): "");
            final String libraryName = (hasLibraryName? row.getField(LIBRARY_NAME_COLUMN): "");
            final String sampleName = (hasSampleName? row.getField(SAMPLE_NAME_COLUMN): "");
            final String description = (hasDescription? row.getField(DESCRIPTION_COLUMN): "");
            
            NamedBarcode namedBarcode = new NamedBarcode(barcode, barcodeName, libraryName, sampleName, description);
            namedBarcodeList.add(namedBarcode);
        }

        return namedBarcodeList;
    }

    /**
     * @param maxMismatches the maxMismatches to set
     */
    public void setMaxMismatches(int maxMismatches) {
        this.maxMismatches = maxMismatches;
    }

    /**
     * @param minMismatchDelta the minMismatchDelta to set
     */
    public void setMinMismatchDelta(int minMismatchDelta) {
        this.minMismatchDelta = minMismatchDelta;
    }

    /**
     * @param maxNoCalls the maxNoCalls to set
     */
    public void setMaxNoCalls(int maxNoCalls) {
        this.maxNoCalls = maxNoCalls;
    }

    /**
     * @return the barcodeLength
     */
    public int getBarcodeLength() {
        return this.barcodeLength;
    }

    /**
     * @return the namedBarcodes
     */
    public List<NamedBarcode> getNamedBarcodes() {
        return namedBarcodes;
    }
  
    /**
     * BarcodeMatch class
     */
    public static class BarcodeMatch {
        boolean matched;
        String barcode;
        int mismatches;
        int mismatchesToSecondBest;
    }

    /**
     * Barcode Class
     */
 
    public static class NamedBarcode {
        public final String barcode;
        public final String barcodeName;
        public final String libraryName;
        public final String sampleName;
        public final String description;

        public NamedBarcode(String barcode, String barcodeName, String libraryName, String sampleName, String description) {
            this.barcode = barcode;
            this.barcodeName = barcodeName;
            this.libraryName = libraryName;
            this.sampleName = sampleName;
            this.description = description;
        }

        public NamedBarcode(String barcode) {
            this.barcode = barcode;
            this.barcodeName = "";
            this.libraryName = "";
            this.sampleName  = "";
            this.description = "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NamedBarcode that = (NamedBarcode) o;

            if (barcode != null ? !barcode.equals(that.barcode) : that.barcode != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return barcode != null ? barcode.hashCode() : 0;
        }
    }

    /**
     * Metrics produced by the ExtractIlluminaBarcodes program that is used to parse data in
     * the basecalls directory and determine to which barcode each read should be assigned.
     */
    public static class BarcodeMetric extends MetricBase {
        /**
         * The barcode (from the set of expected barcodes) for which the following metrics apply.
         * Note that the "symbolic" barcode of NNNNNN is used to report metrics for all reads that
         * do not match a barcode.
         */
        public String BARCODE;
        public String BARCODE_NAME = "";
        public String LIBRARY_NAME = "";
        public String SAMPLE_NAME = "";
        public String DESCRIPTION = "";
        
        /** The total number of reads matching the barcode. */
        public int READS = 0;
        /** The number of PF reads matching this barcode (always less than or equal to READS). */
        public int PF_READS = 0;
        /** The number of all reads matching this barcode that matched with 0 errors or no-calls. */
        public int PERFECT_MATCHES = 0;
        /** The number of PF reads matching this barcode that matched with 0 errors or no-calls. */
        public int PF_PERFECT_MATCHES = 0;
        /** The number of all reads matching this barcode that matched with 1 error or no-call. */
        public int ONE_MISMATCH_MATCHES = 0;
        /** The number of PF reads matching this barcode that matched with 1 error or no-call. */
        public int PF_ONE_MISMATCH_MATCHES = 0;
        /** The percentage of all reads in the lane that matched to this barcode. */
        public double PCT_MATCHES = 0d;
        /**
         * The rate of all reads matching this barcode to all reads matching the most prevelant barcode. For the
         * most prevelant barcode this will be 1, for all others it will be less than 1.  One over the lowest
         * number in this column gives you the fold-difference in representation between barcodes.
         */
        public double RATIO_THIS_BARCODE_TO_BEST_BARCODE_PCT = 0d;
        /** The percentage of PF reads in the lane that matched to this barcode. */
        public double PF_PCT_MATCHES = 0d;

        /**
         * The rate of PF reads matching this barcode to PF reads matching the most prevelant barcode. For the
         * most prevelant barcode this will be 1, for all others it will be less than 1.  One over the lowest
         * number in this column gives you the fold-difference in representation of PF reads between barcodes.
         */
        public double PF_RATIO_THIS_BARCODE_TO_BEST_BARCODE_PCT = 0d;

        /**
         * The "normalized" matches to each barcode. This is calculated as the number of pf reads matching
         * this barcode over the sum of all pf reads matching any barcode (excluding orphans). If all barcodes
         * are represented equally this will be 1.
         */
        public double PF_NORMALIZED_MATCHES;

        protected final byte[] barcodeBytes;

        public BarcodeMetric(final NamedBarcode namedBarcode) {
            this.BARCODE = namedBarcode.barcode;
            this.BARCODE_NAME = namedBarcode.barcodeName;
            this.LIBRARY_NAME = namedBarcode.libraryName;
            this.SAMPLE_NAME  = namedBarcode.sampleName;
            this.DESCRIPTION  = namedBarcode.description;
            this.barcodeBytes = net.sf.samtools.util.StringUtil.stringToBytes(this.BARCODE);
        }

        /**
         * This ctor is necessary for when reading metrics from file
         */
        public BarcodeMetric() {
            barcodeBytes = null;
        }
    }
}
