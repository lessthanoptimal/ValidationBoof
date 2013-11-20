package validate.calib;

import boofcv.abst.calib.CalibrateMonoPlanar;
import boofcv.abst.calib.ImageResults;
import boofcv.alg.geo.calibration.Zhang99Parameters;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import georegression.struct.point.Point2D_F64;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ComputeErrorStatistics {

	public static Zhang99Parameters loadCalibration(String fileName) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(fileName));

		String s[] = reader.readLine().split(" ");

		Zhang99Parameters ret = new Zhang99Parameters();
		ret.a = Double.parseDouble(s[0]);
		ret.b = Double.parseDouble(s[1]);
		ret.c = Double.parseDouble(s[2]);
		ret.x0 = Double.parseDouble(s[3]);
		ret.y0 = Double.parseDouble(s[4]);

		s = reader.readLine().split(" ");
		ret.distortion = new double[ Integer.parseInt(s[0])];
		for( int i = 0; i < ret.distortion.length; i++ ) {
			ret.distortion[i] = Double.parseDouble(s[i+1]);
		}

		int N = Integer.parseInt(reader.readLine());
		ret.views = new Zhang99Parameters.View[N];

		for( int i = 0; i < N; i++ ) {
			s = reader.readLine().split(" ");

			double rx = Double.parseDouble(s[0]);
			double ry = Double.parseDouble(s[1]);
			double rz = Double.parseDouble(s[2]);
			double tx = Double.parseDouble(s[3]);
			double ty = Double.parseDouble(s[4]);
			double tz = Double.parseDouble(s[5]);

			Zhang99Parameters.View v = new Zhang99Parameters.View();

			v.rotation.setParamVector(rx,ry,rz);
			v.T.set(tx,ty,tz);

			ret.views[i] = v;
		}

		return ret;
	}

	public static void main( String args[] ) throws IOException {
//		String fileObservation = "../results/calib/points_boofcv_chess_bumblebee2_left.txt";
		String fileObservation = "../results/calib/points_opencv_chess_bumblebee2_left.txt";
		String fileResults = "../results/calib/calib_boofcv_chess_bumblebee2_left.txt";

		List<List<Point2D_F64>> obs = CalibrateCameraPoints.loadObservations(fileObservation);
		Zhang99Parameters param = loadCalibration(fileResults);
		List<Point2D_F64> grid = FactoryPlanarCalibrationTarget.gridChess(5,7,30).points;  // todo replace with file load

		List<ImageResults> results = CalibrateMonoPlanar.computeErrors(obs, param, grid);

		CalibrateMonoPlanar.printErrors(results);
	}
}
