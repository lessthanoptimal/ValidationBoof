package regression;

import boofcv.struct.image.ImageDataType;
import validate.fast.DetectFast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CornerDetectorChangeRegression implements TextFileRegression {
	String directory;

	@Override
	public void setOutputDirectory(String directory) {
		this.directory = directory;
	}

	public void process( ImageDataType type ) throws IOException {
		DetectFast.detect(directory,ImageDataType.typeToClass(type));
	}

	@Override
	public List<String> getFileNames() {
		List<String> names = new ArrayList<String>();

		names.add(DetectFast.FILE_NAME);

		return names;
	}

	public static void main(String[] args) throws IOException {

		CornerDetectorChangeRegression app = new CornerDetectorChangeRegression();

		app.setOutputDirectory(RegressionManagerApp.CURRENT_DIRECTORY+"/"+ImageDataType.U8+"/");
		app.process(ImageDataType.U8);
	}
}
