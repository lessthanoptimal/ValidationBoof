package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.common.FactoryObject;
import boofcv.common.FactoryObjectAbstract;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;

import java.io.File;
import java.io.IOException;

/**
 * Estimates the location of fiducials in the input images. Results are saved to the specified output directory.
 * The detector should be configured such that the fiducial is of size 1. THe actual size will be read later on
 * and the translation adjusted.
 *
 * @author Peter Abeles
 */
public class EstimateBinaryFiducialToCamera<T extends ImageBase<T>> extends BaseEstimateSquareFiducialToCamera {


	FactoryObject<FiducialDetector<T>> factory;

	public EstimateBinaryFiducialToCamera(FactoryObject<FiducialDetector<T>> factory) {
		this.factory = factory;
	}

	@Override
	public FiducialDetector createDetector(File datasetDir) {
		return factory.newInstance();
	}


	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		FactoryObject factory = new FactoryObjectAbstract() {
			@Override
			public Object newInstance() {
				return FactoryFiducial.squareBinary(new ConfigFiducialBinary(1),
						ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 15), GrayU8.class);
			}
		};

		EstimateBinaryFiducialToCamera app = new EstimateBinaryFiducialToCamera(factory);
		app.initialize(new File("data/fiducials/binary"));
		app.setOutputDirectory(outputDirectory);

//		app.process("distance_straight");
	}


}
