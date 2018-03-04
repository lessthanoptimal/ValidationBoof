package boofcv.metrics;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang99;
import boofcv.alg.geo.calibration.Zhang99AllParam;
import boofcv.alg.geo.calibration.pinhole.CalibParamPinholeRadial;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.calib.CameraPinholeRadial;
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

	public void processStereo( File stereoDetections , boolean tangential ) throws IOException {
		DetectorFiducialCalibration targetDesc = FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5, 30));
		CalibrationPlanarGridZhang99 zhang99 = new CalibrationPlanarGridZhang99(targetDesc.getLayout(),
				new CalibParamPinholeRadial(true,2,tangential));

		List<CalibrationObservation> left = new ArrayList<CalibrationObservation>();
		List<CalibrationObservation> right = new ArrayList<CalibrationObservation>();

		loadObservations(stereoDetections, left, right);

		outputResults.println("=================================================================");
		outputResults.println("FILE: " + stereoDetections);
		outputResults.println("LEFT");
		Zhang99AllParam params = calibrate(zhang99, left);
		printErrors(params,left,targetDesc.getLayout());
		outputResults.println();
		outputResults.println("RIGHT");
		params = calibrate(zhang99, right);
		printErrors(params, right, targetDesc.getLayout());
	}

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;

	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

	public static void loadObservations( File file , List<CalibrationObservation> left , List<CalibrationObservation> right )
			throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line;
		while( (line = reader.readLine()) != null ) {
			CalibrationObservation target = new CalibrationObservation(0,0);

			String s[] = line.split(" ");
			String fileName = s[0];

			int N = Integer.parseInt(s[1]);
			for( int i = 0; i < N; i++ ) {
				float x = Float.parseFloat(s[i*2+2]);
				float y = Float.parseFloat(s[i*2+3]);
				target.add(new Point2D_F64(x, y), i);
			}

			if( fileName.contains("left"))
				left.add(target);
			else if( fileName.contains("right"))
				right.add(target);
			else
				throw new RuntimeException("Unknown");
		}
	}

	private Zhang99AllParam calibrate(CalibrationPlanarGridZhang99 zhang99, List<CalibrationObservation> observations )
			throws FileNotFoundException
	{
		if( !zhang99.process(observations) )
			throw new RuntimeException("Calibration failed!");

		// Get camera parameters and extrinsic target location in each image
		Zhang99AllParam found = zhang99.getOptimized();

		// Convenient function for converting from specialized Zhang99 format to generalized
		CameraPinholeRadial param = found.getIntrinsic().getCameraModel();

		// print the results to standard out
//		param.print();

		outputResults.println("# Intrinsic matrix");
		outputResults.printf("%1.15f %1.15f %1.15f %1.15f %1.15f\n", param.fx, param.fy, param.skew, param.cx, param.cy);
		outputResults.println("# Radial Distortion");
		outputResults.printf("%d", param.radial.length);
		for( int i = 0; i < param.radial.length; i++ )
			outputResults.printf(" %1.15f",param.radial[i]);
		outputResults.println();
		outputResults.println("# Tangential Distortion");
		outputResults.printf("%1.15f %1.15f\n",param.t1,param.t2);
		outputResults.println(found.views.length);
		for( Zhang99AllParam.View v : found.views ) {
			double rx = v.rotation.unitAxisRotation.x * v.rotation.theta;
			double ry = v.rotation.unitAxisRotation.y * v.rotation.theta;
			double rz = v.rotation.unitAxisRotation.z * v.rotation.theta;

			outputResults.println("# Extrinsic");
			outputResults.printf("%1.15f %1.15f %1.15f %1.15f %1.15f %1.15f\n",rx,ry,rz,v.T.x,v.T.y,v.T.z);
		}
		return found;
	}

	private void printErrors( Zhang99AllParam param ,
							  List<CalibrationObservation> observations, List<Point2D_F64> grid )
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

		app.processStereo(new File("data/calib/stereo/points/bumblebee2_chess.txt"),false);

	}
}
