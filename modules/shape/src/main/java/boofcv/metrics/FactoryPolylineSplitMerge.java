package boofcv.metrics;

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.common.FactoryObject;
import boofcv.factory.shape.FactoryPointsToPolyline;

import java.io.File;

/**
 * @author Peter Abeles
 */
public class FactoryPolylineSplitMerge
		implements FactoryObject<PointsToPolyline>
{
	PolylineSettings settings;

	@Override
	public void configure(File file) {
		settings = UtilShapeDetector.loadPolylineSettings(file);
	}

	@Override
	public PointsToPolyline newInstance() {
		PointsToPolyline alg = FactoryPointsToPolyline.splitMerge((ConfigPolylineSplitMerge)null);
		alg.setMinimumSides(settings.minSides);
		alg.setMaximumSides(settings.maxSides);
		alg.setConvex(settings.convex);
		return alg;
	}

}
