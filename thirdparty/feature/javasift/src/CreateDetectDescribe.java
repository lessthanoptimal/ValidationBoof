import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DSIFT;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates both a detection and description file.
 *
 * @author Peter Abeles
 */
public class CreateDetectDescribe {

	private static FloatArray2D convert( BufferedImage image ) {
		FloatArray2D ret = new FloatArray2D(image.getWidth(),image.getHeight());

		for( int y = 0; y < ret.height; y++ ) {
			for( int x = 0; x < ret.width; x++ ) {
				int rgb = image.getRGB(x,y);
				float v = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF))/2;
				ret.set(v,x,y);
			}
		}

		return ret;
	}

	public static void process( BufferedImage image , String detectName, String describeName )
			throws IOException
	{

		// default parameters are the same
		FloatArray2DSIFT.Param param = new FloatArray2DSIFT.Param();
		param.minOctaveSize = 0;
		param.maxOctaveSize = 10000;
		FloatArray2DSIFT sift = new FloatArray2DSIFT(param);

		FloatArray2D input = convert(image);

		sift.init(input);

		List<Feature> features = sift.run();

		System.out.printf("Processing %5d %s\n", features.size(), describeName);

		// save the descriptors
		PrintStream describeOut = new PrintStream(new FileOutputStream(describeName));
		PrintStream detectOut = new PrintStream(new FileOutputStream(detectName));

		describeOut.println("128");
		for( Feature f : features ) {

			describeOut.printf("%7.3f %7.3f %7.5f", f.location[0], f.location[1], f.orientation);
			detectOut.printf("%.3f %.3f %.5f %.5f\n",f.location[0], f.location[1],f.scale, f.orientation);

			float[] desc = f.descriptor;
			if( desc.length != 128 )
				throw new RuntimeException("Unexpected descriptor length");

			for( int i = 0; i < desc.length; i++ ) {
				describeOut.printf(" %12.10f", desc[i]);
			}
			describeOut.println();
		}

		describeOut.close();
		detectOut.close();
	}

	private static void processDirectory( String nameDirectory ) throws IOException {
		for( int i = 1; i <= 6; i++ ) {
			String imageName = String.format("%s/img%d.png",nameDirectory,i);
			BufferedImage img = ImageIO.read(new File(imageName));

			String detectName = String.format("%s/DETECTED_img%d_%s.txt",nameDirectory,i,"JavaSIFT");
			String describeName = String.format("%s/DESCRIBE_img%d_%s.txt",nameDirectory,i,"JavaSIFT");

			process(img, detectName,describeName);

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