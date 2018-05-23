package boofcv.metrics.sfm;

import boofcv.struct.calib.CameraPinhole;
import georegression.metric.UtilAngle;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class GeneratePlanarPnPObservation {
    // TODO 4-points - fixed distance. random roll - fixed yaw
    // TODO 500-points - fixed distance. random roll - fixed yaw

    Random random = new Random(234);

    // Camera parameters
    double hfov = 80; // degrees
    int width = 640;
    int height = 480;
    CameraPinhole intrinsic = new CameraPinhole();

    // Length of a size on the target
    double sizeLength = 1;

    // +- rotation applies to target in target frame
    double rangeRotX = 0;
    double rangeRotY = 0;
    double rangeRotZ = 0;


    public GeneratePlanarPnPObservation() {
        double fx = (width/2)/Math.tan(UtilAngle.radian(hfov)/2);
        intrinsic.fsetK(fx,fx,0,width/2,height/2,width,height);
    }

    /**
     * Given the distance the target is away
     * @param distanceZ Distance away the target is from the camera
     * @param N Number of observations to generate
     */
    public void createObservation( double distanceZ , int N  )
    {

    }
}
