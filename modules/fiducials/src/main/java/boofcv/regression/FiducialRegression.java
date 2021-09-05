package boofcv.regression;

import boofcv.common.*;
import boofcv.factory.fiducial.*;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.metrics.*;
import boofcv.struct.image.ImageDataType;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FiducialRegression extends BaseRegression implements ImageRegression {

	File workDirectory = new File("./tmp");
	File baseFiducial = new File("data/fiducials");

	String infoString;

	RuntimeSummary runtime;
	DogArray_F64 summaryPeriodMS = new DogArray_F64();

	public FiducialRegression() {
		super(BoofRegressionConstants.TYPE_FIDCUIALS);
	}

	@Override
	public void process(ImageDataType type) throws IOException {

		runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime,getClass(),"RUN_Fiducials.txt");

		final Class imageType = ImageDataType.typeToSingleClass(type);

		final ConfigThreshold robust = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,20);
		final ConfigThreshold fast = ConfigThreshold.fixed(80);

		FactoryObject factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
			{return FactoryFiducial.squareBinary(new ConfigFiducialBinary(1), robust, imageType);}};
		process( "BinaryRobust", new EstimateBinaryFiducialToCamera(factory),"square_border_binary",false);

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareBinary(new ConfigFiducialBinary(1), fast, imageType);}};
		process("BinaryFast", new EstimateBinaryFiducialToCamera(factory), "square_border_binary",false);

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareImage(new ConfigFiducialImage(), robust, imageType);}};
		process("ImageRobust", new EstimateImageFiducialToCamera(factory), "square_border_image",false);

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareImage(new ConfigFiducialImage(), fast, imageType);}};
		process("ImageFast", new EstimateImageFiducialToCamera(factory), "square_border_image",false);

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
			{return FactoryFiducial.squareHamming(ConfigHammingMarker.loadDictionary(HammingDictionary.ARUCO_MIP_25h7), null, imageType);}};
		process("Hamming", new EstimateBinaryFiducialToCamera(factory), "square_border_hamming",false);

		process("ChessboardBinary", new EstimateChessboardToCameraBinary(imageType), "chessboard", true);

		process("ChessboardXCorner", new EstimateChessboardToCameraXCorner(imageType), "chessboard", true);

		process("SquareGrid", new EstimateSquareGridToCamera(imageType), "square_grid",false);

		process("CircleHexagonal", new EstimateCircleHexagonalToCamera(imageType), "circle_hexagonal",false);

		process("CircleRegular", new EstimateCircleRegularToCamera(imageType), "circle_regular",false);

		process("Uchiya", new EstimateUchiyaFiducialToCamera(imageType), "random_dots",false);

		runtime.printSummaryResults();
		runtime.out.close();
	}

	private void process(String name, BaseEstimateSquareFiducialToCamera estimate, String type, boolean ignoreOrder )
			throws IOException
	{

		infoString = name;

		estimate.setOutputDirectory(workDirectory);
		estimate.initialize(new File(baseFiducial, type));

		PrintStream out = new PrintStream(new File(directoryMetrics,"ACC_Fiducial_"+name+".txt"));
		BoofRegressionConstants.printGenerator(out,getClass());

		try {
			summaryPeriodMS.reset();
			runtime.out.println(name);
			runtime.printUnitsRow(false);

			out.println("##########################################################################################");
			out.println("######                Standard Metrics\n");
			computeStandardMetrics(type, out, estimate,ignoreOrder, 5);
			out.println("\n");
			out.println("##########################################################################################");
			out.println("######                Static Metrics\n");
			computeStaticMetrics(type, out, estimate, 5);
			out.println("\n");
			out.println("##########################################################################################");
			out.println("######                Always Visible Metrics\n");
			computeAlwaysVisibleMetrics(type, out, estimate);

			runtime.out.println();
			runtime.saveSummary(name,summaryPeriodMS);
		} catch( RuntimeException e ) {
			e.printStackTrace();
			e.printStackTrace(errorLog);
		} finally {
			out.close();
		}
	}

	private void computeStandardMetrics(String type, PrintStream out,
										BaseEstimateSquareFiducialToCamera estimate ,
										boolean ignoreOrder ,
										double maxPixelError )
			throws IOException
	{
		estimate.needsIntrinsic = true;

		EvaluateFiducialToCamera evaluate = new EvaluateFiducialToCamera();
		evaluate.setJustSummary(true);
		evaluate.setIgnoreWrongOrder(ignoreOrder);
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, new File(new File(baseFiducial,type),"standard"), out, evaluate);
	}

	private void computeStaticMetrics(String type, PrintStream out,
										BaseEstimateSquareFiducialToCamera estimate ,
										double maxPixelError  )
			throws IOException
	{
		estimate.needsIntrinsic = true;

		EvaluateStaticFiducialSequence evaluate = new EvaluateStaticFiducialSequence();
		evaluate.setJustSummary(true);
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, new File(new File(baseFiducial,type),"static"), out, evaluate);
	}

	private void computeAlwaysVisibleMetrics(String type, PrintStream out,
											 BaseEstimateSquareFiducialToCamera estimate)
			throws IOException
	{
		estimate.needsIntrinsic = false;

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
			errorLog.println("Can't compute \"always visible\" metrics. Doesn't exist. "+dataSetsRoot.getPath());
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
				BoofRegressionConstants.delete(workDirectory,errorLog);
			}
			if( !workDirectory.mkdirs() )
				throw new RuntimeException("Can't create work directory");

			try {
				estimate.process(dataSet);
				evaluate.evaluate(workDirectory, dataSet);
				totalExpected += evaluate.getTotalExpected();
				totalCorrect += evaluate.getTotalCorrect();

				summaryPeriodMS.addAll(estimate.speed);
				runtime.printStatsRow(dataSet.getName(),estimate.speed);
			} catch( DataSetDoesNotExist e ) {
				System.out.println("DataSetDoesNotExist "+e.getMessage());
				errorLog.println();
				errorLog.println(e.getMessage());
			} catch( RuntimeException e ) {
				out.println("\n\nFatal exception processing "+dataSet);
				errorLog.println();
				errorLog.println("ERROR in "+infoString+" processing data set "+dataSet);
				System.out.println("ERROR in "+infoString+" processing data set "+dataSet);
				System.out.println("  "+e.getMessage());
				e.printStackTrace(errorLog);
			}
			out.println();
			out.println("---------------------------------------------------");
			out.println();
		}

		out.println("---------------------------------------------------");
		out.printf("total correct / total expected = %4d /%4d", totalCorrect,totalExpected);
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
//		RegressionRunner.main(new String[]{FiducialRegression.class.getName(),ImageDataType.F32.toString()});
		RegressionRunner.main(new String[]{FiducialRegression.class.getName(),ImageDataType.U8.toString()});
	}
}
