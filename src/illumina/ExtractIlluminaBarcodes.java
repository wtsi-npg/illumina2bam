/*
 * The MIT License
 *
 * Copyright (c) 2011 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package illumina;

import net.sf.picard.PicardException;
import net.sf.picard.cmdline.CommandLineProgram;
import net.sf.picard.cmdline.Option;
import net.sf.picard.cmdline.StandardOptionDefinitions;
import net.sf.picard.cmdline.Usage;
import net.sf.picard.io.IoUtil;
import net.sf.picard.metrics.MetricsFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;

import net.sf.picard.illumina.parser.IlluminaDataProviderFactory;
import net.sf.picard.illumina.parser.IlluminaDataType;
import net.sf.picard.illumina.parser.AbstractIlluminaDataProvider;
import net.sf.picard.illumina.parser.IlluminaReadData;
import net.sf.picard.util.Log;

/**
 * This class is part of Picard ExtractIlluminaBarcodes, which decoding function was separated into another class.
 * 
 * Determine the barcode for each read in an Illumina lane.
 * For each tile, a file is written to the basecalls directory of the form s_<lane>_<tile>_barcode.txt.
 * An output file contains a line for each read in the tile, aligned with the regular basecall output
 * The output file contains the following tab-separated columns:
 * - read subsequence at barcode position
 * - Y or N indicating if there was a barcode match
 * - matched barcode sequence (empty if read did not match one of the barcodes
 * - number of matches
 * - number of mismatches).
 * 
 * @author alecw@broadinstitute.org
 * @author gq1@sanger.ac.uk
 * 
 */

public class ExtractIlluminaBarcodes extends CommandLineProgram {
    private final Log log = Log.getInstance(ExtractIlluminaBarcodes.class);
    // The following attributes define the command-line arguments
    @Usage
    public String USAGE =
        getStandardUsagePreamble() +  "Determine the barcode for each read in an Illumina lane.\n" +
                "For each tile, a file is written to the basecalls directory of the form s_<lane>_<tile>_barcode.txt." +
                "An output file contains a line for each read in the tile, aligned with the regular basecall output\n" +
                "The output file contains the following tab-separated columns: \n" +
                "    * read subsequence at barcode position\n" +
                "    * Y or N indicating if there was a barcode match\n" +
                "    * matched barcode sequence\n" +
                "Note that the order of specification of barcodes can cause arbitrary differences in output for poorly matching barcodes.\n\n";

    @Option(doc="Deprecated option; use BASECALLS_DIR", mutex = "BASECALLS_DIR")
    public File BUSTARD_DIR;

    @Option(doc="The Illumina basecalls output directory. ", mutex = "BUSTARD_DIR", shortName="B")
    public File BASECALLS_DIR;

    @Option(doc="Where to write _barcode.txt files.  By default, these are written to BASECALLS_DIR.", optional = true)
    public File OUTPUT_DIR;

    @Option(doc="Lane number. ", shortName= StandardOptionDefinitions.LANE_SHORT_NAME)
    public Integer LANE;

    @Option(doc="1-based cycle number of the start of the barcode.", shortName = "BARCODE_POSITION")
    public Integer BARCODE_CYCLE;

    @Option(doc="Barcode sequence.  These must be unique, and all the same length.", mutex = {"BARCODE_FILE"})
    public List<String> BARCODE = new ArrayList<String>();

    @Option(doc="Tab-delimited file of barcode sequences, and optionally barcode name and library name.  " +
            "Barcodes must be unique, and all the same length.  Column headers must be 'barcode_sequence', " +
            "'barcode_name', and 'library_name'.", mutex = {"BARCODE"})
    public File BARCODE_FILE;

    @Option(doc="Per-barcode and per-lane metrics written to this file.", shortName = StandardOptionDefinitions.METRICS_FILE_SHORT_NAME)
    public File METRICS_FILE;

    @Option(doc="Maximum mismatches for a barcode to be considered a match.")
    public int MAX_MISMATCHES = 1;

    @Option(doc="Minimum difference between number of mismatches in the best and second best barcodes for a barcode to be considered a match.")
    public int MIN_MISMATCH_DELTA = 1;

    @Option(doc="Maximum allowable number of no-calls in a barcode read before it is considered unmatchable.")
    public int MAX_NO_CALLS = 2;

    private int barcodeLength;

