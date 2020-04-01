package boofcv.metrics.vo;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.io.calibration.CalibrationIO;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.File;

/**
 * @author Peter Abeles
 */
public class ConvertKittiToBoofStereoCalibration {
	public static void main(String[] args) {
		String path = "data/KITTI/sequences/03";
		ParseKITTI parser = new ParseKITTI(path);
		parser.loadCalibration(path+"/"+"calib.txt");

		DMatrixRMaj leftP = parser.getLeftProjection();
		DMatrixRMaj rightP = parser.getRightProjection();

		// load the first image to get its dimension
		parser.loadFrame(0);
		int leftWidth = parser.getLeftImage().getWidth();
		int leftHeight = parser.getLeftImage().getHeight();
		int rightWidth = parser.getRightImage().getWidth();
		int rightHeight = parser.getRightImage().getHeight();

		// Make a few assumptions here
		DMatrixRMaj K = CommonOps_DDRM.extract(leftP,0,3,0,3);
		DMatrixRMaj K_inv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(K,K_inv);

		StereoParameters param = new StereoParameters();
		param.left = PerspectiveOps.matrixToPinhole(K,leftWidth,leftHeight,new CameraPinholeBrown());
		param.right = PerspectiveOps.matrixToPinhole(K,rightWidth,rightHeight,new CameraPinholeBrown());
		param.left.radial = new double[0];
		param.right.radial = new double[0];


		param.rightToLeft = new Se3_F64();
		param.rightToLeft.getT().set(-rightP.get(0,3),-rightP.get(1,3),-rightP.get(2,3));

		CalibrationIO.save(param,new File(path,"boofcv_stereo.yaml"));
	}
}
