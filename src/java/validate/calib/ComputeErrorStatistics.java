package validate.calib;

import boofcv.alg.geo.calibration.Zhang99Parameters;
import georegression.struct.point.Point2D_F64;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ComputeErrorStatistics {

	public static List<Point2D_F64> loadCalibPoints( String fileName ) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fileName));

		int num = Integer.parseInt(reader.readLine());

		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		for( int i = 0; i < num; i++ ) {
			String words[] = reader.readLine().split(" ");
			double x = Double.parseDouble(words[0]);
			double y = Double.parseDouble(words[1]);

			ret.add( new Point2D_F64(x,y));
		}

		return ret;
	}

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
//		String fileCalib = "../results/calib/calib_boofcv_pts_boofcv_chess_bumblebee2_left.txt";
//		String fileCalib = "../results/calib/calib_opencv_pts_boofcv_chess_bumblebee2_left.txt";
		String fileCalib = "../results/calib/calib_opencv_pts_opencv_chess_bumblebee2_left.txt";
		String fileCalibPoints = "../results/calib/chess.txt";

//		List<List<Point2D_F64>> obs = CalibrateCameraPoints.loadObservations(fileObservation);
//		Zhang99Parameters param = loadCalibration(fileCalib);
//		List<Point2D_F64> grid = loadCalibPoints(fileCalibPoints);
//
//		List<ImageResults> results = CalibrateMonoPlanar.computeErrors(obs, param, grid);
//
//		CalibrateMonoPlanar.printErrors(results);
	}
}