    private int tile = 0;
    private File barcodeFile = null;
    private BufferedWriter writer = null;

    private final NumberFormat tileNumberFormatter = NumberFormat.getNumberInstance();

    private IndexDecoder indexDecoder;

    public ExtractIlluminaBarcodes() {
        tileNumberFormatter.setMinimumIntegerDigits(4);
        tileNumberFormatter.setGroupingUsed(false);
    }

    @Override
    protected int doWork() {
        if(BUSTARD_DIR != null) {
            BASECALLS_DIR = BUSTARD_DIR;
        }
        
        IoUtil.assertDirectoryIsWritable(BASECALLS_DIR);
        IoUtil.assertFileIsWritable(METRICS_FILE);
        if (OUTPUT_DIR == null) {
            OUTPUT_DIR = BASECALLS_DIR;
        }
        IoUtil.assertDirectoryIsWritable(OUTPUT_DIR);

        final IlluminaDataProviderFactory factory = new IlluminaDataProviderFactory(BASECALLS_DIR, LANE,
                BARCODE_CYCLE, barcodeLength, IlluminaDataType.BaseCalls, IlluminaDataType.PF);
        // This is possible for index-only run.
        factory.setAllowZeroLengthFirstEnd(true);
        final AbstractIlluminaDataProvider parser = factory.makeDataProvider();

        // Process each tile qseq file.
        try {
            while (parser.hasNext()) {
                final IlluminaReadData ird = parser.next();
                ensureBarcodeFileOpen(ird.getTile());
                this.indexDecoder.extractBarcode(ird, writer);
            }
            if (writer != null) {
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            throw new PicardException("IOException writing barcode file " + barcodeFile, e);
        }
        
        final MetricsFile<IndexDecoder.BarcodeMetric, Integer> metrics = getMetricsFile();        
        indexDecoder.writeMetrics(metrics, METRICS_FILE);

        return 0;
    }

    /**
     * Scan through qseqFile, and create a sibling barcode file with the barcode assignment lined up with the
     * tile's qseq file.
     */
    private void ensureBarcodeFileOpen(final int tile) {
        if (tile == this.tile) {
            return;
        }
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
            this.tile = tile;
            barcodeFile = getBarcodeFile(tile);
            writer = IoUtil.openFileForBufferedWriting(barcodeFile);
        } catch (IOException e) {
            throw new PicardException("IOException " + barcodeFile, e);
        }
    }


    /**
     * Create a barcode filename corresponding to the given tile qseq file.
     */
    private File getBarcodeFile(final int tile) {
        return new File(OUTPUT_DIR, "s_" + LANE + "_" + tileNumberFormatter.format(tile) + "_barcode.txt");
    }

    /**
     * Validate that POSITION >= 1, and that all BARCODEs are the same length and unique
     *
     * @return null if command line is valid.  If command line is invalid, returns an array of error message
     *         to be written to the appropriate place.
     */
    @Override
    protected String[] customCommandLineValidation() {
        
        final ArrayList<String> messages = new ArrayList<String>();
        if (BARCODE_CYCLE < 1) {
            messages.add("Invalid BARCODE_POSITION=" + BARCODE_CYCLE + ".  Value must be positive.");
        }
        if (BARCODE_FILE != null) {
            this.indexDecoder = new IndexDecoder(BARCODE_FILE);
        } else {
            this.indexDecoder = new IndexDecoder(BARCODE);
        }
        
        indexDecoder.prepareDecode(messages);
        this.barcodeLength = indexDecoder.getBarcodeLength();

        if (messages.isEmpty()) {
            return null;
        }
        return messages.toArray(new String[messages.size()]);
    }
    //BASECALLS_DIR=/nfs/sf44/ILorHSany_sf44/outgoing/110520_IL34_06287/Data/Intensities/Bustard1.8.1a2_29-05-2011_RTA OUTPUT_DIR=/nfs/users/nfs_g/gq1/Desktop/6287 LANE=1 BARCODE_CYCLE=77 BARCODE_FILE=/nfs/users/nfs_g/gq1/Desktop/6287/lane_1.tag METRICS_FILE=/nfs/users/nfs_g/gq1/Desktop/6287/metrics.txt VERBOSITY=DEBUG
    public static void main(final String[] argv) {
        System.exit(new ExtractIlluminaBarcodes().instanceMain(argv));
    }

}
