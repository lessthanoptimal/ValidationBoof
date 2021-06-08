package boofcv.regression;

import boofcv.common.*;
import boofcv.io.UtilIO;
import boofcv.metrics.mvs.UncalibratedToSparseScenePlanarMetrics;
import boofcv.struct.image.ImageGray;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Regression test for uncalibrated image scene reconstruction using planar labels
 * <p>
 * NOTE: This is a file based regression and not an image based regression even though it processes images. All
 * the image processing has been tested elsewhere. The new stuff that is being tested here is all geometric code.
 *
 * @author Peter Abeles
 */
public class UncalibratedSparseReconstructionPlanarFRegression<T extends ImageGray<T>>
        extends BaseRegression implements FileRegression {

    public static final String PATH_SEQUENCE_DATA = "data/mvs/sequences";
    public static final String PATH_UNORDERED_DATA = "data/mvs/unordered";

    RuntimeSummary outputRuntime;

    UncalibratedToSparseScenePlanarMetrics<T> evaluator = new UncalibratedToSparseScenePlanarMetrics<>();

    public UncalibratedSparseReconstructionPlanarFRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    DogArray_F64 runtimes = new DogArray_F64();
    DogArray_F64 meanErrors = new DogArray_F64();
    int totalPoints = 0;
    int totalRegions = 0;
    int totalSkippedImages = 0;
    int totalScenarios = 0;
    int totalFailed = 0;
    int totalCrashed = 0;

    @Override
    public void process() throws IOException {
        outputRuntime = new RuntimeSummary();
        outputRuntime.initializeLog(directoryRuntime, getClass(), "RUN_NViewReconstruction.txt");
        outputRuntime.out.println("default");
        outputRuntime.out.printf("  %-30s %5s %7s %6s %6s %6s\n","","all","similar","pair","metric","sba");

        PrintStream out = new PrintStream(new File(directoryMetrics, "ACC_NViewReconstruction.txt"));
        BoofRegressionConstants.printGenerator(out, getClass());
        out.println("# Uncalibrated Multi-View Sparse Reconstruction using Planar Regions");
        out.println();
        out.println("#           Dataset,           Views,  Point, mean,  p50,  p95, max");
        out.println("#                              Used,   Count, (px), (px), (px), (px)");

        // Point output to correct location
        evaluator.err = errorLog;

        runtimes.reset();
        meanErrors.reset();
        totalPoints = 0;
        totalRegions = 0;
        totalSkippedImages = 0;
        totalScenarios = 0;
        totalFailed = 0;
        totalCrashed = 0;

        // Load all the data directories
        if (processDirectory(PATH_SEQUENCE_DATA, out, (dir) -> evaluator.processSequence(dir)))
            return;

        if (processDirectory(PATH_UNORDERED_DATA, out, (dir) -> evaluator.processUnordered(dir)))
            return;

        meanErrors.sort();
        out.println();
        out.println("Summary:");
        out.println("  scenarios         = " + totalScenarios);
        out.println("  failed            = " + totalFailed);
        out.println("  crashed           = " + totalCrashed);
        out.println("  points inside     = " + totalPoints);
        out.println("  regions evaluated = " + totalRegions);
        out.println("  skipped views     = " + totalSkippedImages);
        out.println("  median error (px) = " + meanErrors.getFraction(0.5));
        out.close();

        outputRuntime.out.println();
        outputRuntime.printUnitsRow(true);
        outputRuntime.printStatsRow("summary", runtimes);
        outputRuntime.out.close();
    }

    private boolean processDirectory(String dataPath, PrintStream out, ProcessDirectory<File> op) {
        File inputDir = new File(dataPath);
        List<File> children = UtilIO.listFilesSorted(inputDir);
        if (children.isEmpty()) {
            errorLog.println("No input directories found! path=" + inputDir.getPath());
            return true;
        }

        for (File dir : children) {
            if (!dir.isDirectory()) {
                continue;
            }
            System.out.println("Evaluating " + dir.getName());
            try {
                if (!op.process(dir)) {
                    totalFailed++;
                    out.printf("%-30s FAILED\n", dir.getName());
                    continue;
                }
                UncalibratedToSparseScenePlanarMetrics.RegionScore score = evaluator.allScore;
                double percentUsed = evaluator.fractionReconstructed * 100.0;

                out.printf("%-30s %5.1f%% %6d %5.1f %5.1f %5.1f %5.1f\n",
                        dir.getName(), percentUsed, score.count, score.mean, score.p50, score.p95, score.p100);
                outputRuntime.out.printf("  %-30s %6.1f %6.1f %6.1f %6.1f %6.1f\n", dir.getName(),
                        evaluator.processingTimeMS/1000.0,
                        evaluator.timeSimilarMS/1000.0, evaluator.timePairwiseMS/1000.0,
                        evaluator.timeMetricMS/1000.0, evaluator.timeBundleMS/1000.0);
                runtimes.add(evaluator.processingTimeMS);

                totalScenarios++;
                totalPoints += evaluator.allScore.count;
                totalRegions += evaluator.totalRegions;
                totalSkippedImages += evaluator.totalSkippedImages;
                meanErrors.add(evaluator.allScore.mean);
            } catch (Exception e) {
                totalCrashed++;
                out.printf("%-30s CRASHED\n", dir.getName());
                errorLog.println("Log Name: " + dir.getName());
                e.printStackTrace(errorLog);
                e.printStackTrace(System.err);
            }
        }
        return false;
    }

    @FunctionalInterface
    interface ProcessDirectory<T> {
        boolean process(T object);
    }

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{UncalibratedSparseReconstructionPlanarFRegression.class.getName()});
    }
}
