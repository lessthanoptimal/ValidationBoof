package boofcv.metrics.object;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.common.BoofRegressionConstants;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import org.ddogleg.struct.DogArray_F64;

import java.awt.image.BufferedImage;
import java.io.*;

/**
 * @author Peter Abeles
 */
// TODO add support for frames file
// TODO compute metrics in bulk
// TODO compute circulant metrics? Need a continuous valued function of distance
public class GenerateDetectionsMilTrackData<T extends ImageBase<T>> {

	public static String[]videos = new String[]{"cliffbar","coke11","david","dollar","faceocc",
			"faceocc2","girl","surfer","sylv","tiger1","tiger2","twinings"};

	T input;

	File outputDirectory = BoofRegressionConstants.tempDir();

	// Processing time for each frame
	public DogArray_F64 periodMS = new DogArray_F64();

	public GenerateDetectionsMilTrackData(ImageType<T> type) {
		input = type.createImage(1,1);
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public Rectangle2D_F64 readInitial( String fileName ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = reader.readLine();
			String[] words = line.split(",");

			Rectangle2D_F64 ret = new Rectangle2D_F64();
			ret.p0.x = Double.parseDouble(words[0]);
			ret.p0.y = Double.parseDouble(words[1]);
			ret.p1.x = ret.p0.x + Double.parseDouble(words[2]);
			ret.p1.y = ret.p0.y + Double.parseDouble(words[3]);

			return ret;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static int[] parseFramesFile( String fileName ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = reader.readLine();
			String[] words = line.split(",");

			return new int[]{Integer.parseInt(words[0]),Integer.parseInt(words[1])};
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void evaluate( String dataName , String outputName , TrackerObjectQuad<T> tracker ) {
		System.out.println("Processing "+dataName);

		periodMS.reset();
		if( !outputDirectory.exists() )
			if( !outputDirectory.mkdirs() )
				throw new RuntimeException("Couldn't create directories. "+outputDirectory.getPath());

		String path = "data/track_rect/MILTrack/"+dataName;
		Quadrilateral_F64 initial = new Quadrilateral_F64();
		Rectangle2D_F64 rect = readInitial(path + "/" + dataName + "_gt.txt");
		UtilPolygons2D_F64.convert(rect, initial);
		Quadrilateral_F64 found = new Quadrilateral_F64();
		Rectangle2D_F64 bounding = new Rectangle2D_F64();

		PrintStream out;

		try {
			out = new PrintStream(new File(outputDirectory,outputName+"_"+dataName+".txt"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		int[] frames = parseFramesFile(path + "/" + dataName + "_frames.txt");

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

			long time0 = System.nanoTime();
			if( firstImage ) {
				firstImage = false;
				detected = tracker.initialize(input,initial);
				found.setTo(initial);
			} else {
				detected = tracker.process(input,found);
			}
			long time1 = System.nanoTime();
			periodMS.add((time1-time0)*1e-6);

			if( !detected ) {
//				System.out.print("-");
				out.printf("%05d,nan,nan,nan,nan\n", imageNum);
			} else {
				UtilPolygons2D_F64.bounding(found,bounding);
//				System.out.print("+");
				out.printf("%05d,%f,%f,%f,%f\n",imageNum,bounding.p0.x,bounding.p0.y,bounding.p1.x,bounding.p1.y);
			}
			System.out.printf("%4d  %f %f\n",imageNum,bounding.p0.x,bounding.p0.y);

			imageNum++;
		}
		System.out.println();
		out.close();
	}

	public static void main(String[] args) {
		FactoryEvaluationTrackerObjectQuad.Info info =
//				FactoryEvaluationTrackerObjectQuad.circulant(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.tld(ImageDataType.F32);
				FactoryEvaluationTrackerObjectQuad.sparseFlow(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.meanShiftComaniciuNoScale(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.meanShiftComaniciuScale(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.meanShiftLikelihoodHist(ImageDataType.U8);

		System.out.println("DONE!");
	}
}
