package boofcv.regression;

import boofcv.common.BaseFileRegression;
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
public class CalibrationIntrinsicChangeFRegression extends BaseFileRegression
{
	@Override
	public void process() throws IOException {
		CalibrateFromDetectedPoints alg = new CalibrateFromDetectedPoints();

		alg.setErrorStream(errorLog);
		alg.setOutputResults(new PrintStream(new File(directory, "estimated_calibration.txt")));

		alg.processStereo(new File("data/calibration_stereo/points/bumblebee2_chess.txt"), false);
		alg.processStereo(new File("data/calibration_stereo/points/bumblebee2_chess.txt"), true);
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		RegressionRunner.main(new String[]{CalibrationIntrinsicChangeFRegression.class.getName()});
	}
}
