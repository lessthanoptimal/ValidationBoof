package boofcv.regression;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.common.*;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.metrics.qrcode.DetectQrCodesInImages;
import boofcv.metrics.qrcode.EvaluateQrCodeDecoding;
import boofcv.metrics.qrcode.EvaluateQrCodeDetections;
import boofcv.struct.image.ImageDataType;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class QrCodeRegression extends BaseRegression implements ImageRegression {

	EvaluateQrCodeDecoding evaluateMessage = new EvaluateQrCodeDecoding();

	RuntimeSummary runtime;
	DogArray_F64 summaryPeriodMS = new DogArray_F64();

	File workDirectory = new File("./tmp");
	File baseFiducial = new File("data/fiducials/qrcodes");

	String infoString;

	public QrCodeRegression() {
		super(BoofRegressionConstants.TYPE_FIDCUIALS);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime,getClass(),"RUN_QRCode.txt");

		evaluateMessage.out = new PrintStream(new File(directoryMetrics,"ACC_QRCodeMessage.txt"));
		evaluateMessage.err = errorLog;
		BoofRegressionConstants.printGenerator(evaluateMessage.out, getClass());

		ConfigQrCode config = new ConfigQrCode();
//		config.threshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,15);
//		config.threshold.scale = 0.95;

		QrCodeDetector defaultDetector = FactoryFiducial.qrcode(config,imageType);

		process("default",defaultDetector);

		runtime.printSummaryResults();
		runtime.out.close();
		evaluateMessage.out.close();
	}

	private void process(String name, QrCodeDetector detector) throws IOException {
		message(name,detector);
		detection(name,detector);
	}

	private void message(String name, QrCodeDetector detector ) throws IOException {

		evaluateMessage.out.println("# Evaluating "+name);
		evaluateMessage.process(detector,new File(baseFiducial,"decoding"));
		evaluateMessage.out.println();
	}

	private void detection(String name, QrCodeDetector detector) throws IOException {

		infoString = name;

		summaryPeriodMS.reset();
		runtime.out.println(name);
		runtime.printUnitsRow(false);

		PrintStream metricsOut = new PrintStream(new File(directoryMetrics,"ACC_QRCodeDetection_"+name+".txt"));
		BoofRegressionConstants.printGenerator(metricsOut, getClass());

		metricsOut.println("# QR Code Detection Metrics for "+name);
		metricsOut.println("# N  = total number of qr codes in truth set");
		metricsOut.println("# TP = true positive");
		metricsOut.println("# FN = false negative");
		metricsOut.println("# FP = false positive");
		metricsOut.println("# MD = multiple detection of truth qr code");
		metricsOut.println("# Overlap = average overlap across true positives");
		metricsOut.println();

		File groundTruthHome = new File(baseFiducial,"detection");

		List<File> listDirectories = BoofRegressionConstants.listAndSort(groundTruthHome);

		int totalTP = 0;
		int totalN = 0;
		int totalFP = 0;

		boolean foundDataSets = false;
		for( File f: listDirectories ) {
			if( !f.isDirectory() )
				return;

			if( workDirectory.exists() ) {
				BoofRegressionConstants.delete(workDirectory,errorLog);
				if( !workDirectory.mkdirs() ) {
					throw new IOException("Can't create work directory");
				}
			}

			DetectQrCodesInImages evaluateDetect = new DetectQrCodesInImages();
			EvaluateQrCodeDetections evaluateMetrics = new EvaluateQrCodeDetections();
			try {
				evaluateDetect.setOutputDirectory(workDirectory);
				evaluateDetect.process(detector, f);
				summaryPeriodMS.addAll(evaluateDetect.periodMS);
				runtime.printStatsRow(f.getName(),evaluateDetect.periodMS);
				evaluateMetrics.evaluate(workDirectory,f);
			} catch( RuntimeException e ) {
				errorLog.println(e.toString());
				continue;
			}

			foundDataSets = true;
			metricsOut.printf("%20s N %3d TP %3d FN %3d FP %3d MD %3d Overlap %5.1f%%\n",
					f.getName(),
					evaluateMetrics.totalTruth,evaluateMetrics.truePositive,evaluateMetrics.falseNegative,
					evaluateMetrics.falsePositive,evaluateMetrics.multipleDetections,100*evaluateMetrics.averageOverlap);

			totalN += evaluateMetrics.totalTruth;
			totalTP += evaluateMetrics.truePositive;
			totalFP += evaluateMetrics.falsePositive;
		}

		metricsOut.printf("\nSummary N %3d TP %3d FP %3d\n", totalN,totalTP,totalFP);
		metricsOut.close();

		runtime.out.println();
		runtime.saveSummary(name,summaryPeriodMS);

		if( !foundDataSets ) {
			throw new IOException("no data set directories found in "+baseFiducial.getPath());
		}
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{QrCodeRegression.class.getName(),ImageDataType.F32.toString()});
//		RegressionRunner.main(new String[]{QrCodeRegression.class.getName(),ImageDataType.U8.toString()});
	}
}
