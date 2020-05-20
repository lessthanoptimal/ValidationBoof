package boofcv.regression;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.common.*;
import boofcv.metrics.object.*;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.GrowQueue_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ObjectTrackingRegression extends BaseRegression implements ImageRegression {

	File trackingOutputDir = BoofRegressionConstants.tempDir();

	RuntimeSummary outputSpeed;
	GrowQueue_F64 summaryPeriod = new GrowQueue_F64();

	public ObjectTrackingRegression() {
		super(BoofRegressionConstants.TYPE_TRACKING);
	}

	@Override
	public void process( ImageDataType type ) throws IOException {

		List<FactoryEvaluationTrackerObjectQuad.Info> all =
				FactoryEvaluationTrackerObjectQuad.createRegression(type);

		outputSpeed = new RuntimeSummary();
		outputSpeed.out = new PrintStream(new File(directoryRuntime, "RUN_ObjectTracking.txt"));
		BoofRegressionConstants.printGenerator(outputSpeed.out, getClass());
		outputSpeed.out.println("# All times are in milliseconds");
		outputSpeed.out.println();

		// compute raw detections
		for( FactoryEvaluationTrackerObjectQuad.Info info : all ) {
			summaryPeriod.reset();
			outputSpeed.out.println(info.name);
			outputSpeed.printHeader(false);
			performMILData(info.name, info.tracker, info.imageType);
			EvaluateResultsMilTrackData.process(directoryMetrics,info.name,trackingOutputDir);

			performTLD(info.name,info.tracker,info.imageType);
			EvaluateResultsTldData.process(directoryMetrics, info.name,trackingOutputDir);

			outputSpeed.out.println();
			outputSpeed.saveSummary(info.name,summaryPeriod);
		}

		outputSpeed.printSummary();
		outputSpeed.out.close();
	}

	public <Input extends ImageBase<Input>>
	void performMILData( String trackerName , TrackerObjectQuad<Input> tracker , ImageType<Input> imageType ) {

		GenerateDetectionsMilTrackData<Input> generator = new GenerateDetectionsMilTrackData<>(imageType);
		generator.setOutputDirectory(trackingOutputDir);

		for( String videoName : GenerateDetectionsMilTrackData.videos ) {
			try {
				generator.evaluate(videoName,trackerName,tracker);
				summaryPeriod.addAll(generator.periodMS);
				outputSpeed.printStats(videoName,generator.periodMS);
			} catch( RuntimeException e ) {
				outputSpeed.out.printf("  %20s FAILED\n",videoName);
				errorLog.println("FAILED "+trackerName+" on "+videoName);
				errorLog.println(e);
				e.printStackTrace(errorLog);
			} finally {
				outputSpeed.out.flush();
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
				summaryPeriod.addAll(generator.periodMS);
				outputSpeed.printStats(dataName,generator.periodMS);
			} catch( RuntimeException e ) {
				outputSpeed.out.printf("  %20s FAILED\n",dataName);
				errorLog.println("FAILED "+trackerName+" on "+dataName);
				e.printStackTrace(errorLog);
				System.out.println("------------------------------------------------------");
			} finally {
				outputSpeed.out.flush();
			}
		}
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{ObjectTrackingRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
