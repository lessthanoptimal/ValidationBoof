package regression;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.ImageDataType;
import org.apache.commons.io.FileUtils;
import validate.fiducial.qrcode.DetectQrCodesInImages;
import validate.fiducial.qrcode.EvaluateQrCodeDetections;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

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

		ConfigQrCode config = new ConfigQrCode();
//		config.polygon.detector.minimumEdgeIntensity = 5;
//		config.polygon.minimumRefineEdgeIntensity = 10;
		QrCodeDetector defaultDetector = FactoryFiducial.qrcode(config,imageType);

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

		File groundTruthHome = new File(baseFiducial,"detection");

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
			EvaluateQrCodeDetections evaluateMetrics = new EvaluateQrCodeDetections();
			try {
				evaluateDetect.setOutputDirectory(workDirectory);
				evaluateDetect.process(detector, f);
				runtimeOut.printf("%20s %8.3f (ms)\n",f.getName(),+evaluateDetect.averageMS);
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

		metricsOut.printf("\nSumary N %3d TP %3d FP %3d\n", totalN,totalTP,totalFP);

		runtimeOut.close();
		metricsOut.close();

		if( !foundDataSets ) {
			throw new IOException("no data set directories found in "+baseFiducial.getPath());
		}

	}
}
