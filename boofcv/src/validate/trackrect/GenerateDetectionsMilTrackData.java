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
import java.io.*;

/**
 * @author Peter Abeles
 */
public class GenerateDetectionsMilTrackData<T extends ImageBase> {
	T input;


	public GenerateDetectionsMilTrackData(ImageType<T> type) {
		input = type.createImage(1,1);
	}

	public RectangleCorner2D_F64 readInitial( String fileName ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = reader.readLine();
			String words[] = line.split(",");

			RectangleCorner2D_F64 ret = new RectangleCorner2D_F64();
			ret.x0 = Double.parseDouble(words[0]);
			ret.y0 = Double.parseDouble(words[1]);
			ret.x1 = ret.x0 + Double.parseDouble(words[2]);
			ret.y1 = ret.y0 + Double.parseDouble(words[3]);

			return ret;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void evaluate( String dataName , String outputName , TrackerObjectQuad<T> tracker ) {
		System.out.println("Processing "+dataName);

		String path = "../data/track_rect/MILTrack/"+dataName;
		Quadrilateral_F64 initial = new Quadrilateral_F64();
		RectangleCorner2D_F64 rect = readInitial(path + "/" + dataName + "_gt.txt");
		UtilPolygons2D_F64.convert(rect, initial);
		Quadrilateral_F64 found = new Quadrilateral_F64();
		RectangleCorner2D_F64 bounding = new RectangleCorner2D_F64();

		PrintStream out;

		try {
			out = new PrintStream(outputName+"_"+dataName+".txt");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}


		int imageNum = 0;
		boolean firstImage = true;

		while( true ) {
			String imageName = String.format("%s/imgs/img%05d.png",path,imageNum);
			BufferedImage image = UtilImageIO.loadImage(imageName);
			if( image == null ) {
				if( imageNum == 0 ) {
					imageNum++;
					// might start on index 1
					continue;
				} else {
					break;
				}
			}

			input.reshape(image.getWidth(),image.getHeight());
			ConvertBufferedImage.convertFrom(image, input, true);

			boolean detected;

			if( firstImage ) {
				firstImage = false;
				detected = tracker.initialize(input,initial);
				found.set(initial);
			} else {
				detected = tracker.process(input,found);
			}

			if( !detected ) {
//				System.out.print("-");
				out.println("nan,nan,nan,nan");
			} else {
				UtilPolygons2D_F64.bounding(found,bounding);
//				System.out.print("+");
				out.printf("%f,%f,%f,%f\n",bounding.x0,bounding.y0,bounding.x1,bounding.y1);
			}
			System.out.printf("%4d  %f %f\n",imageNum,bounding.x0,bounding.y0);

			imageNum++;
			if( imageNum % 50 == 0 )
				System.out.println();
		}
		System.out.println();
		out.close();
	}

	public static void evaluate( String dataset ) {
		Class imageType = ImageFloat32.class;

		GenerateDetectionsMilTrackData generator = new GenerateDetectionsMilTrackData(ImageType.single(imageType));

		TrackerObjectQuad tracker =
//				FactoryTrackerObjectQuad.tld(new TldConfig(false, imageType));
				FactoryTrackerObjectQuad.circulant(new ConfigCirculantTracker(),imageType);

		String name = "BoofCV-Circulant";

		generator.evaluate(dataset,name,tracker);
	}

	public static void main(String[] args) {
//		evaluate("cliffbar");
//		evaluate("coke11");
		evaluate("david");
		evaluate("dollar");
		evaluate("faceocc");
		evaluate("faceocc2");
		evaluate("girl");
		evaluate("surfer");
		evaluate("sylv");
		evaluate("tiger1");
		evaluate("tiger2");
		evaluate("twinings");

		System.out.println("DONE!");
	}
}
