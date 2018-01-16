package regression;

import boofcv.struct.image.ImageDataType;
import validate.features.corner.ComparePreviousCorner;

import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class CornerDetectorChangeRegression extends BaseTextFileRegression {

	@Override
	public void process( ImageDataType type ) throws IOException {
		PrintStream out = new PrintStream(directory+"/detect_corner_change.txt");
		out.println("# Checks to change in the behavior of corner detectors");
		out.println("# detector (change in total features) (difference in location)");
		ComparePreviousCorner compare = new ComparePreviousCorner(out);
		compare.errorLog = errorLog;
		compare.generateAll(ImageDataType.typeToSingleClass(type));
	}

	public static void main(String[] args) throws IOException {

		CornerDetectorChangeRegression app = new CornerDetectorChangeRegression();

		app.setOutputDirectory(GenerateRegressionData.CURRENT_DIRECTORY+"/"+ImageDataType.U8+"/");
		app.process(ImageDataType.U8);
	}
}
