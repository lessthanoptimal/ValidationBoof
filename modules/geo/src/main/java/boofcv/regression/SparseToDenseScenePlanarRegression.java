package boofcv.regression;

import boofcv.alg.structure.SparseSceneToDenseCloud;
import boofcv.common.*;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.io.UtilIO;
import boofcv.metrics.mvs.SparseToDenseScenePlanarMetrics;
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
public class SparseToDenseScenePlanarRegression<T extends ImageGray<T>>
        extends BaseRegression implements ImageRegression {

    public static final String PATH_DATA = "data/mvs";

    RuntimeSummary outputRuntime;

    SparseSceneToDenseCloud<T> alg;

    SparseToDenseScenePlanarMetrics<T> evaluator = new SparseToDenseScenePlanarMetrics<>();

    public SparseToDenseScenePlanarRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process(ImageDataType type) throws IOException {

        outputRuntime = new RuntimeSummary();
        outputRuntime.initializeLog(directoryRuntime, getClass(), "RUN_SparseToDenseCloud.txt");
        outputRuntime.out.println("default\n");

        PrintStream out = new PrintStream(new File(directoryMetrics, "ACC_SparseToDenseCloud.txt"));
        BoofRegressionConstants.printGenerator(out, getClass());
        out.println("# Sparse to Dense Cloud Reconstruction using Planar Regions");
        out.println();
        out.println("#           Dataset,            Points, mean,  p50,  p95, max");
        out.println("#                                1e3  , (px), (px), (px), (px)");

        Class<T> imageType = ImageDataType.typeToSingleClass(type);
        alg = FactorySceneReconstruction.sparseSceneToDenseCloud(null, ImageType.single(imageType));

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
        DogArray_F64 meanErrors = new DogArray_F64();
        int totalScenarios = 0;
        int totalFailed = 0;
        int totalCrashed = 0;

        for (File dir : children) {
            if (!dir.isDirectory()) {
                continue;
            }
            // make sure it has the required files. Not all scenes have reconstructions already
            if (!new File(dir, SparseToDenseScenePlanarMetrics.SCENE_NAME).exists() ||
                    !new File(dir, SparseToDenseScenePlanarMetrics.IMAGE_MAP_NAME).exists())
                continue;

            System.out.println("Evaluating " + dir.getName());
            try {
                if (!evaluator.process(dir, alg)) {
                    totalFailed++;
                    out.printf("%-30s FAILED\n", dir.getName());
                    continue;
                }
                UncalibratedToSparseScenePlanarMetrics.RegionScore score = evaluator.allScore;

                out.printf("%-30s %7d %5.1f %5.1f %5.1f %5.1f\n",
                        dir.getName(), score.count / 1000, score.mean, score.p50, score.p95, score.p100);
                outputRuntime.out.printf("  %-30s %d\n", dir.getName(), evaluator.processingTimeMS);
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

        // Give a useful error message and abort if things went really wrong
        if (totalScenarios == 0) {
            errorLog.println("No scenarios found/processed: path_data=" + PATH_DATA);
            return;
        }

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

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        BoofRegressionConstants.clearCurrentResults();
//        RegressionRunner.main(new String[]{UncalibratedSparseReconstructionPlanarRegression.class.getName(),ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{SparseToDenseScenePlanarRegression.class.getName(), ImageDataType.U8.toString()});
    }
}
