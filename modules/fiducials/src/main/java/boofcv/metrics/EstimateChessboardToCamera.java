package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.parsing.ParseCalibrationConfigFiles;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.io.File;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class EstimateChessboardToCamera <T extends ImageGray<T>> extends BaseEstimateSquareFiducialToCamera<T> {

	Class<T> imageType;

	public EstimateChessboardToCamera(Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDir) {

		ConfigGridDimen config = ParseCalibrationConfigFiles.parseGridDimen2(
				new File(datasetDir,"description.txt"));

		return FactoryFiducial.calibChessboard(null,config, imageType);
	}

	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		EstimateChessboardToCamera app = new EstimateChessboardToCamera(GrayU8.class);
		app.initialize(new File("data/fiducials/chessboard"));
		app.setOutputDirectory(outputDirectory);

//		app.process("distance_straight");
	}


}
