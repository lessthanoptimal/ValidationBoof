package validate.calib;

import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import georegression.struct.point.Point2D_F64;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Saves the location of calibration points on the target to a file
 *
 * @author Peter Abeles
 */
public class SaveTargetDescription {

	public static void main( String args[] ) throws FileNotFoundException {
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(3, 4, 30);

		PrintStream out = new PrintStream(new FileOutputStream("target.txt"));
		out.println(target.points.size());

		for(Point2D_F64 p : target.points ) {
			out.printf("%f %f\n",p.x,p.y);
		}
		System.out.println("Done");
	}
}
