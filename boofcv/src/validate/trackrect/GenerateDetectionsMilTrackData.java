package validate.trackrect;

import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.tld.TldConfig;
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
// TODO add support for frames file
// TODO compute metrics in bulk
// TODO compute circulant metrics? Need a continuous valued function of distance
public class GenerateDetectionsMilTrackData<T extends ImageBase> {

	public static String[]videos = new String[]{"cliffbar","coke11","david","dollar","faceocc",
			"faceocc2","girl","surfer","sylv","tiger1","tiger2","twinings"};

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

	public static int[] parseFramesFile( String fileName ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = reader.readLine();
			String words[] = line.split(",");

			return new int[]{Integer.parseInt(words[0]),Integer.parseInt(words[1])};
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

		int frames[] = parseFramesFile(path + "/" + dataName + "_frames.txt");

		int imageNum = frames[0];
		boolean firstImage = true;

		while( imageNum <= frames[1] ) {

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
			try {
				ConvertBufferedImage.convertFrom(image, input, true);
			} catch( RuntimeException e ) {
				System.out.print("Image type not supported");
				break;
			}
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
				out.printf("%05d,nan,nan,nan,nan\n", imageNum);
			} else {
				UtilPolygons2D_F64.bounding(found,bounding);
//				System.out.print("+");
				out.printf("%05d,%f,%f,%f,%f\n",imageNum,bounding.x0,bounding.y0,bounding.x1,bounding.y1);
			}
			System.out.printf("%4d  %f %f\n",imageNum,bounding.x0,bounding.y0);

			imageNum++;
		}
		System.out.println();
		out.close();
	}

	public static void evaluateTLD( String dataset ) {
		Class imageClass = ImageFloat32.class;
		ImageType imageType = ImageType.single(imageClass);

		TrackerObjectQuad tracker =
				FactoryTrackerObjectQuad.tld(new TldConfig(false, imageClass));

		GenerateDetectionsMilTrackData generator = new GenerateDetectionsMilTrackData(imageType);
		generator.evaluate(dataset,"BoofCV-TLD",tracker);
	}

	public static void evaluateCirculant( String dataset ) {
		Class imageClass = ImageFloat32.class;
		ImageType imageType = ImageType.single(imageClass);

		TrackerObjectQuad tracker =
				FactoryTrackerObjectQuad.circulant(new ConfigCirculantTracker(), imageClass);

		GenerateDetectionsMilTrackData generator = new GenerateDetectionsMilTrackData(imageType);
		generator.evaluate(dataset,"BoofCV-Circulant",tracker);
	}

	public static void evaluateCirculantOrig( String dataset ) {
		Class imageClass = ImageFloat32.class;
		ImageType imageType = ImageType.single(imageClass);

		TrackerObjectQuad tracker =
				FactoryTrackerObjectQuad.circulantOrig(new ConfigCirculantTracker(), imageClass);

		GenerateDetectionsMilTrackData generator = new GenerateDetectionsMilTrackData(imageType);
		generator.evaluate(dataset,"BoofCV-CirculantOrig",tracker);
	}

	public static void evaluateSFT( String dataset ) {
		Class imageClass = ImageFloat32.class;
		ImageType imageType = ImageType.single(imageClass);

		TrackerObjectQuad tracker =
				FactoryTrackerObjectQuad.sparseFlow(new SfotConfig(imageClass));

		GenerateDetectionsMilTrackData generator = new GenerateDetectionsMilTrackData(imageType);
		generator.evaluate(dataset,"BoofCV-SFT",tracker);
	}

	public static void evaluateComaniciu(String dataset) {
		Class imageClass = ImageFloat32.class;
		ImageType imageType = ImageType.ms(3, imageClass);

		TrackerObjectQuad tracker =
				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(imageType));

		GenerateDetectionsMilTrackData generator = new GenerateDetectionsMilTrackData(imageType);
		generator.evaluate(dataset,"BoofCV-Comaniciu",tracker);
	}


	public static void evaluate( String dataset ) {
//		evaluateTLD(dataset);
//		evaluateCirculant(dataset);
		evaluateCirculantOrig(dataset);
//		evaluateSFT(dataset);
//		evaluateComaniciu(dataset);
	}

	public static void main(String[] args) {
//		for( String m : videos )
//			evaluate(m);

//		evaluate("cliffbar");
//		evaluate("coke11");
//		evaluate("david");
//		evaluate("dollar");
//		evaluate("faceocc");
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
