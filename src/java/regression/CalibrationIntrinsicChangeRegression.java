package regression;

import boofcv.struct.image.ImageDataType;
import validate.calib.CalibrateFromDetectedPoints;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Regression test to see if the results of calibration have changed.  Intrinsic parameters are estimated given a known
 * set of calibration points and the estiamted parameters are output.
 *
 * @author Peter Abeles
 */
public class CalibrationIntrinsicChangeRegression extends BaseTextFileRegression
{
	@Override
	public void process(ImageDataType type) throws IOException {
		if( type != null )
			throw new RuntimeException("No image type needed");

		CalibrateFromDetectedPoints alg = new CalibrateFromDetectedPoints();

		alg.setErrorStream(errorLog);
		alg.setOutputResults(new PrintStream(new File(directory, "estimated_calibration.txt")));

		alg.processStereo(new File("data/calib/stereo/points/bumblebee2_chess.txt"));
	}
}
