package boofcv.metrics.ecocheck;

import boofcv.BoofVersion;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckDetector;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckFound;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Searches for EcoCheck inside of images and saves the location of found markers and their corners.
 *
 * @author Peter Abeles
 */
public class DetectECoCheckImages<T extends ImageGray<T>> {
    @Option(name = "-i", aliases = {"--Input"}, usage = "Path to directory with labeled scenarios")
    public String inputDirectory;
    @Option(name = "-o", aliases = {"--Output"}, usage = "Where it should save the output to")
    public String outputFile = ".";
    @Option(name = "-e", aliases = {"--Encoding"}, usage = "Which encoding should it use")
    public String encodingString = "9x7e3n1";

    /**
     * Where results are written to
     */
    public File outputPath = new File("ecocheck_detections");

    public ECoCheckDetector<T> detector;

    private File root;

    final T gray;
    final Class<T> imageType;

    // Number of iterations it will perform before processing an image for real to make runtime results more accurate
    int warmIterations = 3;

    public DetectECoCheckImages(Class<T> imageType) {
        gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
        this.imageType = imageType;
    }

    /**
     * Calls detect using command line arguments
     */
    public void detectCommandline() {
        ConfigECoCheckMarkers config = ConfigECoCheckMarkers.parse(encodingString, 1.0);
        detector = FactoryFiducial.ecocheck(null, config, imageType).getDetector();
        outputPath = new File(outputFile);
        detect(new File(inputDirectory));
    }

    /**
     * Calls detect with the specified directory and whichever detector has been specified
     */
    public void detect(File directory) {
        BoofMiscOps.checkTrue(detector != null, "You must specify the detector!");
        this.root = directory;
        detectRecursive(directory);
    }

    /**
     * Searches for images recursively in the specified directory and attempts to detect EcoCheck markers when found
     */
    public void detectRecursive(File directory) {
        File[] children = directory.listFiles();
        if (children == null)
            return;

        // Ensure the order is consistent
        List<File> listChildren = Arrays.asList(children);
        Collections.sort(listChildren);

        boolean foundImage = false;

        File relativeToRoot = root.toPath().relativize(directory.toPath()).toFile();
        File base = new File(outputPath, relativeToRoot.getPath());

        for (File c : listChildren) {
            // Depth first search to find images.
            if (c.isDirectory()) {
                detectRecursive(c);
                continue;
            }

            if (!UtilImageIO.isImage(c))
                continue;

            if (!foundImage) {
                System.out.println("Processing: " + directory.getPath());
                if (!base.exists())
                    BoofMiscOps.checkTrue(base.mkdirs());
                foundImage = true;
            }

            BufferedImage buffered = UtilImageIO.loadImage(c.getPath());
            ConvertBufferedImage.convertFrom(buffered, true, gray);

            // "warm up" to make profiling results more accurate
            while (warmIterations > 0) {
                detector.process(gray);
                warmIterations--;
            }

            // Process image while measuring execution time
            long time0 = System.nanoTime();
            detector.process(gray);
            long time1 = System.nanoTime();
            double milliseconds = (time1 - time0) * 1e-6;

            // Save found landmarks
            saveResults(new File(base, "found_" + FilenameUtils.getBaseName(c.getName()) + ".txt"), milliseconds);
        }
    }

    private void saveResults(File outputPath, double timeMS) {
        // Merge split marker and remove unknown markers
        List<ECoCheckFound> filtered = detector.getUtils().mergeAndRemoveUnknown(detector.found.toList());

        try (PrintStream out = new PrintStream(outputPath)) {
            out.println("# ECoCheck Detections. BoofCV " + BoofVersion.VERSION);
            out.println("image.shape=" + gray.width + "," + gray.height);
            out.println("milliseconds=" + timeMS);
            out.println("markers.size=" + filtered.size());
            for (int i = 0; i < filtered.size(); i++) {
                ECoCheckFound found = filtered.get(i);
                out.println("marker=" + found.markerID);
                out.println("landmarks.size=" + found.corners.size);
                for (int cornerIdx = 0; cornerIdx < found.corners.size; cornerIdx++) {
                    PointIndex2D_F64 c = found.corners.get(cornerIdx);
                    out.printf("%d %.5f %.5f\n", c.index, c.p.x, c.p.y);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    public static void main(String[] args) {
        var generator = new DetectECoCheckImages<>(GrayF32.class);
        var parser = new CmdLineParser(generator);
        try {
            parser.parseArgument(args);
            if (generator.inputDirectory == null)
                printHelpExit(parser);
            generator.detectCommandline();
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        }
//        for (String encoding : new String[]{"9x7e0n1", "9x7e3n1"}) {
//            ConfigECoCheckMarkers config = ConfigECoCheckMarkers.parse(encoding, 1.0);
//
//            var app = new DetectECoCheckImages<>(config, GrayF32.class);
//            app.outputPath = new File("ecocheck_" + encoding + "_found");
//            app.detect(new File("ecocheck_" + encoding));
//        }
//        System.out.println("Done!");
    }
}
