package boofcv.regression;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.common.*;
import boofcv.metrics.object.*;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ObjectTrackingRegression extends BaseRegression implements ImageRegression {

	File trackingOutputDir = BoofRegressionConstants.tempDir();

	RuntimeSummary outputSpeed;
	DogArray_F64 summaryPeriod = new DogArray_F64();

	// Should it save how long it took to process after track was lost? Some code stops processing after that happens
	public boolean recordTimingAfterLostTrack = true;

	public ObjectTrackingRegression() {
		super(BoofRegressionConstants.TYPE_TRACKING);
	}

	@Override
	public void process( ImageDataType type ) throws IOException {

		List<FactoryEvaluationTrackerObjectQuad.Info> all =
				FactoryEvaluationTrackerObjectQuad.createRegression(type);

		outputSpeed = new RuntimeSummary();
		outputSpeed.initializeLog(directoryRuntime, getClass(),"RUN_ObjectTracking.txt");

		// compute raw detections
		for( FactoryEvaluationTrackerObjectQuad.Info info : all ) {
			recordTimingAfterLostTrack = info.recordTimingAfterLostTrack;
			summaryPeriod.reset();
			outputSpeed.out.println(info.name);
			outputSpeed.printUnitsRow(false);
			performMILData(info.name, info.tracker, info.imageType);
			EvaluateResultsMilTrackData.process(directoryMetrics,info.name,trackingOutputDir);

			performTLD(info.name,info.tracker,info.imageType);
			EvaluateResultsTldData.process(directoryMetrics, info.name,trackingOutputDir);

			outputSpeed.out.println();
			outputSpeed.saveSummary(info.name,summaryPeriod);
		}

		outputSpeed.printSummaryResults();
		outputSpeed.out.close();
	}

	public <Input extends ImageBase<Input>>
	void performMILData( String trackerName , TrackerObjectQuad<Input> tracker , ImageType<Input> imageType ) {

		GenerateDetectionsMilTrackData<Input> generator = new GenerateDetectionsMilTrackData<>(imageType);
		generator.setOutputDirectory(trackingOutputDir);
		generator.err = errorLog;
		generator.recordTimingAfterLostTrack = recordTimingAfterLostTrack;

		for( String videoName : GenerateDetectionsMilTrackData.videos ) {
			try {
				generator.evaluate(videoName,trackerName,tracker);
				summaryPeriod.addAll(generator.periodMS);
				outputSpeed.printStatsRow(videoName,generator.periodMS);
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
		generator.err = errorLog;
		generator.recordTimingAfterLostTrack = recordTimingAfterLostTrack;

		for( String dataName : GenerateDetectionsTldData.videos ) {
			try {
				generator.evaluate(dataName,trackerName,tracker);
				summaryPeriod.addAll(generator.periodMS);
				outputSpeed.printStatsRow(dataName,generator.periodMS);
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
