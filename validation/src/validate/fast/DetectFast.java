package validate.fast;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I16;

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Detects fast features and compares against a text file containing
 *
 *
 *
 * @author Peter Abeles
 */
public class DetectFast {

	public static final String FILE_NAME = "boofcv_fast.txt";

	public static <T extends ImageSingleBand> void detect( String outputDirectory , Class<T> imageType ) throws FileNotFoundException {
		T raw = UtilImageIO.loadImage("data/outdoors_gray.png", imageType);

		FastCornerIntensity<T> alg = FactoryIntensityPointAlg.fast(20,9,imageType);
		ImageFloat32 intensity = new ImageFloat32(raw.width,raw.height);

		alg.process(raw,intensity);

		QueueCorner found = alg.getCandidates();

		PrintStream fos = new PrintStream(outputDirectory+FILE_NAME);

		fos.println("# Detected FAST features using BoofCV");
		fos.println(found.size);
		for(Point2D_I16 p : found.toList() ) {
			fos.println(p.x+" "+p.y);
		}
		fos.close();

		System.out.println("Detected " + found.size + " features");

		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(new ConfigExtract(1,0,3,true, false, true));

		QueueCorner peaks = new QueueCorner(1000);
		nonmax.process(intensity,null,found,null,peaks);

		System.out.println("Nonmax "+peaks.size);
	}

	public static void main( String args[] ) throws FileNotFoundException {

		String outputDirectory = "./";

		if( args.length > 0 ) {
			outputDirectory = args[0];
		}

		detect(outputDirectory,ImageUInt8.class);
	}
}
