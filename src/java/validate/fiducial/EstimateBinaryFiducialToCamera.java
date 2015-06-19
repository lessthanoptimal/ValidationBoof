package validate.fiducial;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageUInt8;

import java.io.File;
import java.io.IOException;

/**
 * Estimates the location of fiducials in the input images.  Results are saved to the specified output directory.
 * The detector should be configured such that the fiducial is of size 1.  THe actual size will be read later on
 * and the translation adjusted.
 *
 * @author Peter Abeles
 */
public class EstimateBinaryFiducialToCamera<T extends ImageBase> extends EstimateSquareFiducialToCamera {


	public EstimateBinaryFiducialToCamera(FiducialDetector<T> detector) {
		super(detector);
	}

	@Override
	public double readFiducialWidth() {
		FiducialCommon.ScenarioBinary scenario = FiducialCommon.parseScenarioBinary(new File(baseDirectory, "fiducials.txt"));
		return scenario.width;
	}

	@Override
	public void configureDetector(File baseDirectory) {

	}

	public static void main(String[] args) throws IOException {

		File outputDirectory = new File("tmp");
		Class imageType = ImageUInt8.class;

		FiducialDetector detector = FactoryFiducial.squareBinaryRobust(new ConfigFiducialBinary(1), 15, imageType);

		outputDirectory.mkdirs();

		EstimateBinaryFiducialToCamera app = new EstimateBinaryFiducialToCamera(detector);
		app.initialize(new File("data/fiducials/binary"));
		app.setOutputDirectory(outputDirectory);

		app.process("distance_straight");
	}

}
