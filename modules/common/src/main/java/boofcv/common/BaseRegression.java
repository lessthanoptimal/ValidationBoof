package boofcv.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Implements some common functionality
 *
 * @author Peter Abeles
 */
public abstract class BaseRegression implements TestRegression {

	protected String directoryMetrics;
	protected String directoryRuntime;
	protected PrintStream errorLog = System.err;
	// determines the sub-directory that the results will be written to
	protected String resultsType;

	public BaseRegression(String resultsType) {
		this.resultsType = resultsType;
	}

	@Override
	public void setMetricsDirectory(String directory) {
		File f = checkAndCreatePath(directory);
		this.directoryMetrics = f.getPath();

		File tmp = BoofRegressionConstants.tempDir();
		if( !tmp.exists() ) {
			if( !tmp.mkdir() )
				throw new RuntimeException("Can't create tmp directory");
		}

		try {
			errorLog = new PrintStream(new File(directory,"ERRORLOG_"+getClass().getSimpleName()+".txt"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private File checkAndCreatePath(String directory) {
		File f = new File(directory,resultsType);
		if( !f.exists() ) {
			if( !f.mkdirs() ) {
				throw new RuntimeException("Can't make output directory "+f.getPath());
			}
		}
		return f;
	}

	@Override
	public void setRuntimeDirectory(String directory) {
		File f = checkAndCreatePath(directory);
		this.directoryRuntime = f.getPath();
	}

	@Override
	public PrintStream getErrorStream() {
		return errorLog;
	}
}
