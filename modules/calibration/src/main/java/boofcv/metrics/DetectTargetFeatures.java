package boofcv.metrics;

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
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
		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.
//				chessboard(null, new ConfigGridDimen(5, 7, 30));
				circleRegularGrid(null,new ConfigGridDimen(4, 3, 4, 6));

		// load image list
		String directory = "data/calib/mono/circle_regular/distant";
		List<String> images = UtilIO.listByPrefix(directory, null,"jpg");

		Collections.sort(images);

		// process and saves results
		for( String name : images ) {
			PrintStream out = new PrintStream(new FileOutputStream(name.substring(0,name.length()-4)+".txt"));
			out.println("# Automatically selected points in file "+new File(name).getName());

			BufferedImage orig = UtilImageIO.loadImage(name);
			GrayF32 input = new GrayF32(orig.getWidth(),orig.getHeight());
			ConvertBufferedImage.convertFrom(orig,input);

			if( detector.process(input) ) {
				System.out.println("Found! "+name);

				CalibrationObservation points = detector.getDetectedPoints();

//				out.printf("%s %d ",new File(name).getName(),points.size());
				for( PointIndex2D_F64 p : points.points ) {
					out.printf("%f %f\n",p.p.x,p.p.y);
				}

			} else {
				System.out.println("Failed: "+name);
			}

			out.close();
		}
	}
}
