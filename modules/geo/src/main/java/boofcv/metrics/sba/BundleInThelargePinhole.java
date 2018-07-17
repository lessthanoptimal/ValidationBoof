package boofcv.metrics.sba;

import boofcv.alg.geo.bundle.cameras.BundleAdjustmentPinholeSimplified;
import georegression.struct.point.Point2D_F64;

/**
 * Make an adjustment for the model used in the paper
 *
 * @author Peter Abeles
 */
public class BundleInThelargePinhole extends BundleAdjustmentPinholeSimplified {
    @Override
    public void project(double camX, double camY, double camZ, Point2D_F64 output) {
        super.project(camX, camY, -camZ, output);
    }

    @Override
    public void jacobian(double X, double Y, double Z, double[] inputX, double[] inputY, double[] calibX, double[] calibY) {
        super.jacobian(X, Y, -Z, inputX, inputY, calibX, calibY);
    }

    @Override
    public void jacobian(double X, double Y, double Z, double[] inputX, double[] inputY) {
        super.jacobian(X, Y, -Z, inputX, inputY);
    }
}
