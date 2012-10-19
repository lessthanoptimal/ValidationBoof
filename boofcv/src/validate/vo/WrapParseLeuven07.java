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

	Se3_F64 rightToWorld = new Se3_F64();
	Se3_F64 leftToWorld = new Se3_F64();

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
			data.getWorldToLeft().invert(leftToWorld);
			data.getWorldToRight().invert(rightToWorld);
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
		return leftToWorld;
	}

	@Override
	public StereoParameters getCalibration() {
		DenseMatrix64F K = data.getLeftK();
		PerspectiveOps.matrixToParam(K,360,288,false,stereoParam.left);

		K = data.getRightK();
		PerspectiveOps.matrixToParam(K,360,288,false,stereoParam.right);

		Se3_F64 worldToLeft = data.getWorldToLeft();

		rightToWorld.concat(worldToLeft,stereoParam.rightToLeft);

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
