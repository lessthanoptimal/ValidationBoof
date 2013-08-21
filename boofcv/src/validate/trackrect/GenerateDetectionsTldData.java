package validate.trackrect;

import boofcv.abst.tracker.TrackerObjectRectangle;
import boofcv.alg.tracker.tld.TldConfig;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.tracker.FactoryTrackerObjectRectangle;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.shapes.RectangleCorner2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class GenerateDetectionsTldData<T extends ImageBase> {

	T input;


	public GenerateDetectionsTldData(ImageDataType<T> type) {
		input = type.createImage(1,1);
	}

	public void evaluate( String dataName , String outputName , TrackerObjectRectangle<T> tracker ) {
		System.out.println("Processing "+dataName);

		String path = "../data/track_rect/TLD/"+dataName;

		RectangleCorner2D_F64 initial = UtilTldData.parseRectangle(path + "/init.txt");
		RectangleCorner2D_F64 found = new RectangleCorner2D_F64(initial);

		PrintStream out;

		try {
			out = new PrintStream(outputName+"_"+dataName+".txt");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		String imageType = new File(path+"/00001.jpg").exists() ? "jpg" : "png";

		int imageNum = 0;
		while( true ) {
			String imageName = String.format("%s/%05d.%s",path,imageNum+1,imageType);
			BufferedImage image = UtilImageIO.loadImage(imageName);
			if( image == null )
				break;

			input.reshape(image.getWidth(),image.getHeight());
			ConvertBufferedImage.convertFrom(image,input);

			boolean detected;

			if( imageNum == 0 ) {
				detected = tracker.initialize(input,(int)initial.x0,(int)initial.y0,(int)initial.x1,(int)initial.y1);
			} else {
				detected = tracker.process(input,found);
			}

			if( !detected ) {
				System.out.print("-");
				out.println("nan,nan,nan,nan");
			} else {
				System.out.print("+");
				out.printf("%f,%f,%f,%f\n",found.x0,found.y0,found.x1,found.y1);
			}

			imageNum++;
			if( imageNum % 50 == 0 )
				System.out.println();
		}
		System.out.println();
		out.close();
	}

	public static void evaluate( String dataset ) {
		GenerateDetectionsTldData generator = new GenerateDetectionsTldData(ImageDataType.single(ImageFloat32.class));

		TrackerObjectRectangle<ImageFloat32> tracker =
				FactoryTrackerObjectRectangle.createTLD(new TldConfig(ImageFloat32.class));

		String name = "BoofCV";

		generator.evaluate(dataset,name,tracker);
	}

	public static void main(String[] args) {
		evaluate("01_david");
//		evaluate("02_jumping");
//		evaluate("03_pedestrian1");
//		evaluate("04_pedestrian2");
//		evaluate("05_pedestrian3");
//		evaluate("06_car");
//		evaluate("07_motocross");
//		evaluate("08_volkswagen");
//		evaluate("09_carchase");
//		evaluate("10_panda");

		System.out.println("DONE!");
	}
}