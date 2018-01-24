package boofcv.common;

import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public interface TestRegression {
    void setOutputDirectory( String directory );
    PrintStream getErrorStream();
}
