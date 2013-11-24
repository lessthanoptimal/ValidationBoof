package validate.trackrect;

import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
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


	public GenerateDetectionsTldData(ImageType<T> type) {
		input = type.createImage(1,1);
	}

	public void evaluate( String dataName , String outputName , TrackerObjectQuad<T> tracker ) {
		System.out.println("Processing "+dataName);

		String path = "../data/track_rect/TLD/"+dataName;
		Quadrilateral_F64 initial = new Quadrilateral_F64();
		RectangleCorner2D_F64 rect = UtilTldData.parseRectangle(path + "/init.txt");
		UtilPolygons2D_F64.convert(rect, initial);
		Quadrilateral_F64 found = new Quadrilateral_F64();
		RectangleCorner2D_F64 bounding = new RectangleCorner2D_F64();

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
			ConvertBufferedImage.convertFrom(image,input,true);

			boolean detected;

			if( imageNum == 0 ) {
				detected = tracker.initialize(input,initial);
			} else {
				detected = tracker.process(input,found);
			}

			if( !detected ) {
				System.out.print("-");
				out.println("nan,nan,nan,nan");
			} else {
				UtilPolygons2D_F64.bounding(found,bounding);
				System.out.print("+");
				out.printf("%f,%f,%f,%f\n",bounding.x0,bounding.y0,bounding.x1,bounding.y1);
			}

			imageNum++;
			if( imageNum % 50 == 0 )
				System.out.println();
		}
		System.out.println();
		out.close();
	}

	public static void evaluate( String dataset ) {
		Class imageType = ImageFloat32.class;

		GenerateDetectionsTldData generator = new GenerateDetectionsTldData(ImageType.single(imageType));

		TrackerObjectQuad tracker =
//				FactoryTrackerObjectQuad.tld(new TldConfig(false, imageType));
				FactoryTrackerObjectQuad.circulant(new ConfigCirculantTracker(), imageType);

//		String name = "BoofCV-TLD";
		String name = "BoofCV-Circulant";

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
