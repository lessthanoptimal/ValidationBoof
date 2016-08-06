package validate.shape;

import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.shape.ConfigEllipseDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.ImageGray;
import validate.FactoryObject;

import java.io.File;

/**
 * @author Peter Abeles
 */
public class FactoryBinaryEllipse<T extends ImageGray>
		implements FactoryObject<BinaryEllipseDetector<T>>
{
	Class<T> imageType;
	ConfigEllipseDetector config;

	public FactoryBinaryEllipse( Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public void configure(File file) {
		config = UtilShapeDetector.configureEllipse();
	}

	@Override
	public BinaryEllipseDetector<T> newInstance() {
		return FactoryShapeDetector.ellipse(config, imageType);
	}
}
