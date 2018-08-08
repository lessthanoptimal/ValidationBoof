package boofcv.regression;

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.BundleAdjustmentSchur_DSCC;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FileRegression;
import boofcv.metrics.sba.CodecBundleAdjustmentInTheLarge;
import boofcv.misc.BoofMiscOps;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import static boofcv.metrics.sba.BundleAdjustmentEvaluationTools.computeReprojectionErrorMetrics;

/**
 * Evaluates bundle adjustment by seeing how much the residuals are reduced.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentFRegression extends BaseRegression implements FileRegression {

    // A smaller subset of the datasets is used here to ensure the regression can run quickly.
    String[] datasets = new String[] {
            "data/bundle_adjustment/dubrovnik/problem-16-22106-pre.txt",
            "data/bundle_adjustment/final/problem-93-61203-pre.txt",
            "data/bundle_adjustment/ladybug/problem-49-7776-pre.txt",
            "data/bundle_adjustment/trafalgar/problem-21-11315-pre.txt"
    };

    PrintStream outputRuntime;
    PrintStream outputQuality;

    double ftol=1e-6,gtol=1e-6;
    int maxIterations = 100;

    CodecBundleAdjustmentInTheLarge parser = new CodecBundleAdjustmentInTheLarge();

    public BundleAdjustmentFRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process() throws IOException {
        ConfigLevenbergMarquardt config = new ConfigLevenbergMarquardt();
        config.dampeningInitial = 0.1;
        config.scalingMinimum = 1e-6;
        config.scalingMinimum = 1e6;
        evaluate(new BundleAdjustmentSchur_DSCC(config),"Schur_DSCC");
    }

    private void evaluate(BundleAdjustment bundleAdjustment , String algorithm ) throws FileNotFoundException {
        bundleAdjustment.setVerbose(true);
        System.out.println("BundleAdjustment Evaluating "+algorithm);
        outputQuality = new PrintStream( new File(directory, "ACC_BundleAdjustment_"+algorithm+".txt"));
        BoofRegressionConstants.printGenerator(outputQuality, getClass());
        outputQuality.println("# Bundle Adjustment "+algorithm);
        outputQuality.println("# ftol="+ftol+"  gtol="+gtol+"  max_iterations="+maxIterations);
        outputQuality.flush();

        outputRuntime = new PrintStream(new File(directory, "RUN_BundleAdjustment_"+algorithm+".txt"));
        BoofRegressionConstants.printGenerator(outputRuntime, getClass());
        outputRuntime.flush();

        for (String path : datasets) {
            try {
                evauluate(bundleAdjustment, new File(path));
            } catch ( Exception e ) {
                e.printStackTrace(errorLog);
            }
        }
    }

    protected void evauluate( BundleAdjustment bundleAdjustment , File f ) throws IOException {
        parser.parse(f);

        bundleAdjustment.configure(ftol, gtol, maxIterations);

        double errorsBefore[] = computeReprojectionErrorMetrics(parser.scene, parser.observations);

        String path = new File(f.getParentFile().getName(), f.getName()).getPath();

        System.out.println(path + " Views=" + parser.scene.views.length + "  Obs=" + parser.observations.getObservationCount());
        outputQuality.printf("%s before p50=%f p95=%f\n", path, errorsBefore[0], errorsBefore[1]);
        outputQuality.flush();

        long startTime = System.currentTimeMillis();

        boolean success = bundleAdjustment.optimize(parser.scene, parser.observations);

        long stopTime = System.currentTimeMillis();


        outputRuntime.printf("%s %s\n", path, BoofMiscOps.milliToHuman(stopTime - startTime));
        outputRuntime.flush();


        System.out.println("Elapsed Time: " + BoofMiscOps.milliToHuman(stopTime - startTime));

        if (!success)
            outputQuality.printf("%s after FAILED\n", path);
        else {
            double errorsAfter[] = computeReprojectionErrorMetrics(parser.scene, parser.observations);
            outputQuality.printf("%s after p50=%f p95=%f\n", path, errorsAfter[0], errorsAfter[1]);
        }
        outputQuality.flush();
    }
}
