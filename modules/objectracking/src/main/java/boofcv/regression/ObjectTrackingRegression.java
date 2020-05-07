package boofcv.regression;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.common.RegressionRunner;
import boofcv.metrics.object.*;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import org.ddogleg.stats.UtilStatisticsQueue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ObjectTrackingRegression extends BaseRegression implements ImageRegression {

	File trackingOutputDir = BoofRegressionConstants.tempDir();

	PrintStream outputSpeed;

	public ObjectTrackingRegression() {
		super(BoofRegressionConstants.TYPE_TRACKING);
	}

	@Override
	public void process( ImageDataType type ) throws IOException {

		List<FactoryEvaluationTrackerObjectQuad.Info> all =
				FactoryEvaluationTrackerObjectQuad.createRegression(type);

		outputSpeed = new PrintStream(new File(directory, "RUN_ObjectTracking.txt"));
		BoofRegressionConstants.printGenerator(outputSpeed, getClass());

		// compute raw detections
		for( FactoryEvaluationTrackerObjectQuad.Info info : all ) {
			outputSpeed.println("Tracker: "+info.name);
			performMILData(info.name, info.tracker, info.imageType);
			EvaluateResultsMilTrackData.process(directory,info.name,trackingOutputDir);

			performTLD(info.name,info.tracker,info.imageType);
			EvaluateResultsTldData.process(directory, info.name,trackingOutputDir);
		}

		outputSpeed.close();
	}

	public <Input extends ImageBase<Input>>
	void performMILData( String trackerName , TrackerObjectQuad<Input> tracker , ImageType<Input> imageType ) {

		GenerateDetectionsMilTrackData<Input> generator = new GenerateDetectionsMilTrackData<>(imageType);
		generator.setOutputDirectory(trackingOutputDir);

		for( String m : GenerateDetectionsMilTrackData.videos ) {
			try {
				generator.evaluate(m,trackerName,tracker);
				generator.periodMS.sort();
				double mean = UtilStatisticsQueue.mean(generator.periodMS);
				double p50 = generator.periodMS.getFraction(0.5);
				double p97 = generator.periodMS.getFraction(0.97);
				outputSpeed.printf("  %20s N %4d ave %7.2f p50 %7.2f p97 %7.2f\n",
						m,generator.periodMS.size,mean,p50,p97);
			} catch( RuntimeException e ) {
				outputSpeed.printf("  %20s FAILED\n",m);
				errorLog.println("FAILED "+trackerName+" on "+m);
				errorLog.println(e);
				e.printStackTrace(errorLog);
			} finally {
				outputSpeed.flush();
			}
		}
	}

	public <Input extends ImageBase<Input>>
	void performTLD( String trackerName , TrackerObjectQuad<Input> tracker , ImageType<Input> imageType ) {

		GenerateDetectionsTldData<Input> generator = new GenerateDetectionsTldData<>(imageType);
		generator.setOutputDirectory(trackingOutputDir);

		for( String dataName : GenerateDetectionsTldData.videos ) {
			try {
				generator.evaluate(dataName,trackerName,tracker);
				generator.periodMS.sort();
				double mean = UtilStatisticsQueue.mean(generator.periodMS);
				double p50 = generator.periodMS.getFraction(0.5);
				double p97 = generator.periodMS.getFraction(0.97);
				outputSpeed.printf("  %20s N %4d ave %7.2f p50 %7.2f p97 %7.2f\n",
						dataName,generator.periodMS.size,mean,p50,p97);
			} catch( RuntimeException e ) {
				outputSpeed.printf("  %20s FAILED\n",dataName);
				errorLog.println("FAILED "+trackerName+" on "+dataName);
				e.printStackTrace(errorLog);
				System.out.println("------------------------------------------------------");
			} finally {
				outputSpeed.flush();
			}
		}
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{ObjectTrackingRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
