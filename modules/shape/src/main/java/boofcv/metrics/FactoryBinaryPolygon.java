package boofcv.metrics;

import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.common.FactoryObject;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.ImageGray;

import java.io.File;

/**
 * @author Peter Abeles
 */
public class FactoryBinaryPolygon<T extends ImageGray<T>>
		implements FactoryObject<DetectPolygonBinaryGrayRefine<T>>
{
	Class<T> imageType;
	ConfigPolygonDetector config;

	public FactoryBinaryPolygon(Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public void configure(File file) {
		config = UtilShapeDetector.configurePolygon(file);
	}

	@Override
	public DetectPolygonBinaryGrayRefine<T> newInstance() {
		return FactoryShapeDetector.polygon(config, imageType);
	}
}
