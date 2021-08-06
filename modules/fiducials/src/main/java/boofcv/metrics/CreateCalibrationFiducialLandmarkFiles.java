package boofcv.metrics;

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import georegression.struct.point.Point2D_F64;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CreateCalibrationFiducialLandmarkFiles {
	public static void main(String[] args) throws FileNotFoundException {
		ConfigGridDimen config = new ConfigGridDimen(5,7,1);
		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.chessboardX(null,config);
//		ConfigGridDimen config = new ConfigGridDimen(5,7,1,1);
//		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.squareGrid(config);
//		ConfigGridDimen config = new ConfigGridDimen(5,6,4,5);
//		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.circleHexagonalGrid(null,config);
//		ConfigCircleRegularGrid config = new ConfigCircleRegularGrid(4,3,4,6);
//		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.circleRegularGrid(config);

		List<Point2D_F64> points = detector.getLayout();

		PrintStream out = new PrintStream("landmarks.txt");

		out.println("# locations of point landmarks on the fiducial\n" +
				"# (fiducial ID) (x y coordinate for each landmark)\n" +
				"# first one is the default that's used if an unknown ID is detected");

		out.print("-1");
		for( Point2D_F64 p : points ) {
			out.print(" "+p.x+" "+p.y);
		}
		out.println();
		out.close();
	}
}
