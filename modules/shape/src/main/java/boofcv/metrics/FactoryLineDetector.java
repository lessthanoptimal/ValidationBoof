package boofcv.metrics;

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.factory.feature.detect.line.ConfigHoughBinary;
import boofcv.factory.feature.detect.line.ConfigHoughFootSubimage;
import boofcv.factory.feature.detect.line.ConfigHoughGradient;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
import boofcv.struct.image.ImageGray;

/**
 * @author Peter Abeles
 */
public class FactoryLineDetector {
	public static String[] THIN = new String[]{"Polar"};
	public static String[] EDGE = new String[]{"Polar","Foot","FootSub"};

	public static <T extends ImageGray<T>> DetectLine<T> createThin( String name , int maxLines, Class<T> imageType ) {
		ConfigHoughBinary configBinary = new ConfigHoughBinary(maxLines);
		switch( name ) {
			case "Polar":
				return FactoryDetectLine.houghLinePolar(configBinary,null, null,imageType);
			default:
				throw new RuntimeException("Unknown "+name);
		}
	}

	public static <T extends ImageGray<T>> DetectLine<T> createEdge( String name , int maxLines, Class<T> imageType ) {
		ConfigHoughGradient configGradient = new ConfigHoughGradient(maxLines);
		switch( name ) {
			case "Polar":
				return FactoryDetectLine.houghLinePolar(configGradient,null, imageType);
			case "Foot":
				return FactoryDetectLine.houghLineFoot(configGradient,null, imageType);
			case "FootSub":
				return FactoryDetectLine.houghLineFootSub(
						new ConfigHoughFootSubimage(3, 8, 5, 25,maxLines, 2, 2), imageType);
			default:
				throw new RuntimeException("Unknown "+name);
		}
	}
}
