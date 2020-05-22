package boofcv.common;

import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public interface TestRegression {
    /**
     * Specifies where performance metrics which are not runtime related should be saved to
     */
    void setMetricsDirectory(String directory );

    /**
     * Specifies where runtime performance metrics are saved to
     */
    void setRuntimeDirectory(String directory );

    PrintStream getErrorStream();
}
