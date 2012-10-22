package validate.vo;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.StereoParameters;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class WrapParseLeuven07 implements SequenceStereoImages {

	int index = 0;
	ParseLeuven07 data;

	StereoParameters stereoParam;

	Se3_F64 worldToLeft = new Se3_F64();

	public WrapParseLeuven07(ParseLeuven07 data) {
		this.data = data;

		stereoParam = new StereoParameters();
		stereoParam.left = new IntrinsicParameters();
		stereoParam.right = new IntrinsicParameters();
		stereoParam.rightToLeft = new Se3_F64();

		stereoParam.left.radial = new double[2];
		stereoParam.right.radial = new double[2];
	}

	@Override
	public boolean next() {
		if( data.loadFrame(index++) ) {
			data.getLeftToWorld().invert(worldToLeft);

			return true;
		}
		return false;
	}

	@Override
	public BufferedImage getLeft() {
		return data.getLeftImage();
	}

	@Override
	public BufferedImage getRight() {
		return data.getRightImage();
	}

	@Override
	public Se3_F64 getLeftToWorld() {
		return data.getLeftToWorld();
	}

	@Override
	public StereoParameters getCalibration()
	{
		PerspectiveOps.matrixToParam(data.getLeftK(),360,288,false,stereoParam.left);
		PerspectiveOps.matrixToParam(data.getRightK(),360,288,false,stereoParam.right);

		data.getRightToWorld().concat(worldToLeft,stereoParam.rightToLeft);

		return stereoParam;
	}


	@Override
	public boolean isTruthKnown() {
		return true;
	}

	@Override
	public boolean isCalibrationFixed() {
		return false;
	}

}
