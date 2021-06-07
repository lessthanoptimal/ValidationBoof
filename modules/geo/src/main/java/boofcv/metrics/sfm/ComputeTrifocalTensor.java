package boofcv.metrics.sfm;

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.factory.geo.ConfigTrifocal;
import boofcv.factory.geo.EnumTrifocal;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
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


	public static List<AssociatedTriple> readObservations( File file ) throws IOException {
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

	public static void saveTrifocal( TrifocalTensor tensor , File outputPath ) throws FileNotFoundException {
		PrintStream out = new PrintStream(outputPath);

		out.println("# BoofCV results file for trifocal tensor estimate. Row-major 3x3 matrices. T1,T2,T3");

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

	public static long compute(File inputDirectory , Estimate1ofTrifocalTensor alg , String algName ,
							   File outputDirectory ) throws IOException
	{
		if( !outputDirectory.exists() )
			if( !outputDirectory.mkdirs() )
				throw new RuntimeException("Can't create output directory");

		long totalTime = 0;
		TrifocalTensor solution = new TrifocalTensor();
		for( File f : inputDirectory.listFiles() ) {
			if( !f.isFile() || !f.getName().startsWith("tensor_pixel")) {
				continue;
			}
			List<AssociatedTriple> obs = readObservations(f);

			long before = System.currentTimeMillis();
			alg.process(obs,solution);
			long after = System.currentTimeMillis();
			totalTime += after-before;

			String outputName = algName+"_"+f.getName();
			saveTrifocal(solution, new File(outputDirectory,outputName));
		}

		return totalTime;
	}

	public static void main( String args[] ) throws IOException {
		File inputDirectory = new File("trifocal");
		File outputDirectory = new File("trifocal/results");

		ConfigTrifocal configAlg7 = new ConfigTrifocal();
		configAlg7.which = EnumTrifocal.ALGEBRAIC_7;
		configAlg7.converge.maxIterations = 300;
		Estimate1ofTrifocalTensor alg = FactoryMultiView.trifocal_1(configAlg7);
		compute(inputDirectory,alg,"algebraic7",outputDirectory);

		ConfigTrifocal configLinear7 = new ConfigTrifocal();
		configAlg7.which = EnumTrifocal.LINEAR_7;
		alg = FactoryMultiView.trifocal_1(configLinear7);
		compute(inputDirectory,alg,"linear7",outputDirectory);
	}
}
