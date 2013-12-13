import org.mite.jsurf.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * @author Peter Abeles
 */
public class CreateDescription {

	public static ArrayList<InterestPoint> loadInterestPoints( BufferedImage image , String fileName ) throws IOException {
		Scanner in = new Scanner(new BufferedReader(new FileReader(fileName)));


		ArrayList<InterestPoint> ret = new ArrayList<InterestPoint>();

		// read in location of points
		while( in.hasNext() ) {
			float x = in.nextFloat();
			float y = in.nextFloat();
			float scale = in.nextFloat();
			float yaw = in.nextFloat();

			ret.add(new InterestPoint(x,y,scale,image));
		}

		return ret;
	}

	public static void process( BufferedImage image , String detectName , String describeName )
			throws IOException
	{
		ArrayList<InterestPoint> points = loadInterestPoints(image,detectName);

		System.out.printf("Processing %5d %s\n",points.size(),detectName);
		
		// Compute descriptors for each point
		ISURFfactory mySURF = SURF.createInstance(image, 0.9f, 800, 4, image);

		IDescriptor descriptor = mySURF.createDescriptor(points);
		descriptor.generateAllDescriptors();

		// save the descriptors
		PrintStream out = new PrintStream(new FileOutputStream(describeName));

		out.println("64");
		for( InterestPoint p : points ) {

			int orientationX = p.getOrientation_x();
			int orientationY = p.getOrientation_y();
			float theta = (float)Math.atan2(orientationY,orientationX);

			out.printf("%7.3f %7.3f %7.5f",p.getX(),p.getY(),theta);
			float[] desc = p.getDescriptorOfTheInterestPoint();
			if( desc.length != 64 )
				throw new RuntimeException("Unexpected descriptor length");

			for( int i = 0; i < desc.length; i++ ) {
				out.printf(" %12.10f",desc[i]);
			}
			out.println();
		}
	}

	private static void processDirectory( String nameDirectory ) throws IOException {
		String nameDetected = "SURF";

		for( int i = 1; i <= 6; i++ ) {
			String detectName = String.format("%s/DETECTED_img%d_%s.txt",nameDirectory,i,nameDetected);

			String imageName = String.format("%s/img%d.png",nameDirectory,i);
			BufferedImage img = ImageIO.read(new File(imageName));

			String describeName = String.format("%s/DESCRIBE_img%d_%s.txt",nameDirectory,i,"JavaSURF");

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