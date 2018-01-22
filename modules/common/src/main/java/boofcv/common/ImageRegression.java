package boofcv.common;

import boofcv.struct.image.ImageDataType;

import java.io.IOException;

/**
 * @author Peter Abeles
 */
public interface ImageRegression {

	void setOutputDirectory( String directory );

	void process( ImageDataType type ) throws IOException;
}
