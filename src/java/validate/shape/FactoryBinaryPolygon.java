package validate.shape;

import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.ImageGray;
import validate.FactoryObject;

import java.io.File;

/**
 * @author Peter Abeles
 */
public class FactoryBinaryPolygon<T extends ImageGray<T>>
		implements FactoryObject<BinaryPolygonDetector<T>>
{
	Class<T> imageType;
	boolean fitLines;
	ConfigPolygonDetector config;

	public FactoryBinaryPolygon(boolean fitLines, Class<T> imageType) {
		this.fitLines = fitLines;
		this.imageType = imageType;
	}

	@Override
	public void configure(File file) {
		config = UtilShapeDetector.configurePolygon(fitLines,file);
	}

	@Override
	public BinaryPolygonDetector<T> newInstance() {
		return FactoryShapeDetector.polygon(config, imageType);
	}
}
