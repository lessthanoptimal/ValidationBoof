package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.common.misc.ParseHelper;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.io.*;

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

		File descriptionFile = new File(datasetDir,"description.txt");
		if( !descriptionFile.exists() )
			throw new RuntimeException("Can't find description.txt for chessboard");

		int numRows,numCols;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(descriptionFile));
			String line = ParseHelper.skipComments(reader);
			String words[] = line.split(" ");
			numRows = Integer.parseInt(words[0]);
			numCols = Integer.parseInt(words[1]);
			reader.close();

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ConfigChessboard config = new ConfigChessboard(numRows,numCols,1);

		return FactoryFiducial.calibChessboard(config, imageType);
	}


	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		EstimateChessboardToCamera app = new EstimateChessboardToCamera(GrayU8.class);
		app.initialize(new File("data/fiducials/chessboard"));
		app.setOutputDirectory(outputDirectory);

//		app.process("distance_straight");
	}


}
