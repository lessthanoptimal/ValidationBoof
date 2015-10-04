package validate.fiducial;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import validate.misc.ParseHelper;

import java.io.*;

/**
 * @author Peter Abeles
 */
public class EstimateChessboardToCamera <T extends ImageSingleBand> extends BaseEstimateSquareFiducialToCamera<T> {

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

		ConfigChessboard config = new ConfigChessboard(numCols,numRows,1);

		return FactoryFiducial.calibChessboard(config, imageType);
	}


	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		EstimateChessboardToCamera app = new EstimateChessboardToCamera(ImageUInt8.class);
		app.initialize(new File("data/fiducials/chessboard"));
		app.setOutputDirectory(outputDirectory);

		app.process("distance_straight");
	}


}
