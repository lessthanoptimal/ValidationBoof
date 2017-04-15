package validate.trackrect;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Peter Abeles
 */
public class DebugTrackerTldData<T extends ImageBase<T>> {

	T input;

	public DebugTrackerTldData(ImageType<T> type) {
		input = type.createImage(1,1);
	}

	public void evaluate( String dataName , TrackerObjectQuad<T> tracker ) {
		System.out.println("Processing "+dataName);

		String path = "data/track_rect/TLD/"+dataName;

		Quadrilateral_F64 initial = new Quadrilateral_F64();
		Rectangle2D_F64 rect = UtilTldData.parseRectangle(path + "/init.txt");
		UtilPolygons2D_F64.convert(rect,initial);
		Quadrilateral_F64 found = new Quadrilateral_F64();

		ImagePanel gui = null;

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
				gui = new ImagePanel(image);
				ShowImages.showWindow(gui,dataName);
				detected = tracker.initialize(input,initial);
			} else {
				detected = tracker.process(input,found);
			}

			if( !detected ) {
				System.out.print("-");
			} else {
				System.out.print("+");

				Graphics2D g2 = image.createGraphics();

				g2.drawLine((int)found.a.x,(int)found.a.y,(int)found.b.x,(int)found.b.y);
				g2.drawLine((int)found.b.x,(int)found.b.y,(int)found.c.x,(int)found.c.y);
				g2.drawLine((int)found.c.x,(int)found.c.y,(int)found.d.x,(int)found.d.y);
				g2.drawLine((int)found.d.x,(int)found.d.y,(int)found.a.x,(int)found.a.y);
			}

			gui.setImageRepaint(image);

			imageNum++;
			if( imageNum % 50 == 0 )
				System.out.println();
		}
		System.out.println();
	}

	public static void evaluate( String dataset ) {
		Class type = GrayU8.class;

		DebugTrackerTldData generator = new DebugTrackerTldData(ImageType.single(type));

		TrackerObjectQuad<GrayF32> tracker =
				FactoryTrackerObjectQuad.tld(null,type);
//				FactoryTrackerObjectQuad.sparseFlow(new SfotConfig(type));
//				FactoryTrackerObjectQuad.meanShiftLikelihood(30,6,255, MeanShiftLikelihoodType.HISTOGRAM_INDEPENDENT_RGB_to_HSV,
//						type);

		generator.evaluate(dataset,tracker);
	}

	public static void main(String[] args) {
//		evaluate("01_david");
		evaluate("02_jumping");
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
