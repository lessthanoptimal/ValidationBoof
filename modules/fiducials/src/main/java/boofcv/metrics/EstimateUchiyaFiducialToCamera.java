package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.Uchiya_to_FiducialDetector;
import boofcv.factory.fiducial.ConfigUchiyaMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.fiducial.FiducialIO;
import boofcv.io.fiducial.RandomDotDefinition;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

import java.io.File;
import java.util.List;

/**
 * Estimate the pose for a Uchiya Marker
 *
 * @author Peter Abeles
 */
public class EstimateUchiyaFiducialToCamera<T extends ImageGray<T>> extends BaseEstimateSquareFiducialToCamera<T> {

	Class<T> imageType;

	public EstimateUchiyaFiducialToCamera(Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDirectory) {
		RandomDotDefinition definition = FiducialIO.loadRandomDotYaml(new File(datasetDirectory, "descriptions.yaml"));

		ConfigUchiyaMarker config = new ConfigUchiyaMarker();
		config.markerLength = definition.markerWidth;

		Uchiya_to_FiducialDetector<T> detector = FactoryFiducial.randomDots(config,imageType);

//		System.out.println("Loading marker definitions");
		for( List<Point2D_F64> marker : definition.markers ) {
			detector.addMarker(marker);
		}
//		System.out.println("       ... done with definitions");

		return detector;
	}
}
