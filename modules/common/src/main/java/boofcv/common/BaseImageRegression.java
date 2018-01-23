package boofcv.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Implements some common functionality
 *
 * @author Peter Abeles
 */
public abstract class BaseImageRegression implements ImageRegression {


	protected String directory;
	protected PrintStream errorLog;
	// determines the sub-directory that the results will be written to
	protected String resultsType;

	public BaseImageRegression(String resultsType) {
		this.resultsType = resultsType;
	}

	@Override
	public void setOutputDirectory(String directory) {
		File f = new File(directory,resultsType);
		if( !f.exists() ) {
			if( !f.mkdirs() ) {
				throw new RuntimeException("Can't make output directory "+f.getPath());
			}
		}
		this.directory = f.getPath();

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
}
