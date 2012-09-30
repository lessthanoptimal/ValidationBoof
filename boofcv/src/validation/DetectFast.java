package validation;

import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.detect.extract.NonMaxBlockStrict;
import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.alg.feature.detect.intensity.impl.ImplFastHelper_U8;
import boofcv.alg.feature.detect.intensity.impl.ImplFastIntensity9;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I16;

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 *
 *
 *
 *
 * @author Peter Abeles
 */
public class DetectFast {
	public static void main( String args[] ) throws FileNotFoundException {
		ImageUInt8 raw = UtilImageIO.loadImage("../../images/outdoors_gray.png", ImageUInt8.class);

		FastCornerIntensity<ImageUInt8> alg = new ImplFastIntensity9<ImageUInt8>(new ImplFastHelper_U8(20));
		ImageFloat32 intensity = new ImageFloat32(raw.width,raw.height);

		alg.process(raw,intensity);

		QueueCorner found = alg.getCandidates();

		PrintStream fos = new PrintStream("detected.txt");

		fos.println("# Detected FAST features using BoofCV");
		fos.println(found.size);
		for(Point2D_I16 p : found.toList() ) {
			fos.println(p.x+" "+p.y);
		}
		fos.close();

		System.out.println("Detected " + found.size + " features");

		NonMaxBlockStrict nonmax = new NonMaxBlockStrict(1,0,3);

		QueueCorner peaks = new QueueCorner(1000);
		nonmax.process(intensity,peaks);

		System.out.println("Nonmax "+peaks.size);
	}
}
