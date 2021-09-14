package boofcv.metrics.ecocheck;

import boofcv.common.parsing.ObservedLandmarkMarkers;
import boofcv.common.parsing.ParseCalibrationConfigFiles;
import boofcv.common.parsing.UniqueMarkerObserved;
import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.PointIndex2D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_F64;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import static boofcv.common.parsing.ParseCalibrationConfigFiles.parseUniqueMarkerTruth;

/**
 * Process labeled ground truth and found detections to compute performance metrics based on detection rate and
 * corner location accuracy. Results are summarized in each directory and overall
 *
 * @author Peter Abeles
 */
public class EvaluateMarkerLandmarkDetections {
    @Option(name = "-f", aliases = {"--Found"}, usage = "Path to found data.")
    public String foundPath = "";

    @Option(name = "-t", aliases = {"--Truth"}, usage = "Path to truth data.")
    public String truthPath = "";

    @Option(name = "-o", aliases = {"--Output"}, usage = "Where it should dump machine readable metrics.")
    public String machinePath = "";

    Statistics summary = new Statistics();
    Statistics scenario = new Statistics();
    Statistics fileStats = new Statistics();

    public PrintStream out = System.out;
    public PrintStream err = System.err;

    File rootTruth;
    File rootFound;
    // If it's told to save machine-readable results, this is where they should go
    File rootOutput;

    // log landmarks with very large errors
    public double largeErrorTol = 10.0;
    // If not null then bad landmarks will be logged
    public PrintStream outBad = null;

    // It should save
    public boolean saveMachineReadable = false;

    public final Map<String, DogArray_F64> runtimeResults = new HashMap<>();

    // Workspace for saving per landmark errors in a file
    StringBuffer stringLandmarkErrors = new StringBuffer(10000);

    /**
     * Evaluates the directories specified using command line arguments
     */
    public void evaluateCommandline() {
        BoofMiscOps.checkTrue(!foundPath.isEmpty(), "Must specify found path");
        BoofMiscOps.checkTrue(!truthPath.isEmpty(), "Must specify truth path");

        evaluateRecursive(foundPath, truthPath);
    }

    /**
     * Evalutes the directories which are specified as arguments
     */
    public void evaluateRecursive(String foundPath, String truthPath) {
        saveMachineReadable = !machinePath.isEmpty();
        if (saveMachineReadable) {
            rootOutput = new File(machinePath);
        }

        System.out.println("\nfound path = " + foundPath);
        out.println("# Detection performance using labeled markers and corners.");
        out.println("# name (count markers) (marker false positive) (marker false negative), (count corners) (percent FP) (percent FN), (duplicate markers) (duplicate corners), error50 err90 err100");
        out.printf("# %-38s %3s %3s %4s , %5s %4s %4s , %2s %2s , %-6s %-6s %-6s\n",
                "", "CM", "MFP", "MFN", "CC", "CFP", "CFN", "DM", "DC", "  E50", "  E90", "  E100");
        out.println();
        rootFound = new File(foundPath);
        rootTruth = new File(truthPath);

        runtimeResults.clear();
        summary.reset();
        evaluateRecursive(rootTruth);

        out.println();
        summary.print(out, "Summary");
    }

    private void evaluateRecursive(File truthPath) {
        File[] children = truthPath.listFiles();
        if (children == null)
            return;

        // Ensure the order is consistent
        List<File> listChildren = Arrays.asList(children);
        Collections.sort(listChildren);

        File relativeToRoot = rootTruth.toPath().relativize(truthPath.toPath()).toFile();
        File baseFound = new File(rootFound, relativeToRoot.getPath());
        File baseOutput = new File(rootOutput, relativeToRoot.getPath());

        if (!baseFound.exists()) {
            err.println("Missing directory: " + baseFound.getPath());
            return;
        }

        PrintStream machineOut = createOutputStream(baseOutput, "scenarios.txt");
        if (machineOut != null)
            Statistics.printMachineHeader(machineOut);

        for (File f : listChildren) {
            if (!f.isDirectory())
                continue;

            File b = new File(baseFound, f.getName());
            if (!b.exists()) {
                err.println("Missing child: " + b.getPath());
                continue;
            }

            File outputDir = new File(baseOutput, f.getName());

//            System.out.println("Processing: " + b.getPath() + " " + f.getPath());
            DogArray_F64 runtime = new DogArray_F64();
            if (evaluate(b.getPath(), f.getPath(), runtime, outputDir.getPath())) {
                String relativePath = new File(relativeToRoot, f.getName()).getPath();
                scenario.print(out, relativePath);
                if (saveMachineReadable)
                    scenario.printMachine(machineOut, f.getName());
                summary.add(scenario);
                runtimeResults.put(relativePath, runtime);
            } else {
                evaluateRecursive(f);
            }
        }

        if (machineOut != null)
            machineOut.close();

        PrintStream summaryOut = createOutputStream(baseOutput, "summary.txt");
        if (summaryOut != null) {
            Statistics.printMachineHeader(summaryOut);
            summary.printMachine(summaryOut, "summary");
            summaryOut.close();
        }
    }

