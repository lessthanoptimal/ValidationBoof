package boofcv.metrics;

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
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
		DetectSingleFiducialCalibration target = FactoryFiducialCalibration.chessboardX(
				null,new ConfigGridDimen(5, 7, 30));

		PrintStream out = new PrintStream(new FileOutputStream("target.txt"));
		out.println(target.getLayout().size());

		for(Point2D_F64 p : target.getLayout() ) {
			out.printf("%f %f\n",p.x,p.y);
		}
		System.out.println("Done");
	}
}
