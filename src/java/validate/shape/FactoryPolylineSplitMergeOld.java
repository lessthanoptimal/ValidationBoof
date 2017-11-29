package validate.shape;

import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.factory.shape.ConfigSplitMergeLineFit;
import boofcv.factory.shape.FactoryPointsToPolyline;
import validate.FactoryObject;

import java.io.File;

/**
 * @author Peter Abeles
 */
public class FactoryPolylineSplitMergeOld
		implements FactoryObject<PointsToPolyline>
{
	ConfigSplitMergeLineFit config;

	@Override
	public void configure(File file) {
		config = UtilShapeDetector.configurePolylineSplitMergeOld(file);
	}

	@Override
	public PointsToPolyline newInstance() {
		return FactoryPointsToPolyline.splitMerge(config);
	}

}