    @Nullable
    private PrintStream createOutputStream(File baseOutput, String fileName) {
        PrintStream machineOut = null;
        if (saveMachineReadable) {
            try {
                UtilIO.mkdirs(baseOutput);
                machineOut = new PrintStream(new File(baseOutput, fileName));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return machineOut;
    }

    /**
     * Compare detected results against the true expected results and compute performance metrics
     */
    protected boolean evaluate(String detectionPath, String truthPath, DogArray_F64 runtime,
                               String outputPath) {
        scenario.reset();

        List<String> listFoundPath = UtilIO.listSmart("glob:" + detectionPath + "/found_*.txt", true, (f) -> true);
        List<String> listTruthPath = UtilIO.listSmart("glob:" + truthPath + "/landmarks_*.txt", true, (f) -> true);
        BoofMiscOps.checkEq(listFoundPath.size(), listTruthPath.size(), detectionPath);

        if (listFoundPath.isEmpty())
            return false;

        PrintStream machineOut = createOutputStream(new File(outputPath), "images.txt");
        if (machineOut != null) {
            Statistics.printMachineHeader(machineOut);
        }

        for (int imageIndex = 0; imageIndex < listFoundPath.size(); imageIndex++) {
            try {
                File foundFile = new File(listFoundPath.get(imageIndex));
                ObservedLandmarkMarkers found = ParseCalibrationConfigFiles.parseObservedLandmarkMarker(foundFile);
                List<UniqueMarkerObserved> expected = parseUniqueMarkerTruth(new File(listTruthPath.get(imageIndex)));
//                System.out.println("path="+listFoundPath.get(imageIndex));
                evaluateImage(found, expected, foundFile);
                if (machineOut != null) {
                    fileStats.printMachine(machineOut, foundFile.getName());
                    String name = FilenameUtils.getBaseName(foundFile.getName());

                    // Save the error every found feature in the image
                    PrintStream individualImage = createOutputStream(new File(outputPath), name+".txt");
                    if (individualImage != null) {
                        individualImage.print(stringLandmarkErrors.toString());
                        individualImage.close();
                    }
                }
                scenario.add(fileStats);
                runtime.add(found.milliseconds);
            } catch (RuntimeException e) {
                e.printStackTrace(err);
                err.println("Error processing " + listFoundPath.get(imageIndex) + " " + listTruthPath.get(imageIndex));
            }
        }

        if (machineOut != null) {
            machineOut.close();
        }

        return true;
    }

    void evaluateImage(ObservedLandmarkMarkers found, List<UniqueMarkerObserved> expected, File foundFile) {
        fileStats.reset();
        fileStats.totalMarkers += expected.size();

        if (saveMachineReadable) {
            stringLandmarkErrors.setLength(0);
            stringLandmarkErrors.append("# individual landmark errors. marker, feature, dx, dy\n");
            stringLandmarkErrors.append("# ").append(foundFile.getPath()).append("\n");
        }

        DogArray_B markerMatched = new DogArray_B();
        markerMatched.resetResize(expected.size(), false);
        DogArray_B cornerMatched = new DogArray_B();
        for (UniqueMarkerObserved f : found.markers.toList()) {
            UniqueMarkerObserved e = null;
            for (int i = 0; i < expected.size(); i++) {
                if (expected.get(i).markerID == f.markerID) {
                    e = expected.get(i);
                    if (markerMatched.get(i)) {
                        fileStats.duplicateMarkers++;
                    }
                    markerMatched.set(i, true);
                    break;
                }
            }

            if (e == null) {
                fileStats.falsePositiveMarker++;
                continue;
            }

            // crude estimate of max size
            cornerMatched.resetResize(e.landmarks.size, false);

            fileStats.totalCorners += e.landmarks.size;
            for (int i = 0; i < f.landmarks.size; i++) {
                PointIndex2D_F64 foundLandmark = f.landmarks.get(i);
                PointIndex2D_F64 truthLandmark = e.findLandmark(foundLandmark.index);
                if (truthLandmark == null) {
//                    System.out.println("FP corner. "+foundLandmark);
                    fileStats.falsePositiveCorner++;
                    continue;
                }

                // We don't know initially how many landmarks there are so if we need to increase the array's size
                if (foundLandmark.index >= cornerMatched.size) {
                    cornerMatched.resize(foundLandmark.index + 1, false);
                }

                // If there's a duplicate record it
                if (cornerMatched.get(foundLandmark.index)) {
                    fileStats.duplicateCorners++;
                }

                cornerMatched.set(foundLandmark.index, true);
                double error = foundLandmark.p.distance(truthLandmark.p);
                fileStats.errors.add(error);

                if (saveMachineReadable)
                    stringLandmarkErrors.append(String.format("%d %d %.6f %.6f\n", e.markerID, foundLandmark.index,
                            foundLandmark.p.x - truthLandmark.p.x, foundLandmark.p.y - truthLandmark.p.y));

                if (outBad != null && error > largeErrorTol) {
                    outBad.printf("bad landmark: error=%6.1f landmarkID=%d file=%s\n"
                            , error, foundLandmark.index, foundFile.getPath());
                }

            }

            fileStats.falseNegativeCorner += cornerMatched.count(false);
        }

        fileStats.falseNegativeMarker += markerMatched.count(false);
    }

    static class Statistics {
        public int totalMarkers;
        public int totalCorners;
        public int falsePositiveMarker = 0;
        public int falseNegativeMarker = 0;
        public int falsePositiveCorner = 0;
        public int falseNegativeCorner = 0;
        public int duplicateCorners = 0;
        public int duplicateMarkers = 0;
        public DogArray_F64 errors = new DogArray_F64();
        public DogArray_F64 runtime = new DogArray_F64();

        public void reset() {
            totalMarkers = 0;
            totalCorners = 0;
            falsePositiveMarker = 0;
            falseNegativeMarker = 0;
            falsePositiveCorner = 0;
            falseNegativeCorner = 0;
            duplicateCorners = 0;
            duplicateMarkers = 0;
            errors.reset();
            runtime.reset();
        }

        public void add(Statistics src) {
            totalMarkers += src.totalMarkers;
            totalCorners += src.totalCorners;
            falsePositiveMarker += src.falsePositiveMarker;
            falseNegativeMarker += src.falseNegativeMarker;
            falsePositiveCorner += src.falsePositiveCorner;
            falseNegativeCorner += src.falseNegativeCorner;
            duplicateCorners += src.duplicateCorners;
            duplicateMarkers += src.duplicateMarkers;
            errors.addAll(src.errors);
            runtime.addAll(src.runtime);
        }

        public void print(PrintStream out, String directory) {
            errors.sort();
            double error50, error90, error100;
            if (errors.isEmpty()) {
                error50 = error90 = error100 = Double.NaN;
            } else {
                error50 = errors.getFraction(0.5);
                error90 = errors.getFraction(0.9);
                error100 = errors.getFraction(1.0);
            }

            double percentFalsePositiveCorner = 100.0 * falsePositiveCorner / totalCorners;
            double percentFalseNegativeCorner = 100.0 * falseNegativeCorner / totalCorners;

            out.printf("%-40s %3d %3d %4d , %5d %4.1f %4.1f , %2d %2d , %6.2f %6.2f %6.2f\n", directory,
                    totalMarkers, falsePositiveMarker, falseNegativeMarker,
                    totalCorners, percentFalsePositiveCorner, percentFalseNegativeCorner,
                    duplicateMarkers, duplicateCorners,
                    error50, error90, error100);
        }

        public static void printMachineHeader(PrintStream out) {
            out.printf("# %s %s %s %s %s %s %s %s %s %s %s %s\n",
                    "directory", "total_markers", "fp_markers", "fn_markers", "total_corners", "ffp_corners", "ffn_corners", "dup_markers", "dup_corners", "err50", "err90", "err100");
        }

        public void printMachine(PrintStream out, String directory) {
            errors.sort();
            double error50, error90, error100;
            if (errors.isEmpty()) {
                error50 = error90 = error100 = Double.NaN;
            } else {
                error50 = errors.getFraction(0.5);
                error90 = errors.getFraction(0.9);
                error100 = errors.getFraction(1.0);
            }

            double fractionFalsePositiveCorner = falsePositiveCorner / (double) totalCorners;
            double fractionFalseNegativeCorner = falseNegativeCorner / (double) totalCorners;

            out.printf("%s %d %d %d %d %.5e %.5e %d %d %.5e %.5e %.5e\n", directory,
                    totalMarkers, falsePositiveMarker, falseNegativeMarker,
                    totalCorners, fractionFalsePositiveCorner, fractionFalseNegativeCorner,
                    duplicateMarkers, duplicateCorners,
                    error50, error90, error100);
        }
    }

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    public static void main(String[] args) {
        var app = new EvaluateMarkerLandmarkDetections();
        var parser = new CmdLineParser(app);
        try {
            parser.parseArgument(args);
            app.evaluateCommandline();
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        }
//        var app = new EvaluateMarkerLandmarkDetections();
////        app.outBad = System.out;
//        app.evaluateRecursive("ecocheck_9x7e0n1_found", "ecocheck_9x7e0n1");
//        app.evaluateRecursive("ecocheck_9x7e3n1_found", "ecocheck_9x7e3n1");
//        app.evaluateRecursive("charuco/charuco_6X8_6X6_100_found", "charuco/charuco_6X8_6X6_100");
//        app.evaluateRecursive("aruco_grids/7x5_6X6_found", "aruco_grids/7x5_6X6");
//        app.evaluateRecursive("aruco_grids/original_found_aruco3", "aruco_grids/original");
//        app.evaluateRecursive("found/tcad", "renderings/tcad");
    }
}
