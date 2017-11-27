package validate.shape;

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.factory.shape.FactoryPointsToPolyline;
import validate.FactoryObject;

import java.io.File;

/**
 * @author Peter Abeles
 */
public class FactoryPolylineSplitMerge
		implements FactoryObject<PointsToPolyline>
{
	ConfigPolylineSplitMerge config;

	@Override
	public void configure(File file) {
		config = UtilShapeDetector.configurePolylineSplitMerge(file);
	}

	@Override
	public PointsToPolyline newInstance() {
		return FactoryPointsToPolyline.splitMerge(config);
	}

}
