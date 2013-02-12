package validate.vo;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.StereoParameters;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class WrapParseKITTI implements SequenceStereoImages {

	int frameNumber = 0;

	StereoParameters param = new StereoParameters();

	ParseKITTI parser;

	public WrapParseKITTI( String baseDirectory , String sequence ) {
		parser = new ParseKITTI(baseDirectory+"/sequences/"+sequence);
		parser.loadCalibration(baseDirectory+"/sequences/"+sequence+"/calib.txt");
		parser.loadTruth(baseDirectory+"/poses/"+sequence+".txt");

		DenseMatrix64F leftP = parser.getLeftProjection();
		DenseMatrix64F rightP = parser.getRightProjection();

		// load the first image to get its dimension
		parser.loadFrame(0);
		int leftWidth = parser.getLeftImage().getWidth();
		int leftHeight = parser.getLeftImage().getHeight();
		int rightWidth = parser.getRightImage().getWidth();
		int rightHeight = parser.getRightImage().getHeight();


		// Make a few assumptions here
		DenseMatrix64F K = CommonOps.extract(leftP,0,3,0,3);
		DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
		CommonOps.invert(K,K_inv);

		param.left = PerspectiveOps.matrixToParam(K,leftWidth,leftHeight,false,null);
		param.right = PerspectiveOps.matrixToParam(K,rightWidth,rightHeight,false,null);
		param.left.radial = new double[0];
		param.right.radial = new double[0];


		param.rightToLeft = new Se3_F64();
		param.rightToLeft.getT().set(-rightP.get(0,3),-rightP.get(1,3),-rightP.get(2,3));

		GeometryMath_F64.mult(K_inv,param.rightToLeft.getT(),param.rightToLeft.getT());
	}

	public WrapParseKITTI( String baseDirectory , String sequence , int initialFrame ) {
		this(baseDirectory,sequence);
		frameNumber = initialFrame;
	}

	@Override
	public boolean next() {
		return parser.loadFrame(frameNumber++);
	}

	@Override
	public BufferedImage getLeft() {
		return parser.getLeftImage();
	}

	@Override
	public BufferedImage getRight() {
		return parser.getRightImage();
	}

	@Override
	public Se3_F64 getLeftToWorld() {
		return parser.getLeftToWorld();
	}

	@Override
	public StereoParameters getCalibration() {
		return param;
	}

	@Override
	public boolean isTruthKnown() {
		return true;
	}

	@Override
	public boolean isCalibrationFixed() {
		return true;
	}
}
