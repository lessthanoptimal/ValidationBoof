package boofcv.common;

import java.io.IOException;

/**
 * @author Peter Abeles
 */
public interface FileRegression extends TestRegression {
    void process() throws IOException;
}
