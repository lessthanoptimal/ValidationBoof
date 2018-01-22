package boofcv.metrics.sfm;

import boofcv.alg.geo.trifocal.TrifocalAlgebraicPoint7;
import boofcv.alg.geo.trifocal.TrifocalLinearPoint7;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ejml.data.DMatrixRMaj;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Process trifocal estimate test data and outputs different estimates of the tensor
 *
 * @author Peter Abeles
 */
public class ComputeTrifocalTensor {

	public static List<AssociatedTriple> readObservations( String file ) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));

		List<AssociatedTriple> ret = new ArrayList<AssociatedTriple>();

		String line;
		while( (line = reader.readLine()) != null ) {
			if( line.charAt(0) == '#' )
				continue;

			String a[] = line.split("\\s+");

			AssociatedTriple t = new AssociatedTriple();
			t.p1.x = Double.parseDouble(a[0]);
			t.p1.y = Double.parseDouble(a[1]);
			t.p2.x = Double.parseDouble(a[2]);
			t.p2.y = Double.parseDouble(a[3]);
			t.p3.x = Double.parseDouble(a[4]);
			t.p3.y = Double.parseDouble(a[5]);

			ret.add(t);
		}

		return ret;
	}

	public static void saveTrifocal( TrifocalTensor tensor , String fileName ) throws FileNotFoundException {
		PrintStream out = new PrintStream(fileName);

		out.println("# BoofCV results file for trifocal tensor estimate. Row-major 3x3 matrices.  T1,T2,T3");

		outputMatrix3x3(tensor.T1, out);
		outputMatrix3x3(tensor.T2, out);
		outputMatrix3x3(tensor.T3, out);

		out.close();
	}

	private static void outputMatrix3x3(DMatrixRMaj M, PrintStream out) {
		for( int i = 0; i < 3; i++ )
			out.printf("%.15f %.15f %.15f\n", M.get(i, 0), M.get(i, 1), M.get(i, 2));
		out.println();
	}

	public static void evaluate( List<AssociatedTriple> obs , String dataName ) throws FileNotFoundException {
		TrifocalLinearPoint7 linear7 = new TrifocalLinearPoint7();
		TrifocalTensor solution = new TrifocalTensor();
		linear7.process(obs,solution);

		saveTrifocal(solution, "boofcv_"+dataName+"_linear7.txt");

		UnconstrainedLeastSquares optimizer = FactoryOptimization.leastSquareLevenberg(1e-3);
		TrifocalAlgebraicPoint7 algebraic7 = new TrifocalAlgebraicPoint7(optimizer,300,1e-20,1e-20);
		algebraic7.process(obs,solution);

		saveTrifocal(solution, "boofcv_"+dataName+"_algebraic7.txt");
	}

	public static void main( String args[] ) throws IOException {
		List<AssociatedTriple> perfectObs = readObservations("../trifocal/tensor_pixel_perfect.txt");
		List<AssociatedTriple> noisyObs = readObservations("../trifocal/tensor_pixel_noise.txt");

		evaluate(perfectObs,"perfect");
		evaluate(noisyObs,"noise");
	}
}
