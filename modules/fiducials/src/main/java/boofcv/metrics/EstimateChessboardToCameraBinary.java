package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.common.parsing.ParseCalibrationConfigFiles;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.io.File;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class EstimateChessboardToCameraBinary<T extends ImageGray<T>> extends BaseEstimateSquareFiducialToCamera<T> {

	Class<T> imageType;

	public EstimateChessboardToCameraBinary(Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDir) {

		ConfigGridDimen config = ParseCalibrationConfigFiles.parseGridDimen2(
				new File(datasetDir,"description.txt"));

		return FactoryFiducial.calibChessboardB(null,config, imageType);
	}

	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		EstimateChessboardToCameraBinary app = new EstimateChessboardToCameraBinary(GrayU8.class);
		app.initialize(new File("data/fiducials/chessboard"));
		app.setOutputDirectory(outputDirectory);

//		app.process("distance_straight");
	}


}
