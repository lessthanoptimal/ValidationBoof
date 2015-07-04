package validate.calib;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

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
		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(5, 7,30));

		// load image list
		String directory = "data/calib/stereo/Bumblebee2_Chess";
		List<String> images = BoofMiscOps.directoryList(directory, "jpg");

		Collections.sort(images);

		PrintStream out = new PrintStream(new FileOutputStream("calib_pts.txt"));

		// process and saves results
		for( String name : images ) {
			BufferedImage orig = UtilImageIO.loadImage(name);
			ImageFloat32 input = new ImageFloat32(orig.getWidth(),orig.getHeight());
			ConvertBufferedImage.convertFrom(orig,input);

			if( detector.process(input) ) {
				System.out.println("Found! "+name);

				List<Point2D_F64> points = detector.getDetectedPoints();

				out.printf("%s %d ",new File(name).getName(),points.size());
				for( Point2D_F64 p : points ) {
					out.printf("%f %f ",p.x,p.y);
				}
				out.println();

			} else {
				System.out.println("Failed: "+name);
			}
		}
	}
}
