package regression;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.ImageDataType;
import validate.DataSetDoesNotExist;
import validate.fiducial.*;
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
	String dataSetsStandard[] = new String[]{"rotation","distance_straight","distance_angle"};
	String dataSetsBlur[] = new String[]{"motion_blur"};
	String dataSetsStatic[] = new String[]{"static_scene","static_front_close","static_front_far"};

	String infoString;

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


		BaseEstimateSquareFiducialToCamera estimate;

		if( type.compareTo("binary") == 0) {
			estimate = new EstimateBinaryFiducialToCamera(detector);
		} else {
			estimate = new EstimateImageFiducialToCamera(detector);
		}

		infoString = name;

		estimate.setOutputDirectory(workDirectory);
		estimate.initialize(new File(baseFiducial,type));

		computeStandardMetrics(type, "Fiducial_Standard_" + name + ".txt", estimate, 5, dataSetsStandard);
		computeStandardMetrics(type, "Fiducial_Blur_"+name+".txt", estimate,10,dataSetsBlur);
		computeStaticMetrics(type, "Fiducial_Static_"+name+".txt", estimate, 5,dataSetsStatic);
	}

	private void computeStandardMetrics(String type, String outName,
										BaseEstimateSquareFiducialToCamera estimate ,
										double maxPixelError ,
										String dataSets[] )
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));

		EvaluateFiducialToCamera evaluate = new EvaluateFiducialToCamera();
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.initialize(new File(baseFiducial,type));
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, dataSets, out, evaluate);
	}

	private void computeStaticMetrics(String type, String outName,
										BaseEstimateSquareFiducialToCamera estimate ,
										double maxPixelError ,
										String dataSets[] )
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));

		EvaluateStaticFiducialSequence evaluate = new EvaluateStaticFiducialSequence();
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.initialize(new File(baseFiducial,type));
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, dataSets, out, evaluate);
	}

	private void processDataSets(BaseEstimateSquareFiducialToCamera estimate, String[] dataSets,
								 PrintStream out,
								 BaseEvaluateFiducialToCamera evaluate)
			throws IOException
	{
		for( String dataSet : dataSets) {
			if( workDirectory.exists() ) {
				ParseHelper.deleteRecursive(workDirectory);
			}
			if( !workDirectory.mkdirs() )
				throw new RuntimeException("Can't create work directory");

			try {
				estimate.process(dataSet);
				evaluate.evaluate(workDirectory, dataSet);
			} catch( DataSetDoesNotExist e ) {
				errorLog.println();
				errorLog.println(e.getMessage());
			} catch( RuntimeException e ) {
				errorLog.println();
				errorLog.println("ERROR in "+infoString+" processing data set "+dataSet);
				e.printStackTrace(errorLog);
			}
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
