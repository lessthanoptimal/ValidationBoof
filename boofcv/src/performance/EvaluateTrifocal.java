package performance;

import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.SpecializedOps;
import validation.ComputeTrifocalTensor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the quality of the trifocal tensor estimate
 *
 * @author Peter Abeles
 */
public class EvaluateTrifocal {

	public static TrifocalTensor readTensor( File file ) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));

		List<double[]> data = new ArrayList<double[]>();

		String line;
		while( (line = reader.readLine()) != null ) {
			if( line.length() == 0 || line.charAt(0) == '#' )
				continue;

			String a[] = line.split("\\s+");

			double d[] = new double[3];
			d[0] = Double.parseDouble(a[0]);
			d[1] = Double.parseDouble(a[1]);
			d[2] = Double.parseDouble(a[2]);

			data.add(d);
		}

		TrifocalTensor tensor = new TrifocalTensor();

		for( int i = 0; i < 3; i++ ) {
			int row = i*3;

			DenseMatrix64F T = tensor.getT(i);

			for( int j = 0; j < 3; j++ ) {
				T.data[j] = data.get(row)[j];
				T.data[j+3] = data.get(row+1)[j];
				T.data[j+6] = data.get(row+2)[j];
			}
		}

		return tensor;
	}

	public static double score( TrifocalTensor tensor , List<AssociatedTriple> obs )
	{
		// adjust the scale so that there is no advantage there
		tensor.normalizeScale();

		double total = 0;
		for( AssociatedTriple o : obs ) {
			DenseMatrix64F m = MultiViewOps.constraint(tensor,o.p1,o.p2,o.p3,null);
			double score = SpecializedOps.elementSumSq(m);

			total += score;
		}

		total /= obs.size();

		return total;
	}

	public static void main( String args[] ) throws IOException {
		String path = "../trifocal";

		List<AssociatedTriple> noiseObs = ComputeTrifocalTensor.readObservations(path+"/tensor_pixel_noise.txt");
		List<AssociatedTriple> perfectObs = ComputeTrifocalTensor.readObservations(path+"/tensor_pixel_perfect.txt");

		File files[] = new File(path+"/results").listFiles();

		for( File f : files ) {
			if( !f.isFile() || !f.getName().endsWith(".txt"))
				continue;

			TrifocalTensor tensor = readTensor(f);

			if( f.getName().contains("noise") ) {
				System.out.printf("%40s score = %15.4f\n", f.getName(), score(tensor, noiseObs));
			} else {
				System.out.printf("%40s score = %15.4f\n",f.getName(),score(tensor,perfectObs));
			}
		}
	}
}
