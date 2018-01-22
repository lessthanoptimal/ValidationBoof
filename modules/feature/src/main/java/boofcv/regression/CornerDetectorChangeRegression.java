package boofcv.regression;

import boofcv.common.BaseImageRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.RegressionRunner;
import boofcv.metrics.corner.ComparePreviousCorner;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class CornerDetectorChangeRegression extends BaseImageRegression {

	@Override
	public void process( ImageDataType type ) throws IOException {
		PrintStream out = new PrintStream(new File(directory,"detect_corner_change.txt"));
		out.println("# Checks to change in the behavior of corner detectors");
		out.println("# detector (change in total features) (difference in location)");
		ComparePreviousCorner compare = new ComparePreviousCorner(out);
		compare.errorLog = errorLog;
		compare.generateAll(ImageDataType.typeToSingleClass(type));
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{CornerDetectorChangeRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
