package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.common.misc.ParseHelper;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class EstimateChessboardToCamera2<T extends ImageGray<T>> extends BaseEstimateSquareFiducialToCamera<T> {

	Class<T> imageType;

	public EstimateChessboardToCamera2(Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDir) {

		ConfigGridDimen config = loadDimension(datasetDir);

		return FactoryFiducial.calibChessboard2(null,config, imageType);
	}

	public static ConfigGridDimen loadDimension(File datasetDir) {
		File descriptionFile = new File(datasetDir,"description.txt");
		if( !descriptionFile.exists() )
			throw new RuntimeException("Can't find description.txt for chessboard");

		int numRows,numCols;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(descriptionFile));
			String line = ParseHelper.skipComments(reader);
			String words[] = line.split(" ");
			numRows = Integer.parseInt(words[0]);
			numCols = Integer.parseInt(words[1]);
			reader.close();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new ConfigGridDimen(numRows,numCols,1);
	}


	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		EstimateChessboardToCamera2 app = new EstimateChessboardToCamera2(GrayU8.class);
		app.initialize(new File("data/fiducials/chessboard"));
		app.setOutputDirectory(outputDirectory);

//		app.process("distance_straight");
	}


}
