package regression;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import validate.trackrect.*;

import java.io.IOException;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ObjectTrackingRegression extends BaseTextFileRegression {

	@Override
	public void process( ImageDataType type ) throws IOException {

		List<FactoryEvaluationTrackerObjectQuad.Info> all =
				FactoryEvaluationTrackerObjectQuad.createRegression(type);

		// compute raw detections
		for( FactoryEvaluationTrackerObjectQuad.Info info : all ) {
			performMILData(info.name, info.tracker, info.imageType);
			EvaluateResultsMilTrackData.process(directory,info.name);

			performTLD(info.name,info.tracker,info.imageType);
			EvaluateResultsTldData.process(directory, info.name);
		}

		errorLog.close();
	}

	public <Input extends ImageBase>
	void performMILData( String trackerName , TrackerObjectQuad<Input> tracker , ImageType<Input> imageType ) {

		GenerateDetectionsMilTrackData<Input> generator = new GenerateDetectionsMilTrackData(imageType);

		for( String m : GenerateDetectionsMilTrackData.videos ) {
			try {
				generator.evaluate(m,trackerName,tracker);
			} catch( RuntimeException e ) {
				errorLog.println("FAILED "+trackerName+" on "+m);
				errorLog.println(e);
				e.printStackTrace(errorLog);
			}
		}
	}

	public <Input extends ImageBase>
	void performTLD( String trackerName , TrackerObjectQuad<Input> tracker , ImageType<Input> imageType ) {

		GenerateDetectionsTldData<Input> generator = new GenerateDetectionsTldData(imageType);

		for( String dataName : GenerateDetectionsTldData.videos ) {
			try {
				generator.evaluate(dataName,trackerName,tracker);
			} catch( RuntimeException e ) {
				errorLog.println("FAILED "+trackerName+" on "+dataName);
				e.printStackTrace(errorLog);
				System.out.println("------------------------------------------------------");
			}
		}
	}

	public static void main(String[] args) throws IOException {

		ObjectTrackingRegression app = new ObjectTrackingRegression();

		ImageDataType type = ImageDataType.F32;

		app.setOutputDirectory(GenerateRegressionData.CURRENT_DIRECTORY+"/"+type+"/");
		app.process(type);
	}
}
