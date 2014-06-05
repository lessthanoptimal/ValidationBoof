package validate.vo;

import boofcv.struct.calib.StereoParameters;
import georegression.struct.se.Se3_F64;

import java.awt.image.BufferedImage;

/**
 * Image sequence for a stereo camera system.
 *
 * @author Peter Abeles
 */
public interface SequenceStereoImages {

	public boolean next();

	public BufferedImage getLeft();

	public BufferedImage getRight();

	public Se3_F64 getLeftToWorld();

	public StereoParameters getCalibration();

	public boolean isTruthKnown();

	public boolean isCalibrationFixed();

}
