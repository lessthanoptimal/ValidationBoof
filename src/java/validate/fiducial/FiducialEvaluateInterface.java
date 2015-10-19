package validate.fiducial;

import java.io.File;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public interface FiducialEvaluateInterface {

	void evaluate( File resultsDirectory , File dataset );

	void setOutputResults(PrintStream outputResults);

	void setErrorStream(PrintStream err);

	int getTotalExpected();
	int getTotalCorrect();
}
