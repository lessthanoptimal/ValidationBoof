package boofcv.regression;

import boofcv.common.*;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.generate.RenderDocumentViewsApp;
import boofcv.metrics.ecocheck.EvaluateMarkerLandmarkDetections;
import boofcv.metrics.qrcode.DetectMicroQrImages;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import org.apache.commons.io.FileUtils;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MicroQrDetectionRegression extends BaseRegression implements ImageRegression {

    String pathDataset = "data/fiducials/microqr";

    // directory used as temporary workspace
    File fileTmp = new File("tmp");


    public MicroQrDetectionRegression() {
        super(BoofRegressionConstants.TYPE_FIDCUIALS);
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        // Generate images
        var generatedBase = new File(pathDataset);
        var detectedBase = new File(fileTmp, "detected");

        if (!renderSyntheticImages(type, generatedBase, detectedBase))
            return;

        detect(type, generatedBase, detectedBase);

        System.out.println("Evaluating");
        var evaluator = new EvaluateMarkerLandmarkDetections();
        evaluator.out = new PrintStream(new File(directoryMetrics, "ACC_MicroQR.txt"));
        evaluator.err = errorLog;
        BoofRegressionConstants.printGenerator(evaluator.out, getClass());
        evaluator.evaluateRecursive(detectedBase.getPath(), generatedBase.getPath());

        var runtime = new RuntimeSummary();
        runtime.initializeLog(directoryRuntime, getClass(), "RUN_MicroQR.txt");
        runtime.out.println("default");
        runtime.printUnitsRow(false);

        List<String> keys = new ArrayList<>(evaluator.runtimeResults.keySet());
        Collections.sort(keys);

        var combined = new DogArray_F64();
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

    private <T extends ImageGray<T>> void detect(ImageDataType type, File generatedBase, File detectedBase) {
        System.out.println("Detecting");
        final Class<T> imageType = (Class)ImageDataType.typeToSingleClass(type);
        var detector = new DetectMicroQrImages<>(imageType);
        try {
            detector.detector = FactoryFiducial.microqr(null, imageType);
            detector.outputPath = detectedBase;
            detector.detect(generatedBase);

            if (detector.totalProcessed == 0) {
                errorLog.println("Detected no images!");
            }
        } catch (RuntimeException e) {
            e.printStackTrace(errorLog);
            String path;
            if (detector.imageBeingProcessed == null) {
                path = "none";
            } else {
                path = detector.imageBeingProcessed.getPath();
            }
            errorLog.println("Failed when detecting encoding: path='" + path + "'");
        }
    }

    private boolean renderSyntheticImages(ImageDataType type, File generatedBase, File detectedBase) {
        System.out.println("Rendering");
        var renderedOutput = new File(generatedBase, "synthetic");

        try {
            // Render the simulated data if it doesn't already exist
            if (!renderedOutput.exists() || FileUtils.isEmptyDirectory(renderedOutput)) {
                var generator = new RenderDocumentViewsApp();
                generator.inputFile = new File(generatedBase, "microqr.pdf").getPath();
                generator.destinationDir = renderedOutput.getPath();
                generator.landmarksFile = new File(generatedBase, "corners.txt").getPath();
                generator.process();
            }
        } catch (Exception e) {
            e.printStackTrace(errorLog);
            errorLog.println("Failed to render");
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{MicroQrDetectionRegression.class.getName(), ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{MicroQrDetectionRegression.class.getName(), ImageDataType.U8.toString()});
    }
}
