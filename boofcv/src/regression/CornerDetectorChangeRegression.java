package regression;

import boofcv.struct.image.ImageDataType;
import validate.fast.DetectFast;

import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class CornerDetectorChangeRegression extends BaseTextFileRegression {

	@Override
	public void process( ImageDataType type ) throws IOException {
		DetectFast.detect(directory,ImageDataType.typeToClass(type));
	}

	public static void main(String[] args) throws IOException {

		CornerDetectorChangeRegression app = new CornerDetectorChangeRegression();

		app.setOutputDirectory(GenerateRegressionData.CURRENT_DIRECTORY+"/"+ImageDataType.U8+"/");
		app.process(ImageDataType.U8);
	}
}
