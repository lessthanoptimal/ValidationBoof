package validate.calib;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/**
 * Detects calibration points in an image and saves the results to a file.
 *
 * @author Peter Abeles
 */
public class DetectTargetFeatures {

	public static void main( String args[] ) throws FileNotFoundException {
		// detects the calibration target points
		DetectorFiducialCalibration detector = FactoryFiducialCalibration.chessboard(new ConfigChessboard(5, 7, 30));

		// load image list
		String directory = "data/calib/stereo/Bumblebee2_Chess";
		List<String> images = BoofMiscOps.directoryList(directory, "jpg");

		Collections.sort(images);

		PrintStream out = new PrintStream(new FileOutputStream("calib_pts.txt"));

		// process and saves results
		for( String name : images ) {
			BufferedImage orig = UtilImageIO.loadImage(name);
			GrayF32 input = new GrayF32(orig.getWidth(),orig.getHeight());
			ConvertBufferedImage.convertFrom(orig,input);

			if( detector.process(input) ) {
				System.out.println("Found! "+name);

				CalibrationObservation points = detector.getDetectedPoints();

				out.printf("%s %d ",new File(name).getName(),points.size());
				for( PointIndex2D_F64 p : points.points ) {
					out.printf("%f %f ",p.x,p.y);
				}
				out.println();

			} else {
				System.out.println("Failed: "+name);
			}
		}
	}
}
