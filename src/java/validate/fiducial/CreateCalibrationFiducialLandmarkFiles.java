package validate.fiducial;

import boofcv.abst.calib.ConfigSquareGrid;
import boofcv.abst.calib.PlanarDetectorSquareGrid;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import georegression.struct.point.Point2D_F64;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CreateCalibrationFiducialLandmarkFiles {
	public static void main(String[] args) throws FileNotFoundException {
//		ConfigChessboard config = new ConfigChessboard(5,7,1);
//		PlanarDetectorChessboard detector = FactoryPlanarCalibrationTarget.detectorChessboard(config);
		ConfigSquareGrid config = new ConfigSquareGrid(5,7,1,1);
		PlanarDetectorSquareGrid detector = FactoryPlanarCalibrationTarget.detectorSquareGrid(config);

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
