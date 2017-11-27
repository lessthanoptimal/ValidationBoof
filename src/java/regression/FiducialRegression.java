package regression;

import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.struct.image.ImageDataType;
import validate.DataSetDoesNotExist;
import validate.FactoryObject;
import validate.FactoryObjectAbstract;
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

		final ConfigThreshold robust = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,20);
		final ConfigThreshold fast = ConfigThreshold.fixed(80);

		FactoryObject factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
			{return FactoryFiducial.squareBinary(new ConfigFiducialBinary(1), robust, imageType);}};
		process( "BinaryRobust", new EstimateBinaryFiducialToCamera(factory),"square_border_binary");

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareBinary(new ConfigFiducialBinary(1), fast, imageType);}};
		process("BinaryFast", new EstimateBinaryFiducialToCamera(factory), "square_border_binary");

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareImage(new ConfigFiducialImage(), robust, imageType);}};
		process("ImageRobust", new EstimateImageFiducialToCamera(factory), "square_border_image");

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareImage(new ConfigFiducialImage(), fast, imageType);}};
		process("ImageFast", new EstimateImageFiducialToCamera(factory), "square_border_image");

		process("Chessboard", new EstimateChessboardToCamera(imageType), "chessboard");

		process("SquareGrid", new EstimateSquareGridToCamera(imageType), "square_grid");

		process("CircleHexagonal", new EstimateCircleHexagonalToCamera(imageType), "circle_hexagonal");

		process("CircleRegular", new EstimateCircleRegularToCamera(imageType), "circle_regular");

	}

	private void process(String name, BaseEstimateSquareFiducialToCamera estimate, String type) throws IOException {

		infoString = name;

		estimate.setOutputDirectory(workDirectory);
		estimate.initialize(new File(baseFiducial, type));

		computeRuntimeMetrics(type, "Fiducial_Runtime_" + name + ".txt", estimate);
		computeStandardMetrics(type, "Fiducial_Standard_" + name + ".txt", estimate, 5);
		computeStaticMetrics(type, "Fiducial_Static_" + name + ".txt", estimate, 5);
		computeAlwaysVisibleMetrics(type, "Fiducial_AlwaysVisible_" + name + ".txt", estimate);
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

	private void computeAlwaysVisibleMetrics(String type, String outName,
											 BaseEstimateSquareFiducialToCamera estimate)
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));

		EvaluateAlwaysVisibleSequence evaluate = new EvaluateAlwaysVisibleSequence();
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, new File(new File(baseFiducial,type),"always_visible"), out, evaluate);
	}

	private void processDataSets(BaseEstimateSquareFiducialToCamera estimate, File dataSetsRoot,
								 PrintStream out,
								 FiducialEvaluateInterface evaluate)
			throws IOException
	{
		if( !dataSetsRoot.exists() ) {
			errorLog.println("Can't compute \"always visible\" metrics.  Doesn't exist. "+dataSetsRoot.getPath());
			return;
		}

		List<File> directories = Arrays.asList(dataSetsRoot.listFiles());
		Collections.sort(directories);

		int totalExpected = 0;
		int totalCorrect = 0;

		for( File dataSet : directories) {
			if( dataSet.isFile() )
				continue;

			if( workDirectory.exists() ) {
				ParseHelper.deleteRecursive(workDirectory);
			}
			if( !workDirectory.mkdirs() )
				throw new RuntimeException("Can't create work directory");

			try {
				estimate.process(dataSet);
				evaluate.evaluate(workDirectory, dataSet);
				totalExpected += evaluate.getTotalExpected();
				totalCorrect += evaluate.getTotalCorrect();
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

		out.println("---------------------------------------------------");
		out.printf("total correct / total expected = %4d /%4d", totalCorrect,totalExpected);
	}

	public static void main(String[] args) throws IOException {
		FiducialRegression app = new FiducialRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
