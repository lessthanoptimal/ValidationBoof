package boofcv.metrics.sfm;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Random;

/**
 * Creates synthetic data used to evaluate trifocal estimating algorithms
 *
 * @author Peter Abeles
 */
public class GenerateTrifocalObservations {

	private static void printCameraMatrix(DMatrixRMaj p1, PrintStream outCamera) {
		for( int i = 0; i < 3; i++ ) {
			outCamera.printf("%.10f %.10f %.10f %.10f\n", p1.get(i,0), p1.get(i,1), p1.get(i,2), p1.get(i,3));
		}
		outCamera.println();
	}

	public static void main( String args[] ) throws FileNotFoundException {
		Random rand = new Random(234);

		// camera calibration matrix
		DMatrixRMaj K = new DMatrixRMaj(3,3,true,60,0.01,320,0,80,250,0,0,1);

		Se3_F64 se2 = new Se3_F64();
		Se3_F64 se3 = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
				rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1, -rand.nextGaussian() * 0.1, se2.R);
		se2.getT().set(0.3,0,0.05);

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
				rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, -rand.nextGaussian()*0.1, se3.R);
		se3.getT().set(0.6, 0.2, -0.02);

		// While not technically needed, save the camera matrices
		DMatrixRMaj P1 = PerspectiveOps.createCameraMatrix(CommonOps_DDRM.identity(3), new Vector3D_F64(), K, null);
		DMatrixRMaj P2 = PerspectiveOps.createCameraMatrix(se2.R, se2.T, K, null);
		DMatrixRMaj P3 = PerspectiveOps.createCameraMatrix(se3.R, se3.T, K, null);

		PrintStream outCamera = new PrintStream("camera_matrix.txt");
		outCamera.println("# Trifocal Test: Camera matrices 3x4 in row major order.  Camera 1,2,3");
		printCameraMatrix(P1, outCamera);
		printCameraMatrix(P2, outCamera);
		printCameraMatrix(P3, outCamera);
		outCamera.close();

		// output the location of points in the world and their observed location in each camera
		PrintStream outWorld = new PrintStream("tensor_world.txt");
		PrintStream outPixel = new PrintStream("tensor_pixel_perfect.txt");
		PrintStream outNoise = new PrintStream("tensor_pixel_noise.txt");

		double noiseSigma = 0.5;

		outWorld.println("# Trifocal Test: Location of world points");
		outPixel.println("# Trifocal Test: Observed pixel locations of each 3D point in each image.  Noise Free,  Order = first, second, third camera. (x,y)");
		outNoise.println("# Trifocal Test: Observed pixel locations with " + noiseSigma + " sigma noise added to each axis.");

		for( int i = 0; i < 20; i++ ) {
			Point3D_F64 p = new Point3D_F64();
			p.x = rand.nextGaussian()*0.5;
			p.y = rand.nextGaussian()*0.5;
			p.z = rand.nextGaussian()*0.5 + 2;

			outWorld.printf("%.10f %.10f %.10f\n",p.x,p.y,p.z);

			AssociatedTriple o = new AssociatedTriple();
			o.p1 = PerspectiveOps.renderPixel(new Se3_F64(), K, p);
			o.p2 = PerspectiveOps.renderPixel(se2,K,p);
			o.p3 = PerspectiveOps.renderPixel(se3,K,p);

			outPixel.printf("%.10f %.10f %.10f %.10f %.10f %.10f\n",o.p1.x,o.p1.y,o.p2.x,o.p2.y,o.p3.x,o.p3.y);

			o.p1.x += rand.nextGaussian()*noiseSigma;
			o.p1.y += rand.nextGaussian()*noiseSigma;
			o.p2.x += rand.nextGaussian()*noiseSigma;
			o.p2.y += rand.nextGaussian()*noiseSigma;
			o.p3.x += rand.nextGaussian()*noiseSigma;
			o.p3.y += rand.nextGaussian()*noiseSigma;

			outNoise.printf("%.10f %.10f %.10f %.10f %.10f %.10f\n",o.p1.x,o.p1.y,o.p2.x,o.p2.y,o.p3.x,o.p3.y);

		}

		outWorld.close();
		outPixel.close();
	}
}
