package regression;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.ImageDataType;
import validate.fiducial.EstimateBinaryFiducialToCamera;
import validate.fiducial.EstimateImageFiducialToCamera;
import validate.fiducial.EstimateSquareFiducialToCamera;
import validate.fiducial.EvaluateFiducialToCamera;
import validate.misc.ParseHelper;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class FiducialRegression extends BaseTextFileRegression {

	File workDirectory = new File("./tmp");
	File baseFiducial = new File("data/fiducials");
	String dataSets[] = new String[]{"rotation","distance_straight","distance_angle"};

	@Override
	public void process(ImageDataType type) throws IOException {
		Class imageType = ImageDataType.typeToSingleClass(type);

		FiducialDetector detector = FactoryFiducial.squareBinaryRobust(new ConfigFiducialBinary(1),20,imageType);
		process( "BinaryRobust", detector,"binary");
		detector = FactoryFiducial.squareBinaryFast(new ConfigFiducialBinary(1), 80, imageType);
		process( "BinaryFast", detector,"binary");

		detector = FactoryFiducial.squareImageRobust(new ConfigFiducialImage(1), 20, imageType);
		process( "ImageRobust", detector,"image");
		detector = FactoryFiducial.squareImageFast(new ConfigFiducialImage(1), 80, imageType);
		process( "ImageFast", detector,"image");
	}

	private void process(String name, FiducialDetector detector, String type) throws IOException {
		PrintStream out = new PrintStream(new File(directory,"Fiducial_"+name+".txt"));

		EstimateSquareFiducialToCamera estimate;

		if( type.compareTo("binary") == 0) {
			estimate = new EstimateBinaryFiducialToCamera(detector);
		} else {
			estimate = new EstimateImageFiducialToCamera(detector);
		}

		estimate.setOutputDirectory(workDirectory);
		estimate.initialize(new File(baseFiducial,type));

		EvaluateFiducialToCamera evaluate = new EvaluateFiducialToCamera();
		evaluate.initialize(new File(baseFiducial,type));
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		for( String dataSet : dataSets ) {
			if( workDirectory.exists() ) {
				ParseHelper.deleteRecursive(workDirectory);
			}
			if( !workDirectory.mkdirs() )
				throw new RuntimeException("Can't create work directory");

			estimate.process(dataSet);
			evaluate.evaluate(workDirectory,dataSet);

			out.println();
			out.println("---------------------------------------------------");
			out.println();
		}
	}

	public static void main(String[] args) throws IOException {
		FiducialRegression app = new FiducialRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
