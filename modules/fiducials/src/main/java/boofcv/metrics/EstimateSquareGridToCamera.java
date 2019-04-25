package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.common.misc.ParseHelper;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.io.*;

/**
 * @author Peter Abeles
 */
public class EstimateSquareGridToCamera<T extends ImageGray<T>> extends BaseEstimateSquareFiducialToCamera<T> {

	Class<T> imageType;

	public EstimateSquareGridToCamera(Class<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDir) {

		File descriptionFile = new File(datasetDir,"description.txt");
		if( !descriptionFile.exists() )
			throw new RuntimeException("Can't find description.txt for square grid");

		int numRows,numCols;
		double square,space;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(descriptionFile));
			String line = ParseHelper.skipComments(reader);
			String words[] = line.split(" ");
			numRows = Integer.parseInt(words[0]);
			numCols = Integer.parseInt(words[1]);
			square = Double.parseDouble(words[2]);
			space = Double.parseDouble(words[3]);
			reader.close();

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ConfigGridDimen config = new ConfigGridDimen(numRows,numCols,square,space);

		return FactoryFiducial.calibSquareGrid(null,config, imageType);
	}


	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		EstimateSquareGridToCamera app = new EstimateSquareGridToCamera(GrayU8.class);
		app.initialize(new File("data/fiducials/chessboard"));
		app.setOutputDirectory(outputDirectory);

//		app.process("distance_straight");
	}


}
