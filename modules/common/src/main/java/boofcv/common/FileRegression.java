package boofcv.common;

import java.io.IOException;

/**
 * @author Peter Abeles
 */
public interface FileRegression {
    void setOutputDirectory( String directory );

    void process() throws IOException;
}
