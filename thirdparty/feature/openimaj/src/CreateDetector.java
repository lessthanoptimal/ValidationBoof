import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Creates detector file and removes duplicates
 *
 * @author Peter Abeles
 */
public class CreateDetector {


	public static void process( BufferedImage image , String detectName)
			throws IOException
	{
		DoGSIFTEngine engine = new DoGSIFTEngine();

		engine.getOptions().setDoubleInitialImage(false);
//		engine.getOptions().setMagnitudeThreshold(0);
//		engine.getOptions().setScales(3);
//		engine.getOptions().setExtraScaleSteps(0);
//		How to set number of octaves?


		MBFImage query = ImageUtilities.createMBFImage(image,false);

		System.out.printf("Detecting %s ",detectName);

		LocalFeatureList<Keypoint> points = engine.findFeatures(query.flatten());

		// save the descriptors
		PrintStream out = new PrintStream(new FileOutputStream(detectName));
		float prevX = -1;
		float prevY = -1;
		float prevScale = -1;

		int totalSkipped = 0;

		for( Keypoint p : points ) {
			if( !(prevX == p.getX() && prevY == p.getY() && prevScale == p.getScale()) ) {
				out.printf("%7.3f %7.3f %7.5f %7.5f\n",p.getX(),p.getY(),p.getScale(),p.ori);
				prevX = p.getX();
				prevY = p.getY();
				prevScale = p.getScale();
			} else {
				totalSkipped++;
			}
		}
		out.close();

		System.out.println(" skipped = "+totalSkipped);
	}

	private static void processDirectory( String nameDirectory ) throws IOException {
		String libName = "OpenIMAJ_SIFT";

		for( int i = 1; i <= 6; i++ ) {
			String detectName = String.format("%s/DETECTED_img%d_%s.txt",nameDirectory,i,libName);

			String imageName = String.format("%s/img%d.png",nameDirectory,i);
			BufferedImage img = ImageIO.read(new File(imageName));


			process(img, detectName);

		}
	}

	public static void main( String args[] ) throws IOException {
		processDirectory("../../data/bark");
		processDirectory("../../data/bikes");
		processDirectory("../../data/boat");
		processDirectory("../../data/graf");
		processDirectory("../../data/leuven");
		processDirectory("../../data/trees");
		processDirectory("../../data/ubc");
		processDirectory("../../data/wall");
	}
}
