package boofcv.regression;

import boofcv.alg.structure.ImageSequenceToSparseScene;
import boofcv.common.*;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.io.UtilIO;
import boofcv.metrics.mvs.UncalibratedToSparseScenePlanarMetrics;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Regression test for uncalibrated image scene reconstruction using planar labels
 *
 * @author Peter Abeles
 */
public class UncalibratedSparseReconstructionPlanarRegression<T extends ImageGray<T>>
        extends BaseRegression implements ImageRegression {

    public static final String PATH_DATA = "data/mvs";

    RuntimeSummary outputRuntime;

    ImageSequenceToSparseScene<T> alg;

    UncalibratedToSparseScenePlanarMetrics<T> evaluator = new UncalibratedToSparseScenePlanarMetrics<>();

    public UncalibratedSparseReconstructionPlanarRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process(ImageDataType type) throws IOException {

        outputRuntime = new RuntimeSummary();
        outputRuntime.initializeLog(directoryRuntime, getClass(), "RUN_NViewReconstruction.txt");
        outputRuntime.out.println("default\n");

        PrintStream out = new PrintStream(new File(directoryMetrics, "ACC_NViewReconstruction.txt"));
        BoofRegressionConstants.printGenerator(out, getClass());
        out.println("# Uncalibrated Multi-View Sparse Reconstruction using Planar Regions");
        out.println();
        out.println("#           Dataset,           Views, Point, mean,  p50,  p95, max");
        out.println("#                              Used,  Count, (px), (px), (px), (px)");

        Class<T> imageType = ImageDataType.typeToSingleClass(type);
//        ConfigSequenceToSparseScene config = new ConfigSequenceToSparseScene();
//        config.pairwise.score.type = ConfigEpipolarScore3D.Type.FUNDAMENTAL_ERROR;
        alg = FactorySceneReconstruction.sequenceToSparseScene(null, ImageType.single(imageType));

        // Load all the data directories
        File inputDir = new File(PATH_DATA);
        List<File> children = UtilIO.listFilesSorted(inputDir);
        if (children.isEmpty()) {
            errorLog.println("No input directories found! path=" + inputDir.getPath());
            return;
        }

        // Point output to correct location
        evaluator.err = errorLog;

        DogArray_F64 runtimes = new DogArray_F64();
        int totalPoints = 0;
        int totalRegions = 0;
        int totalSkippedImages = 0;
        double averageMeanError = 0;
        int totalScenarios = 0;
        int totalFailed = 0;

        for (File dir : children) {
            if (!dir.isDirectory()) {
                continue;
            }
            System.out.println("Evaluating " + dir.getName());
            try {
                if (!evaluator.process(dir, alg)) {
                    totalFailed++;
                    out.printf("%-30s FAILED\n", dir.getName());
                    continue;
                }
                UncalibratedToSparseScenePlanarMetrics.RegionScore score = evaluator.allScore;
                double percentUsed = evaluator.fractionReconstructed * 100.0;

                out.printf("%-30s %5.1f%% %5d %5.1f %5.1f %5.1f %5.1f\n",
                        dir.getName(), percentUsed, score.count, score.mean, score.p50, score.p95, score.p100);
                outputRuntime.out.printf("  %-30s %d\n", dir.getName(), evaluator.processingTimeMS);
                runtimes.add(evaluator.processingTimeMS);

                totalScenarios++;
                totalPoints += evaluator.allScore.count;
                totalRegions += evaluator.totalRegions;
                totalSkippedImages += evaluator.totalSkippedImages;
                averageMeanError += evaluator.allScore.mean;
            } catch (Exception e) {
                out.printf("%-30s CRASHED\n", dir.getName());
                errorLog.println("Log Name: " + dir.getName());
                e.printStackTrace(errorLog);
                e.printStackTrace(System.err);
            }
        }
        out.println();
        out.println("Summary:");
        out.println("  scenarios         = " + totalScenarios);
        out.println("  failed            = " + totalFailed);
        out.println("  points inside     = " + totalPoints);
        out.println("  regions evaluated = " + totalRegions);
        out.println("  skipped views     = " + totalSkippedImages);
        out.printf("  mean error (px)   = %.2f", averageMeanError / totalScenarios);
        out.close();

        outputRuntime.out.println();
        outputRuntime.printUnitsRow(true);
        outputRuntime.printStatsRow("summary", runtimes);
        outputRuntime.out.close();
    }

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        BoofRegressionConstants.clearCurrentResults();
//        RegressionRunner.main(new String[]{UncalibratedSparseReconstructionPlanarRegression.class.getName(),ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{UncalibratedSparseReconstructionPlanarRegression.class.getName(), ImageDataType.U8.toString()});
    }
}
