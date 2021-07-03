package boofcv.regression;

import boofcv.common.*;
import boofcv.factory.structure.ConfigEpipolarScore3D;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.metrics.reconstruction.EvaluateEpipolarScore3D;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Evaluates implementations of {@link boofcv.alg.structure.EpipolarScore3D}.
 *
 * @author Peter Abeles
 */
public class EvaluateEpipolarScore3DFRegression extends BaseRegression implements FileRegression {

    public EvaluateEpipolarScore3DFRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    EvaluateEpipolarScore3D evaluator = new EvaluateEpipolarScore3D();
    RuntimeSummary outputRuntime;

    @Override
    public void process() throws IOException {
        outputRuntime = new RuntimeSummary();
        outputRuntime.initializeLog(directoryRuntime, getClass(), "RUN_EpipolarScore3D.txt");
        outputRuntime.printUnitsRow(true);

        evaluate("FundamentalRotation", ConfigEpipolarScore3D.Type.FUNDAMENTAL_ROTATION);
        evaluate("FundamentalCompatible", ConfigEpipolarScore3D.Type.FUNDAMENTAL_COMPATIBLE);
        evaluate("ModelInliers", ConfigEpipolarScore3D.Type.MODEL_INLIERS);

        outputRuntime.out.close();
    }

    private void evaluate(String name, ConfigEpipolarScore3D.Type type) throws IOException {
        ConfigEpipolarScore3D config = new ConfigEpipolarScore3D();
        config.type = type;

        try (PrintStream out = new PrintStream(new File(directoryMetrics, "ACC_EpipolarScore3D_" + name + ".txt"))) {
            BoofRegressionConstants.printGenerator(out, getClass());

            evaluator.score3D = FactorySceneReconstruction.epipolarScore3D(config);
            evaluator.metricsLog = out;
            evaluator.errorLog = errorLog;
            evaluator.evaluate();

            outputRuntime.printStatsRow(name, evaluator.timingMS);
        }
    }

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{EvaluateEpipolarScore3DFRegression.class.getName()});
    }
}
