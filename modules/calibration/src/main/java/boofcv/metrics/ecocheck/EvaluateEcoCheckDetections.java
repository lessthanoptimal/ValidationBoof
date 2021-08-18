package boofcv.metrics.ecocheck;

import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import boofcv.parsing.ParseCalibrationConfigFiles;
import boofcv.parsing.UniqueMarkerObserved;
import boofcv.struct.geo.PointIndex2D_F64;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateEcoCheckDetections {

    Statistics summary = new Statistics();
    Statistics scenario = new Statistics();

    PrintStream out = System.out;
    PrintStream err = System.err;

    File rootTruth;
    File rootFound;

    public void evaluateRecursive(String foundPath, String truthPath) {
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

            if (evaluate(b.getPath(), f.getPath())) {
                summary.add(scenario);
            } else {
                evaluateRecursive(f);
            }
        }
    }

    /**
     * Compare detected results against the true expected results and compute performance metrics
     */
    public boolean evaluate(String detectionPath, String truthPath) {
        scenario.reset();

        List<String> listFoundPath = UtilIO.listSmart("glob:" + detectionPath + "/detection_*.txt", true, null);
        List<String> listTruthPath = UtilIO.listSmart("glob:" + truthPath + "/landmarks_*.txt", true, null);
        BoofMiscOps.checkEq(listFoundPath.size(), listTruthPath.size());

        if (listFoundPath.isEmpty())
            return false;

        for (int imageIndex = 0; imageIndex < listFoundPath.size(); imageIndex++) {
            List<UniqueMarkerObserved> found = ParseCalibrationConfigFiles.parseObservedMarkers(new File(listFoundPath.get(imageIndex)));
            List<UniqueMarkerObserved> expected = ParseCalibrationConfigFiles.parseObservedMarkers(new File(listTruthPath.get(imageIndex)));

            evaluateImage(found, expected);
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
                PointIndex2D_F64 foundLandmark = e.landmarks.get(i);
                PointIndex2D_F64 truthLandmark = e.findLandmark(foundLandmark.index);
                if (truthLandmark == null) {
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

        public void add( Statistics src ) {
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
    }
}
