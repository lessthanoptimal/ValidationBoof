package regression;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import validate.trackrect.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ObjectTrackingRegression implements TextFileRegression {

	String directory;
	PrintStream errorLog;

	@Override
	public void setOutputDirectory(String directory) {
		this.directory = directory;
		try {
			errorLog = new PrintStream(directory+"ERRORLOG_ObjectTracking.txt");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

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

	@Override
	public List<String> getFileNames() {
		List<String> names = new ArrayList<String>();

		String milLibraries[]=new String[]{"BoofCV-TLD","BoofCV-Circulant","BoofCV-Comaniciu"};
		for( String s : milLibraries) {
			names.add( "MILTrackData_"+s+".txt");
		}

		return names;
	}

	public static void main(String[] args) throws IOException {

		ObjectTrackingRegression app = new ObjectTrackingRegression();

		ImageDataType type = ImageDataType.F32;

		app.setOutputDirectory(RegressionManagerApp.CURRENT_DIRECTORY+"/"+type+"/");
		app.process(type);
	}
}
