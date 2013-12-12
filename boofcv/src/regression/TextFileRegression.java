package regression;

import boofcv.struct.image.ImageDataType;

import java.io.IOException;
import java.util.List;

/**
 * @author Peter Abeles
 */
public interface TextFileRegression {

	public void setOutputDirectory( String directory );

	public void process( ImageDataType type ) throws IOException;

	public List<String> getFileNames();
}
