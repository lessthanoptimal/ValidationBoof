package boofcv.metrics.ecocheck;

import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import boofcv.parsing.UniqueMarkerObserved;
import boofcv.struct.geo.PointIndex2D_F64;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static boofcv.parsing.ParseCalibrationConfigFiles.parseObservedECoCheck;
import static boofcv.parsing.ParseCalibrationConfigFiles.parseUniqueMarkerTruth;

/**
 * Process labeled ground truth and found detections to compute performance metrics based on detection rate and
 * corner location accuracy. Results are summarized in each directory and overall
 *
 * @author Peter Abeles
 */
public class EvaluateECoCheckDetections {

    Statistics summary = new Statistics();
    Statistics scenario = new Statistics();

    PrintStream out = System.out;
    PrintStream err = System.err;

    File rootTruth;
    File rootFound;

    public void evaluateRecursive(String foundPath, String truthPath) {
        out.println("# ECoCheck detection performance using labeled markers and corners.");
        out.println("# name (count markers) (marker false positive) (marker false negative) (count corners) (corner FP) (corner FN) (duplicate markers) (duplicate corners) err90 err100");
        out.println();
        rootFound = new File(foundPath);
        rootTruth = new File(truthPath);

        summary.reset();
        evaluateRecursive(rootTruth);
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

        if (!baseFound.exists()) {
            err.println("Missing directory: " + baseFound.getPath());
            return;
        }

        for (File f : listChildren) {
            File b = new File(baseFound, f.getName());
            if (!b.exists()) {
                err.println("Missing directory: " + b.getPath());
                continue;
            }

//            System.out.println("Processing: " + b.getPath() + " " + f.getPath());
            if (evaluate(b.getPath(), f.getPath())) {
                scenario.print(out, f.getName());
                summary.add(scenario);
            } else {
                evaluateRecursive(f);
            }
        }

        out.println();
        summary.print(out, "Summary");
    }

    /**
     * Compare detected results against the true expected results and compute performance metrics
     */
    public boolean evaluate(String detectionPath, String truthPath) {
        scenario.reset();

        List<String> listFoundPath = UtilIO.listSmart("glob:" + detectionPath + "/found_*.txt", true, (f)->true);
        List<String> listTruthPath = UtilIO.listSmart("glob:" + truthPath + "/landmarks_*.txt", true, (f)->true);
        BoofMiscOps.checkEq(listFoundPath.size(), listTruthPath.size());

        if (listFoundPath.isEmpty())
            return false;

        for (int imageIndex = 0; imageIndex < listFoundPath.size(); imageIndex++) {
            try {
                List<UniqueMarkerObserved> found = parseObservedECoCheck(new File(listFoundPath.get(imageIndex)));
                List<UniqueMarkerObserved> expected = parseUniqueMarkerTruth(new File(listTruthPath.get(imageIndex)));
//                System.out.println("path="+listFoundPath.get(imageIndex));
                evaluateImage(found, expected);
            } catch (RuntimeException e) {
                e.printStackTrace(err);
                err.println("Error processing "+listFoundPath.get(imageIndex)+" "+listTruthPath.get(imageIndex));
            }
        }

        return true;
    }

    void evaluateImage(List<UniqueMarkerObserved> found, List<UniqueMarkerObserved> expected) {
        scenario.totalMarkers += expected.size();

        DogArray_B markerMatched = new DogArray_B();
        markerMatched.resetResize(expected.size(), false);
        DogArray_B cornerMatched = new DogArray_B();
        for (UniqueMarkerObserved f : found) {
            UniqueMarkerObserved e = null;
            for (int i = 0; i < expected.size(); i++) {
                if (expected.get(i).markerID == f.markerID) {
                    e = expected.get(i);
                    if (markerMatched.get(i)) {
                        scenario.duplicateMarkers++;
                    }
                    markerMatched.set(i, true);
                    break;
                }
            }

            if (e == null) {
                scenario.falsePositiveMarker++;
                continue;
            }

            // crude estimate of max size
            cornerMatched.resetResize(e.landmarks.size, false);

            scenario.totalCorners += e.landmarks.size;
            for (int i = 0; i < f.landmarks.size; i++) {
                PointIndex2D_F64 foundLandmark = f.landmarks.get(i);
                PointIndex2D_F64 truthLandmark = e.findLandmark(foundLandmark.index);
                if (truthLandmark == null) {
//                    System.out.println("FP corner. "+foundLandmark);
                    scenario.falsePositiveCorner++;
                    continue;
                }

                // We don't know initially how many landmarks there are so if we need to increase the array's size
                if (foundLandmark.index >= cornerMatched.size) {
                    cornerMatched.resize(foundLandmark.index + 1, false);
                }

                // If there's a duplicate record it
                if (cornerMatched.get(foundLandmark.index)) {
                    scenario.duplicateCorners++;
                }

                cornerMatched.set(foundLandmark.index, true);
                scenario.errors.add(foundLandmark.p.distance(truthLandmark.p));
            }

            scenario.falseNegativeCorner += cornerMatched.count(false);
        }

        scenario.falseNegativeMarker += markerMatched.count(false);
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
        }

        public void print(PrintStream out, String directory) {
            errors.sort();
            double error90, error100;
            if (errors.isEmpty()) {
                error90 = error100 = Double.NaN;
            } else {
                error90 = errors.getFraction(0.9);
                error100 = errors.getFraction(1.0);
            }

            out.printf("%-30s %3d %3d %3d %5d %3d %3d %d %d %6.2f %6.2f\n", directory,
                    totalMarkers, falsePositiveMarker, falseNegativeMarker,
                    totalCorners, falsePositiveCorner, falseNegativeCorner,
                    duplicateMarkers, duplicateCorners,
                    error90, error100);
        }
    }

    public static void main(String[] args) {
        var app = new EvaluateECoCheckDetections();
        app.evaluateRecursive("ecocheck_9x7e3n1_found", "ecocheck_9x7e3n1");
        app.evaluateRecursive("ecocheck_9x7e0n1_found", "ecocheck_9x7e0n1");
    }
}
