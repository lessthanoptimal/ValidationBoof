package boofcv.regression;

import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.DiscreteRange;
import boofcv.common.FileRegression;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.metrics.sfm.EvaluatePnPObservations;
import boofcv.metrics.sfm.GeneratePnPObservation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Evaluates bundle adjustment by seeing how much the residuals are reduced.
 *
 * @author Peter Abeles
 */
public class PoseNPointFRegression extends BaseRegression implements FileRegression {

    public static final String SIMULATED_PATH = "tmp/pnp";

    public PoseNPointFRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process() throws IOException {
        evaluatePlanerP4P("ACC_Planar_P4P.txt");

    }

    private void evaluatePlanerP4P( String name ) throws IOException {
        // Generate simulated test data
        GeneratePnPObservation generator = new GeneratePnPObservation(60,1024,768);
        generator.initialize(1.0,SIMULATED_PATH);
        generator.targetSquare(0.2);
        generator.generateUniformImageDiscreteDistances(new DiscreteRange(1,10,20),45,4000);
        generator.generateUniformImageDiscreteAngles(new DiscreteRange(0,45,10),1.5,4000);

        PrintStream out = new PrintStream( new File(directory, name) );

        out.println("Planar square target. Pixel Stdev = "+generator.getStdevPixel());
        out.println("    range tests: distance = 1 to 10  max_tilt = 45 degrees");
        out.println("    angle tests: distance = 1.5      angle    = 0 to 45 degrees");
        out.println();
        EvaluatePnPObservations evalutor = new EvaluatePnPObservations();
        evalutor.setErr(errorLog);
        evalutor.setOut(out);

        out.println("GRUNERT");
        evalutor.printHeader();
        evalutor.evaluate(new File(SIMULATED_PATH), FactoryMultiView.pnp_1(EnumPNP.P3P_GRUNERT,-1,1));
        out.println();

        out.println("FINSTERWALDER");
        evalutor.printHeader();
        evalutor.evaluate(new File(SIMULATED_PATH), FactoryMultiView.pnp_1(EnumPNP.P3P_FINSTERWALDER,-1,1));
        out.println();

        out.println("EPNP");
        evalutor.printHeader();
        evalutor.evaluate(new File(SIMULATED_PATH), FactoryMultiView.computePnPwithEPnP(10,0.1));
        out.println();

        out.println("IPPE");
        evalutor.printHeader();
        evalutor.evaluate(new File(SIMULATED_PATH), FactoryMultiView.pnp_1(EnumPNP.IPPE,-1,0));
        out.println();
    }

    public static void main(String[] args) throws IOException {
        PoseNPointFRegression regression = new PoseNPointFRegression();
        regression.process();
    }
}
