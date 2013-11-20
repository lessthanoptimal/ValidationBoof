package validate.calib;

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
 * @author Peter Abeles
 */
public class CalibrateCameraPoints {


	public static List<List<Point2D_F64>> loadObservations( String file ) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(file));

		List<List<Point2D_F64>> ret = new ArrayList<List<Point2D_F64>>();

		String line;
		while( (line = reader.readLine()) != null ) {
			List<Point2D_F64> target = new ArrayList<Point2D_F64>();

			String s[] = line.split(" ");
			int N = Integer.parseInt(s[0]);

			for( int i = 0; i < N; i++ ) {
				float x = Float.parseFloat(s[i*2+1]);
				float y = Float.parseFloat(s[i*2+2]);
				target.add( new Point2D_F64(x,y));
			}
			ret.add(target);
		}

		return ret;
	}

	public static void main( String args[] ) throws IOException {
		// todo load file a file instead
		PlanarCalibrationTarget targetDesc = FactoryPlanarCalibrationTarget.gridChess(5,7, 30);
		CalibrationPlanarGridZhang99 zhang99 =
				new CalibrationPlanarGridZhang99(targetDesc,true,2);

		List<List<Point2D_F64>>  observations = loadObservations("../results/calib/points_boofcv_chess_bumblebee2_left.txt");
//		List<List<Point2D_F64>>  observations = loadObservations("../results/calib/points_opencv_chess_bumblebee2_left.txt");


		if( !zhang99.process(observations) )
			throw new RuntimeException("Calibration failed!");

		// Get camera parameters and extrinsic target location in each image
		Zhang99Parameters found = zhang99.getOptimized();

		// Convenient function for converting from specialized Zhang99 format to generalized
		IntrinsicParameters param = found.convertToIntrinsic();

		// print the results to standard out
		param.print();

		PrintStream writer = new PrintStream("boofcv_calib.txt");

		writer.printf("%1.15f %1.15f %1.15f %1.15f %1.15f\n",found.a,found.b,found.c,found.x0,found.y0);
		writer.printf("%d",found.distortion.length);
		for( int i = 0; i < found.distortion.length; i++ )
			writer.printf(" %1.15f",found.distortion[i]);
		writer.println();
		writer.println(found.views.length);
		for( Zhang99Parameters.View v : found.views ) {
			double rx = v.rotation.unitAxisRotation.x * v.rotation.theta;
			double ry = v.rotation.unitAxisRotation.y * v.rotation.theta;
			double rz = v.rotation.unitAxisRotation.z * v.rotation.theta;

			writer.printf("%1.15f %1.15f %1.15f %1.15f %1.15f %1.15f\n",rx,ry,rz,v.T.x,v.T.y,v.T.z);
		}
		writer.close();
	}
}
