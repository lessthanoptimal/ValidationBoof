package boofcv.regression;

import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FileRegression;
import boofcv.common.RegressionRunner;
import boofcv.metrics.CalibrateFromDetectedPoints;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Regression test to see if the results of calibration have changed.  Intrinsic parameters are estimated given a known
 * set of calibration points and the estiamted parameters are output.
 *
 * @author Peter Abeles
 */
public class CalibrationIntrinsicChangeFRegression extends BaseRegression implements FileRegression
{
	public CalibrationIntrinsicChangeFRegression() {
		super("");
	}

	@Override
	public void process() throws IOException {
		CalibrateFromDetectedPoints alg = new CalibrateFromDetectedPoints();

		PrintStream output = new PrintStream(new File(directoryMetrics, "ACC_estimated_calibration.txt"));
		BoofRegressionConstants.printGenerator(output,getClass());

		alg.setErrorStream(errorLog);
		alg.setOutputResults(output);

		alg.processStereo(new File("data/calibration_stereo/points/bumblebee2_chess.txt"), false);
		alg.processStereo(new File("data/calibration_stereo/points/bumblebee2_chess.txt"), true);
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		RegressionRunner.main(new String[]{CalibrationIntrinsicChangeFRegression.class.getName()});
	}
}
