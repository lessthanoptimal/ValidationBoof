package regression;

import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.ImageDataType;
import validate.DataSetDoesNotExist;
import validate.FactoryObject;
import validate.fiducial.*;
import validate.misc.ParseHelper;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FiducialRegression extends BaseTextFileRegression {

	File workDirectory = new File("./tmp");
	File baseFiducial = new File("data/fiducials");

	String infoString;

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		FactoryObject factory = new FactoryObject() { @Override public Object newInstance()
			{return FactoryFiducial.squareBinaryRobust(new ConfigFiducialBinary(1), 20, imageType);}};
		process( "BinaryRobust", new EstimateBinaryFiducialToCamera(factory),"binary");

		factory = new FactoryObject() { @Override public Object newInstance()
		{return FactoryFiducial.squareBinaryFast(new ConfigFiducialBinary(1), 80, imageType);}};
		process("BinaryFast", new EstimateBinaryFiducialToCamera(factory), "binary");

		factory = new FactoryObject() { @Override public Object newInstance()
		{return FactoryFiducial.squareImageRobust(new ConfigFiducialImage(), 20, imageType);}};
		process("ImageRobust", new EstimateImageFiducialToCamera(factory), "image");

		factory = new FactoryObject() { @Override public Object newInstance()
		{return FactoryFiducial.squareImageFast(new ConfigFiducialImage(), 80, imageType);}};
		process("ImageFast", new EstimateImageFiducialToCamera(factory), "image");

		process("Chessboard", new EstimateChessboardToCamera(imageType), "chessboard");
	}

	private void process(String name, BaseEstimateSquareFiducialToCamera estimate, String type) throws IOException {

		infoString = name;

		estimate.setOutputDirectory(workDirectory);
		estimate.initialize(new File(baseFiducial, type));

		computeRuntimeMetrics(type, "Fiducial_Runtime_" + name + ".txt", estimate);
		computeStandardMetrics(type, "Fiducial_Standard_" + name + ".txt", estimate, 5);
		computeStaticMetrics(type, "Fiducial_Static_" + name + ".txt", estimate, 5);
	}

	private void computeRuntimeMetrics(String type, String outName, BaseEstimateSquareFiducialToCamera factory )
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));


		RuntimePerformanceFiducialToCamera benchmark =
				new RuntimePerformanceFiducialToCamera(factory);

		benchmark.setErrorStream(errorLog);
		benchmark.setOutputResults(out);

		benchmark.evaluate(new File("data/fiducials/",type));
	}

	private void computeStandardMetrics(String type, String outName,
										BaseEstimateSquareFiducialToCamera estimate ,
										double maxPixelError )
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));

		EvaluateFiducialToCamera evaluate = new EvaluateFiducialToCamera();
		evaluate.setJustSummary(true);
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, new File(new File(baseFiducial,type),"standard"), out, evaluate);
	}

	private void computeStaticMetrics(String type, String outName,
										BaseEstimateSquareFiducialToCamera estimate ,
										double maxPixelError  )
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));

		EvaluateStaticFiducialSequence evaluate = new EvaluateStaticFiducialSequence();
		evaluate.setJustSummary(true);
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, new File(new File(baseFiducial,type),"static"), out, evaluate);
	}

	private void processDataSets(BaseEstimateSquareFiducialToCamera estimate, File dataSetsRoot,
								 PrintStream out,
								 BaseEvaluateFiducialToCamera evaluate)
			throws IOException
	{
		List<File> directories = Arrays.asList(dataSetsRoot.listFiles());
		Collections.sort(directories);

		for( File dataSet : directories) {
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
