package boofcv.regression;

import boofcv.abst.disparity.StereoDisparity;
import boofcv.common.*;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.io.UtilIO;
import boofcv.metrics.mvs.DisparitySequenceConsistency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Evaluates how consistent the surface model is from stereo disparity as camera moves over a surface.
 *
 * @author Peter Abeles
 */
public class StereoSequenceConsistencyRegression extends BaseRegression implements ImageRegression {
    public static final String PATH_SEQUENCES = "data/mvs/stereo_consistency";

    RuntimeSummary outputRuntime;

    public StereoSequenceConsistencyRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        outputRuntime = new RuntimeSummary();
        outputRuntime.initializeLog(directoryRuntime, getClass(), "RUN_StereoSequenceConsistency.txt");
        outputRuntime.out.println("Scenarios");
        outputRuntime.printUnitsRow(false);

        var out = new PrintStream(new File(directoryMetrics, "ACC_StereoSequenceConsistency.txt"));
        BoofRegressionConstants.printGenerator(out, getClass());

        var config = new ConfigDisparityBMBest5();
        config.disparityRange = 250;
        config.regionRadiusX = config.regionRadiusY = 8;

        StereoDisparity<GrayF32, GrayF32> stereoDisparity = FactoryStereoDisparity.blockMatchBest5(config, GrayF32.class, GrayF32.class);

        var metrics = new DisparitySequenceConsistency<>(stereoDisparity);
        metrics.err = errorLog;
        metrics.out = out;
        metrics.showVisuals = false;
        boolean success = false;

        for (File child : UtilIO.listFilesSorted(new File(PATH_SEQUENCES))) {
            if (!child.exists() || !child.isDirectory())
                continue;

            try {
                out.println("Scenario: " + child.getName());
                out.println();
                metrics.process(child);
                outputRuntime.printStatsRow(child.getName(), metrics.timingMS);
                success = true;
            } catch (Exception e) {
                e.printStackTrace(errorLog);
            }
        }

        if (!success)
            errorLog.println("No datasets processed");

        out.close();
        outputRuntime.out.close();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{StereoSequenceConsistencyRegression.class.getName(), ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{StereoSequenceConsistencyRegression.class.getName(), ImageDataType.U8.toString()});
    }
}
