package validate.fiducial;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.calib.ConfigCircleAsymmetricGrid;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import validate.misc.ParseHelper;

import java.io.*;

/**
 * @author Peter Abeles
 */
public class EstimateCircleAsymmetricToCamera<T extends ImageGray> extends BaseEstimateSquareFiducialToCamera<T> {

	Class<T> imageType;

	public EstimateCircleAsymmetricToCamera(Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDir) {

		File descriptionFile = new File(datasetDir,"description.txt");
		if( !descriptionFile.exists() )
			throw new RuntimeException("Can't find description.txt for square grid");

		int numRows,numCols;
		double diameter,centerDistance;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(descriptionFile));
			String line = ParseHelper.skipComments(reader);
			String words[] = line.split(" ");
			numRows = Integer.parseInt(words[0]);
			numCols = Integer.parseInt(words[1]);
			diameter = Double.parseDouble(words[2]);
			centerDistance = Double.parseDouble(words[3]);
			reader.close();

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ConfigCircleAsymmetricGrid config = new ConfigCircleAsymmetricGrid(numRows,numCols,diameter/2,centerDistance);

		return FactoryFiducial.calibCircleAsymGrid(config, imageType);
	}


	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		EstimateCircleAsymmetricToCamera app = new EstimateCircleAsymmetricToCamera(GrayU8.class);
		app.initialize(new File("data/fiducials/chessboard"));
		app.setOutputDirectory(outputDirectory);

//		app.process("distance_straight");
	}


}
