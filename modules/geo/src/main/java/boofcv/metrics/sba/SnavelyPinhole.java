package boofcv.metrics.sba;

import boofcv.alg.geo.bundle.cameras.BundleAdjustmentPinholeSimplified;
import georegression.struct.point.Point2D_F64;

/**
 * Bundler and Bundle Adjustment in the Large use a different coordinate system. This
 * converts it into what BoofCV understands by applying a negative sign to the Z coordinate.
 *
 * @author Peter Abeles
 */
public class SnavelyPinhole extends BundleAdjustmentPinholeSimplified {
    @Override
    public void project(double camX, double camY, double camZ, Point2D_F64 output) {
        super.project(camX, camY, -camZ, output);
    }

    @Override
    public void jacobian(double X, double Y, double Z, double[] inputX, double[] inputY,boolean computeIntrinsic, double[] calibX, double[] calibY) {
        super.jacobian(X, Y, -Z, inputX, inputY,computeIntrinsic, calibX, calibY);
    }
}
