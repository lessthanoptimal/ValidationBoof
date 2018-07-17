package boofcv.metrics.sba;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.GrowQueue_F64;

/**
 * Tools for evaluating bundle adjustment
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentEvaluationTools {
    public static double[] computeReprojectionErrorMetrics(BundleAdjustmentSceneStructure structure ,
                                                       BundleAdjustmentObservations observations)
    {

        Point2D_F64 observed = new Point2D_F64();
        Point2D_F64 predicted = new Point2D_F64();

        Point3D_F64 worldPt = new Point3D_F64();
        Point3D_F64 cameraPt = new Point3D_F64();

        GrowQueue_F64 errors = new GrowQueue_F64(observations.getObservationCount());

        for (int viewIdx = 0; viewIdx < observations.views.length; viewIdx++) {
            BundleAdjustmentObservations.View obsView = observations.views[viewIdx];
            BundleAdjustmentSceneStructure.View view = structure.views[viewIdx];
            BundleAdjustmentCamera camera = structure.cameras[view.camera].model;

            for (int pointIdx = 0; pointIdx < obsView.size() ; pointIdx++) {
                obsView.get(pointIdx,observed);
                int pointId = obsView.point.get(pointIdx);
                structure.points[pointId].get(worldPt);

                view.worldToView.transform(worldPt,cameraPt);

                camera.project(cameraPt.x,cameraPt.y,cameraPt.z,predicted);

                errors.add( predicted.distance(observed));
            }
        }

        errors.sort();

        return new double[]{errors.getFraction(0.5),errors.getFraction(0.95)};
    }
}
