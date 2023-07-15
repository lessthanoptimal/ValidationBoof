package boofcv.regression;

import boofcv.common.*;
import boofcv.metrics.disparity.EvaluateDisparity;
import boofcv.metrics.disparity.EvaluateDisparityByDistance;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Regression test for Disparity
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DisparityDistanceRegression extends BaseRegression implements ImageRegression {
	public DisparityDistanceRegression() {
		super(BoofRegressionConstants.TYPE_GEOMETRY);
	}

	@Override
	public void process( ImageDataType type ) throws IOException {
		PrintStream out = new PrintStream(new File(directoryMetrics, "ACC_DisparityDistanceRegression.txt"));
		BoofRegressionConstants.printGenerator(out, getClass());

		Class imageType = ImageType.getImageClass(ImageType.Family.GRAY, type);
		var evaluator = new EvaluateDisparityByDistance<>(imageType);

		var runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime, getClass(), "RUN_DisparityDistanceRegression.txt");
		runtime.printUnitsRow(true);

		out.println("# Computes range error as a function from distance in a simulated planar scene");
		out.printf("# baseline=%.4f min=%.2f max=%.2f trials=%d\n",
				evaluator.BASELINE, evaluator.MIN_DISTANCE, evaluator.MAX_DISTANCE, evaluator.trials);
		out.println("# \"lock\" indicates if the error sign is random. Ideally it should be 0.5.");
		out.println("# errors are fractional errors relative to true distance");
		out.println("#         name            lock | err25  err50  | err95  errMAX");
		out.println();

		try {
			// Disparity will max out around 7
			List<EvaluateDisparity.TestSubject> subjects = EvaluateDisparity.createAlgorithms(14, imageType);
			for (EvaluateDisparity.TestSubject subject : subjects) {
				System.out.println("algorithm: " + subject.name);
				evaluator.evaluate(subject.alg);
				out.printf("%25s %.2f | %.4f %.4f | %.4f %.4f\n", subject.name, evaluator.pixelLockFraction,
						evaluator.errorFraction.getFraction(0.25), evaluator.errorFraction.getFraction(0.5),
						evaluator.errorFraction.getFraction(0.95), evaluator.errorFraction.getFraction(1.0));
				runtime.printStatsRow(subject.name, evaluator.processingTimeMS);

				// Sanity check
				BoofMiscOps.checkEq(evaluator.errorFraction.size, evaluator.trials);
			}
		} catch (RuntimeException e) {
			e.printStackTrace(errorLog);
		} finally {
			out.close();
			runtime.out.close();
			errorLog.close();
		}
	}

	public static void main( String[] args ) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DisparityDistanceRegression.class.getName(), ImageDataType.F32.toString()});
	}
}
