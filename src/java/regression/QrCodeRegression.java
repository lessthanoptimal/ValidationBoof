package regression;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.ImageDataType;
import org.apache.commons.io.FileUtils;
import validate.DataSetDoesNotExist;
import validate.fiducial.*;
import validate.fiducial.qrcode.DetectQrCodesInImages;
import validate.fiducial.qrcode.EvaluateQrCodeDetections;
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
public class QrCodeRegression extends BaseTextFileRegression {

	File workDirectory = new File("./tmp");
	File baseFiducial = new File("data/fiducials/qrcodes");

	String infoString;

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		QrCodeDetector defaultDetector = FactoryFiducial.qrcode(null,imageType);

		process("default",defaultDetector);
	}

	private void process(String name, QrCodeDetector detector) throws IOException {

		infoString = name;

		PrintStream runtimeOut = new PrintStream(new File(directory,"QRCodeRuntime_"+name+".txt"));
		runtimeOut.println("# Average runtime in milliseconds for each dataset for "+name);

		PrintStream metricsOut = new PrintStream(new File(directory,"QRCodeDetection_"+name+".txt"));

		metricsOut.println("# QR Code Detection Metrics for "+name);
		metricsOut.println("# N  = total number of qr codes in truth set");
		metricsOut.println("# TP = true positive");
		metricsOut.println("# FN = false negative");
		metricsOut.println("# FP = false positive");
		metricsOut.println("# MD = multiple detection of truth qr code");
		metricsOut.println("# Overlap = average overlap across true positives");
		metricsOut.println();

		File groundTruthHome = new File(baseFiducial,"standard");

		File[] directories = groundTruthHome.listFiles();
		if( directories == null )
			throw new IOException("no data set directories found in "+groundTruthHome.getPath());

		int totalTP = 0;
		int totalN = 0;
		int totalFP = 0;

		boolean foundDataSets = false;
		for( File f: directories ) {
			if( !f.isDirectory() )
				return;

			if( workDirectory.exists() ) {
				FileUtils.deleteDirectory(workDirectory);
				if( !workDirectory.mkdirs() ) {
					throw new IOException("Can't create work directory");
				}
			}

			DetectQrCodesInImages evaluateDetect = new DetectQrCodesInImages();
			evaluateDetect.setOutputDirectory(workDirectory);
			try {
				evaluateDetect.process(detector, f);
			} catch( RuntimeException e ) {
				errorLog.println(e.toString());
				return;
			}

			runtimeOut.printf("%20s %8.3f (ms)\n",f.getName(),+evaluateDetect.averageMS);

			EvaluateQrCodeDetections evaluateMetrics = new EvaluateQrCodeDetections();
			evaluateMetrics.evaluate(workDirectory,f);

			foundDataSets = true;
			metricsOut.printf("%20s N %3d TP %3d FN %3d FP %3d MD %3d Overlap %5.1f%%\n",
					f.getName(),
					evaluateMetrics.totalTruth,evaluateMetrics.truePositive,evaluateMetrics.falseNegative,
					evaluateMetrics.falsePositive,evaluateMetrics.multipleDetections,100*evaluateMetrics.averageOverlap);

			totalN += evaluateMetrics.totalTruth;
			totalTP += evaluateMetrics.truePositive;
			totalFP += evaluateMetrics.falsePositive;
		}

		metricsOut.printf("\nSumary N %3d TP %3d FP %3d\n", totalN,totalTP,totalFP);

		runtimeOut.close();
		metricsOut.close();

		if( !foundDataSets ) {
			throw new IOException("no data set directories found in "+baseFiducial.getPath());
		}

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
		QrCodeRegression app = new QrCodeRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
