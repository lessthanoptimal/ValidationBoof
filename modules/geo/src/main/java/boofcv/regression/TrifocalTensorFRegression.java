package boofcv.regression;

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.common.*;
import boofcv.factory.geo.ConfigTrifocal;
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

    EvaluateTrifocal evaulator = new EvaluateTrifocal();
    File inputDirectory;
    File outputDirectory;
    PrintStream outputRuntime;

    public TrifocalTensorFRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process() throws IOException {
        // initialize directories
        inputDirectory = new File(SIMULATED_PATH);
        FileUtils.deleteDirectory(inputDirectory);
        outputDirectory = new File(SIMULATED_PATH+"/estimated");


        // performance output
        evaulator.out = new PrintStream( new File(directoryMetrics, "ACC_TrifocalTensor.txt") );
        BoofRegressionConstants.printGenerator(evaulator.out, getClass());
        evaulator.directoryObservations = new File(SIMULATED_PATH);
        evaulator.directoryResults = new File(SIMULATED_PATH+"/estimated");

        // set up runtime results file
        outputRuntime = new PrintStream(new File(directoryRuntime, "RUN_TrifocalTensor.txt"));
        BoofRegressionConstants.printGenerator(outputRuntime, getClass());
        outputRuntime.println("\n"+ValidationConstants.TARGET_OVERRIDE+"Milliseconds");
        outputRuntime.println("\nIndividual");
        outputRuntime.printf("%30s Milliseconds\n","");

        System.out.println("Generating trifocal data");
        new GenerateTrifocalObservations().initialize(SIMULATED_PATH).generate();

        System.out.println("Estimating trifocal");
        ConfigTrifocal configAlg7 = new ConfigTrifocal();
        configAlg7.which = EnumTrifocal.ALGEBRAIC_7;
        configAlg7.converge.maxIterations = 300;
        Estimate1ofTrifocalTensor alg = FactoryMultiView.trifocal_1(configAlg7);
        process(alg,"algebraic7");
        ConfigTrifocal configLinear7 = new ConfigTrifocal();
        configLinear7.which = EnumTrifocal.LINEAR_7;
        alg = FactoryMultiView.trifocal_1(configLinear7);
        process(alg,"linear7");

        evaulator.out.close();
        outputRuntime.close();

        System.out.println("Done trifocal");
    }

    private void process( Estimate1ofTrifocalTensor alg , String name ) throws IOException {
        inputDirectory = new File(SIMULATED_PATH);
        long time = ComputeTrifocalTensor.compute(inputDirectory, alg, name, outputDirectory);
        evaulator.evaluate(name);

        outputRuntime.printf("%30s %s\n",name, time);
    }

    public static void main(String[] args)
            throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        RegressionRunner.main(new String[]{TrifocalTensorFRegression.class.getName()});
    }
}
