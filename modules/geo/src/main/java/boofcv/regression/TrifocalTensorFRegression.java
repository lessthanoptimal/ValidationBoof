package boofcv.regression;

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FileRegression;
import boofcv.factory.geo.EnumTrifocal;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.metrics.sfm.ComputeTrifocalTensor;
import boofcv.metrics.sfm.EvaluateTrifocal;
import boofcv.metrics.sfm.GenerateTrifocalObservations;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class TrifocalTensorFRegression extends BaseRegression implements FileRegression {

    public static final String SIMULATED_PATH = "tmp/trifocal";

    public TrifocalTensorFRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process() throws IOException {
        File inputDirectory = new File(SIMULATED_PATH);
        FileUtils.deleteDirectory(inputDirectory);

        System.out.println("Generating trifocal data");
        new GenerateTrifocalObservations().initialize(SIMULATED_PATH).generate();

        File outputDirectory = new File(SIMULATED_PATH+"/estimated");

        System.out.println("Estimating trifocal");
        Estimate1ofTrifocalTensor alg = FactoryMultiView.trifocal_1(EnumTrifocal.ALGEBRAIC_7,300);
        ComputeTrifocalTensor.compute(inputDirectory, alg, "algebraic7", outputDirectory);

        alg = FactoryMultiView.trifocal_1(EnumTrifocal.LINEAR_7,300);
        ComputeTrifocalTensor.compute(inputDirectory,alg,"linear7",outputDirectory);

        EvaluateTrifocal evaulator = new EvaluateTrifocal();
        evaulator.out = new PrintStream( new File(directory, "TrifocalTensor.txt") );
        BoofRegressionConstants.printGenerator(evaulator.out, getClass());
        evaulator.directoryObservations = new File(SIMULATED_PATH);
        evaulator.directoryResults = new File(SIMULATED_PATH+"/estimated");

        evaulator.evaluate("algebraic7");
        evaulator.evaluate("linear7");

        evaulator.out.close();

        System.out.println("Done trifocal");
    }

    public static void main(String[] args) throws IOException {
        TrifocalTensorFRegression regression = new TrifocalTensorFRegression();
        regression.process();
    }
}
