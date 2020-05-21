package boofcv.regression;

import boofcv.common.*;
import boofcv.metrics.disparity.EvaluateDisparity;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

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
		PrintStream out = new PrintStream(new File(directoryMetrics,"ACC_DisparityRegression.txt"));
		BoofRegressionConstants.printGenerator(out,getClass());

		RuntimeSummary runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime,getClass(),"RUN_DisparityRegression.txt");

		EvaluateDisparity evaluator = new EvaluateDisparity(ImageType.getImageClass(ImageType.Family.GRAY,type));
		evaluator.out = out;
		evaluator.err = getErrorStream();
		evaluator.runtime = runtime;

		try {
			evaluator.process();
		} catch( RuntimeException e ) {
			e.printStackTrace(errorLog);
		} finally {
			out.close();
			runtime.out.close();
			errorLog.close();
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DisparityRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
