package boofcv.regression;

import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.common.RegressionRunner;
import boofcv.metrics.disparity.EvaluateDisparity;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Regression test for Disparity
 *
 * @author Peter Abeles
 */
public class DisparityRegression extends BaseRegression implements ImageRegression {
	public DisparityRegression() {
		super(BoofRegressionConstants.TYPE_GEOMETRY);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		if( type != ImageDataType.U8 ) {
			throw new IOException("Only supports U8 images");
		}

		PrintStream out = new PrintStream(new File(directory,"ACC_DisparityRegression.txt"));
		BoofRegressionConstants.printGenerator(out,getClass());

		PrintStream outRuntime = new PrintStream(new File(directory,"RUN_DisparityRegression.txt"));
		BoofRegressionConstants.printGenerator(outRuntime,getClass());

		EvaluateDisparity evaluator = new EvaluateDisparity();
		evaluator.out = out;
		evaluator.outRun = outRuntime;
		evaluator.err = getErrorStream();

		try {
			evaluator.process();
		} catch( RuntimeException e ) {
			errorLog.println(e);
		} finally {
			out.close();
			outRuntime.close();
			errorLog.close();
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DisparityRegression.class.getName(),ImageDataType.U8.toString()});
	}
}
