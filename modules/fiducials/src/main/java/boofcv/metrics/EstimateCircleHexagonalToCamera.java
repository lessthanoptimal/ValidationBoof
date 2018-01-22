package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.calib.ConfigCircleHexagonalGrid;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.io.File;
import java.io.IOException;

import static boofcv.parsing.ParseCalibrationConfigFiles.parseCircleHexagonalConfig;

/**
 * @author Peter Abeles
 */
public class EstimateCircleHexagonalToCamera<T extends ImageGray<T>> extends BaseEstimateSquareFiducialToCamera<T> {

	Class<T> imageType;

	public EstimateCircleHexagonalToCamera(Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDir) {

		File descriptionFile = new File(datasetDir,"description.txt");
		if( !descriptionFile.exists() )
			throw new RuntimeException("Can't find description.txt for square grid");

		ConfigCircleHexagonalGrid config = parseCircleHexagonalConfig(descriptionFile);

		return FactoryFiducial.calibCircleHexagonalGrid(config, imageType);
	}

	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		EstimateCircleHexagonalToCamera app = new EstimateCircleHexagonalToCamera(GrayU8.class);
		app.initialize(new File("data/fiducials/chessboard"));
		app.setOutputDirectory(outputDirectory);

//		app.process("distance_straight");
	}


}
