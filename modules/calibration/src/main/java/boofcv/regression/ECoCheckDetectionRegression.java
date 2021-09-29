package boofcv.regression;

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.common.*;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.generate.RenderDocumentViewsApp;
import boofcv.metrics.ecocheck.DetectECoCheckImages;
import boofcv.metrics.ecocheck.EvaluateMarkerLandmarkDetections;
import boofcv.struct.image.ImageDataType;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ECoCheckDetectionRegression extends BaseRegression implements ImageRegression {

    String pathDataset = "data/calibration_mono/ecocheck";

    // directory used as temporary workspace
    File fileTmp = new File("tmp");


    public ECoCheckDetectionRegression() {
        super(BoofRegressionConstants.TYPE_CALIBRATION);
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        // Generate images
        File generatedBase = new File(pathDataset);
        File detectedBase = new File(fileTmp, "detected");

        for (String encoding : new String[]{"9x7n1", "9x7n1e0"}) {
            System.out.println("Rendering");
            File renderedOutput = new File(generatedBase, encoding);

            try {
                // Render the simulated data if it doesn't already exist
                if (!renderedOutput.exists()) {
                    var generator = new RenderDocumentViewsApp();
                    generator.inputFile = new File(new File(pathDataset), "ecocheck_" + encoding + ".pdf").getPath();
                    generator.destinationDir = renderedOutput.getPath();
                    generator.landmarksFile = new File(new File(pathDataset), "corners_9x7.txt").getPath();
                    generator.process();
                }
            } catch (RuntimeException e) {
                e.printStackTrace(errorLog);
                errorLog.println("Failed to render encoding: '" + encoding + "'");
                continue;
            }

            try {
                System.out.println("Detecting");
                final Class imageType = ImageDataType.typeToSingleClass(type);
                var detector = new DetectECoCheckImages<>(imageType);
                ConfigECoCheckMarkers configMarkers = ConfigECoCheckMarkers.parse(encoding, 1.0);
                detector.detector = FactoryFiducial.ecocheck(null, configMarkers, imageType).getDetector();
                detector.outputPath = new File(detectedBase, encoding);
                detector.detect(renderedOutput);

                if (detector.totalProcessed == 0) {
                    errorLog.println("Detected no images! encoding=" + encoding);
                }
            } catch (RuntimeException e) {
                e.printStackTrace(errorLog);
                errorLog.println("Failed when detecting encoding: '" + encoding + "'");
            }
        }

        System.out.println("Evaluating");
        var evaluator = new EvaluateMarkerLandmarkDetections();
        evaluator.out = new PrintStream(new File(directoryMetrics, "ACC_ECoCheck.txt"));
        evaluator.err = errorLog;
        BoofRegressionConstants.printGenerator(evaluator.out, getClass());
        evaluator.evaluateRecursive(detectedBase.getPath(), generatedBase.getPath());

        var runtime = new RuntimeSummary();
        runtime.initializeLog(directoryRuntime, getClass(), "RUN_ECoCheck.txt");
        runtime.out.println("default");
        runtime.printUnitsRow(false);

        // TODO compute runtime results
        List<String> keys = new ArrayList<>(evaluator.runtimeResults.keySet());
        Collections.sort(keys);

        DogArray_F64 combined = new DogArray_F64();
        for (String key : keys) {
            DogArray_F64 scenario = evaluator.runtimeResults.get(key);
            runtime.printStatsRow(key, scenario);
            combined.addAll(scenario);
        }
        runtime.out.println();
        runtime.saveSummary("summary", combined);

        runtime.printSummaryResults();
        runtime.out.close();
        evaluator.out.close();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{ECoCheckDetectionRegression.class.getName(), ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{ECoCheckDetectionRegression.class.getName(), ImageDataType.U8.toString()});
    }
}
