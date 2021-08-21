package boofcv.regression;

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.common.*;
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

    String pathCalibration = "data/calibration_mono/ecocheck";

    // directory used as temporary workspace
    File fileTmp = new File("tmp");


    public ECoCheckDetectionRegression() {
        super(BoofRegressionConstants.TYPE_CALIBRATION);
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        // Generate images
        File generatedBase = new File(fileTmp, "generated");
        File detectedBase = new File(fileTmp, "detected");

        for ( String encoding : new String[]{"9x7e3n1","9x7e0n1"}) {
            System.out.println("Rendering");
            var generator = new RenderDocumentViewsApp();
            generator.inputFile = new File(new File(pathCalibration), "ecocheck_" + encoding + ".pdf").getPath();
            generator.destinationDir = new File(generatedBase, encoding).getPath();
            generator.landmarksFile = new File(new File(pathCalibration), "corners_9x7.txt").getPath();
            generator.process();

            System.out.println("Detecting");
            ConfigECoCheckMarkers configMarkers = ConfigECoCheckMarkers.parse(encoding, 1.0);
            final Class imageType = ImageDataType.typeToSingleClass(type);
            DetectECoCheckImages<?> detector = new DetectECoCheckImages<>(configMarkers, imageType);
            detector.outputPath = new File(detectedBase, encoding);
            detector.detect(new File(generator.destinationDir));
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
        RegressionRunner.main(new String[]{ECoCheckDetectionRegression.class.getName(),ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{ECoCheckDetectionRegression.class.getName(),ImageDataType.U8.toString()});
    }
}
