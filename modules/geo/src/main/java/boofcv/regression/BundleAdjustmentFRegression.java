package boofcv.regression;

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.ScaleSceneStructure;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FileRegression;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.io.geo.CodecBundleAdjustmentInTheLarge;
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
        ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
        configLM.mixture = 0.99;
        configLM.dampeningInitial = 1e-4;
        configLM.hessianScaling = true;
        ConfigBundleAdjustment configBA = new ConfigBundleAdjustment();
        configBA.configOptimizer = configLM;
        BundleAdjustment<SceneStructureMetric> sba = FactoryMultiView.bundleAdjustmentMetric(configBA);
        evaluate(sba,"SchurLM_DSCC");

        // matrix is almost never positive definite. very slow convergence. working on this
//        ConfigTrustRegion configDL = new ConfigTrustRegion();
//        configDL.regionInitial = 100;
//        configDL.hessianScaling = true;
//        evaluate(new BundleAdjustmentSchur_DSCC(configDL),"SchurDogleg_DSCC");
    }

    private void evaluate(BundleAdjustment<SceneStructureMetric> bundleAdjustment , String algorithm ) throws FileNotFoundException {
        bundleAdjustment.setVerbose(System.out,1);
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

    protected void evauluate( BundleAdjustment<SceneStructureMetric> bundleAdjustment , File f ) throws IOException {
        parser.parse(f);

        bundleAdjustment.configure(ftol, gtol, maxIterations);

        double errorsBefore[] = computeReprojectionErrorMetrics(parser.scene, parser.observations);

        String path = new File(f.getParentFile().getName(), f.getName()).getPath();

        System.out.println(path + " Views=" + parser.scene.views.length + "  Obs=" + parser.observations.getObservationCount());

        long startTime = System.currentTimeMillis();

        ScaleSceneStructure bundleScale = new ScaleSceneStructure();
        bundleScale.applyScale(parser.scene, parser.observations);
        bundleAdjustment.setParameters(parser.scene, parser.observations);

        outputQuality.printf("%-45s before fx=%-5.2e p50=%-7.4f p95=%-7.4f views=%-6d obs=%-8d\n",
                path, bundleAdjustment.getFitScore(), errorsBefore[0], errorsBefore[1],
                parser.scene.views.length,parser.observations.getObservationCount());
        outputQuality.flush();

        boolean success = bundleAdjustment.optimize(parser.scene);
        bundleScale.undoScale(parser.scene, parser.observations);

        long stopTime = System.currentTimeMillis();


        outputRuntime.printf("%-45s %s\n", path, BoofMiscOps.milliToHuman(stopTime - startTime));
        outputRuntime.flush();


        System.out.println("Elapsed Time: " + BoofMiscOps.milliToHuman(stopTime - startTime));

        if (!success)
            outputQuality.printf("%s after FAILED\n", path);
        else {
            double errorsAfter[] = computeReprojectionErrorMetrics(parser.scene, parser.observations);
            outputQuality.printf("%-45s after  fx=%-5.2e p50=%-7.4f p95=%-7.4f views=%-6d obs=%-8d\n",
                    path, bundleAdjustment.getFitScore(), errorsAfter[0], errorsAfter[1],
                    parser.scene.views.length,parser.observations.getObservationCount());
        }
        outputQuality.flush();
    }

    public static void main(String[] args) throws IOException {
        BundleAdjustmentFRegression regression = new BundleAdjustmentFRegression();
        regression.process();
    }
}
