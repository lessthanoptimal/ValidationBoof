package regression;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Implements some common functionality
 *
 * @author Peter Abeles
 */
public abstract class BaseTextFileRegression implements TextFileRegression {


	protected String directory;
	protected PrintStream errorLog;

	@Override
	public void setOutputDirectory(String directory) {
		this.directory = directory;

		File tmp = new File("tmp");
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
