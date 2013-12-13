import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Peter Abeles
 */
public class CreateDetectorDescriptor {


	public static void process( BufferedImage image , String detectName, String describeName )
			throws IOException
	{
		DoGSIFTEngine engine = new DoGSIFTEngine();

		engine.getOptions().setDoubleInitialImage(false);
//		engine.getOptions().setMagnitudeThreshold(0);
//		engine.getOptions().setScales(3);
//		engine.getOptions().setExtraScaleSteps(0);
//		How to set number of octaves?


		MBFImage query = ImageUtilities.createMBFImage(image,false);

		System.out.printf("Detecting %s\n",detectName);

		LocalFeatureList<Keypoint> points = engine.findFeatures(query.flatten());

		// save the descriptors
		PrintStream out = new PrintStream(new FileOutputStream(detectName));
		for( Keypoint p : points ) {
			out.printf("%7.3f %7.3f %7.5f %7.5f\n",p.getX(),p.getY(),p.getScale(),p.ori);
		}
		out.close();

		System.out.printf("Describing %5d\n",points.size());

		out = new PrintStream(new FileOutputStream(describeName));

		out.println("128");
		for( Keypoint p : points ) {

			out.printf("%7.3f %7.3f %7.5f",p.getX(),p.getY(),p.ori);
			byte[] desc = p.ivec;
			if( desc.length != 128 )
				throw new RuntimeException("Unexpected descriptor length");

			for( int i = 0; i < desc.length; i++ ) {
				out.printf(" %d",desc[i]);
			}
			out.println();
		}
		out.close();
	}

	private static void processDirectory( String nameDirectory ) throws IOException {
		String libName = "OpenIMAJ_SIFT";

		for( int i = 1; i <= 6; i++ ) {
			String detectName = String.format("%s/DETECTED_img%d_%s.txt",nameDirectory,i,libName);
			String describeName = String.format("%s/DESCRIBE_img%d_%s.txt",nameDirectory,i,libName);

			String imageName = String.format("%s/img%d.png",nameDirectory,i);
			BufferedImage img = ImageIO.read(new File(imageName));


			process(img, detectName, describeName);

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
