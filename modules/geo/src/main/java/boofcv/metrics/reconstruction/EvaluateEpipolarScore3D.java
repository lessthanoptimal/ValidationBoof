package boofcv.metrics.reconstruction;

import boofcv.alg.structure.EpipolarScore3D;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Evaluates EpipolarScore3D by giving it synthetic scenes with known relationships.
 *
 * @author Peter Abeles
 */
public class EvaluateEpipolarScore3D {
    // TODO detect pure translation vs rotation
    //      general and planar
    //      variable amount of noise
    // TODO slowly increase amount of translation and see if it detects its increasing.
    //      do a grid with the point cloud at different distances
    //      do this for
    // TODO evaluate amount of translation with inlier sets matching
    // TODO evaluate amount of translation in general

    EpipolarScore3D score3D;

    public double defaultNoisePixels = 0.5;
    public double defaultInlierFraction = 0.75;

    CameraPinholeBrown cameraA = new CameraPinholeBrown(2).fsetK(500,500,0.0,500,600,1000,1200);
    CameraPinholeBrown cameraB = new CameraPinholeBrown(2).fsetK(600,600,0.0,500,600,1000,1200);

    Random rand = new Random(32);
    List<Point3D_F64> cloud = new ArrayList<>();

    // work space
    DMatrixRMaj fundamental = new DMatrixRMaj(3,3);
    DogArray_I32 inliersIdx = new DogArray_I32();

    protected void evaluatePureWithNoise() {
        cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2.0), -1, 1, 500, rand);


        score3D.process(cameraA, cameraB, pairs, fundamental, inliersIdx);
    }

}
