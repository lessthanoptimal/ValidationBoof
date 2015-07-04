package validate.calib;

import boofcv.abst.calib.CalibrateMonoPlanar;
import boofcv.abst.calib.ImageResults;
import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang99;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.alg.geo.calibration.Zhang99Parameters;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Point2D_F64;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes calibration parameters using saved detected calibration points
 *
 * @author Peter Abeles
 */
public class CalibrateFromDetectedPoints {

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	public void processStereo( File stereoDetections ) throws IOException {
		PlanarCalibrationTarget targetDesc = FactoryPlanarCalibrationTarget.gridChess(5,7, 30);
		CalibrationPlanarGridZhang99 zhang99 = new CalibrationPlanarGridZhang99(targetDesc,true,2);

		List<List<Point2D_F64>> left = new ArrayList<List<Point2D_F64>>();
		List<List<Point2D_F64>> right = new ArrayList<List<Point2D_F64>>();

		loadObservations(stereoDetections, left, right);

		outputResults.println("=================================================================");
		outputResults.println("FILE: " + stereoDetections);
		outputResults.println("LEFT");
		Zhang99Parameters params = calibrate(zhang99, left);
		printErrors(params,left,targetDesc.points);
		outputResults.println();
		outputResults.println("RIGHT");
		params = calibrate(zhang99, right);
		printErrors(params, right, targetDesc.points);
	}

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

	public static void loadObservations( File file , List<List<Point2D_F64>> left , List<List<Point2D_F64>> right )
			throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line;
		while( (line = reader.readLine()) != null ) {
			List<Point2D_F64> target = new ArrayList<Point2D_F64>();

			String s[] = line.split(" ");
			String fileName = s[0];

			int N = Integer.parseInt(s[1]);
			for( int i = 0; i < N; i++ ) {
				float x = Float.parseFloat(s[i*2+2]);
				float y = Float.parseFloat(s[i*2+3]);
				target.add( new Point2D_F64(x,y));
			}

			if( fileName.contains("left"))
				left.add(target);
			else if( fileName.contains("right"))
				right.add(target);
			else
				throw new RuntimeException("Unknown");
		}
	}

	private Zhang99Parameters calibrate(CalibrationPlanarGridZhang99 zhang99, List<List<Point2D_F64>> observations )
			throws FileNotFoundException
	{
		if( !zhang99.process(observations) )
			throw new RuntimeException("Calibration failed!");

		// Get camera parameters and extrinsic target location in each image
		Zhang99Parameters found = zhang99.getOptimized();

		// Convenient function for converting from specialized Zhang99 format to generalized
		IntrinsicParameters param = found.convertToIntrinsic();

		// print the results to standard out
		param.print();

		outputResults.println("# Intrinsic matrix");
		outputResults.printf("%1.15f %1.15f %1.15f %1.15f %1.15f\n", found.a, found.b, found.c, found.x0, found.y0);
		outputResults.println("# Radial Distortion");
		outputResults.printf("%d", found.distortion.length);
		for( int i = 0; i < found.distortion.length; i++ )
			outputResults.printf(" %1.15f",found.distortion[i]);
		outputResults.println();
		outputResults.println(found.views.length);
		for( Zhang99Parameters.View v : found.views ) {
			double rx = v.rotation.unitAxisRotation.x * v.rotation.theta;
			double ry = v.rotation.unitAxisRotation.y * v.rotation.theta;
			double rz = v.rotation.unitAxisRotation.z * v.rotation.theta;

			outputResults.println("# Extrinsic");
			outputResults.printf("%1.15f %1.15f %1.15f %1.15f %1.15f %1.15f\n",rx,ry,rz,v.T.x,v.T.y,v.T.z);
		}
		return found;
	}

	private void printErrors( Zhang99Parameters param ,
							  List<List<Point2D_F64>> observations, List<Point2D_F64> grid )
	{
		List<ImageResults> results = CalibrateMonoPlanar.computeErrors(observations, param, grid);

		outputResults.println();
		outputResults.println("Errors");
		for (int i = 0; i < results.size(); i++) {
			ImageResults r = results.get(i);
			outputResults.printf("[%03d]  mean = %6f max = %6f\n",i,r.meanError,r.maxError);
		}
	}

	public static void main( String args[] ) throws IOException {
		CalibrateFromDetectedPoints app = new CalibrateFromDetectedPoints();

		app.processStereo(new File("data/calib/stereo/points/bumblebee2_chess.txt"));

	}
}
