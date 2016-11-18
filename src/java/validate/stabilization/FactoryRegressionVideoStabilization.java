package validate.stabilization;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.PlToGrayMotion2D;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.struct.Tuple2;

/**
 * @author Peter Abeles
 */
public class FactoryRegressionVideoStabilization {
    public static <T extends ImageBase>
    Tuple2<String,StitchingFromMotion2D<T,?>> createKlt(ImageType<T> imageType )
    {
        // Configure the feature detector
        ConfigGeneralDetector confDetector = new ConfigGeneralDetector();
        confDetector.threshold = 10;
        confDetector.maxFeatures = 300;
        confDetector.radius = 2;

        Class imageClass = imageType.getImageClass();
        Class derivClass = GImageDerivativeOps.getDerivativeType(imageClass);

        // Use a KLT tracker
        PointTracker<T> tracker = FactoryPointTracker.klt(new int[]{1,2,4,8},confDetector,3,
                imageClass,derivClass);

        // This estimates the 2D image motion
        // An Affine2D_F64 model also works quite well.
        ImageMotion2D<T,Homography2D_F64> motion2D =
                FactoryMotion2D.createMotion2D(200,3,2,30,0.6,0.5,false,tracker,new Homography2D_F64());

        // wrap it so it output color images while estimating motion from gray
        ImageMotion2D motion;
        if( imageType.getFamily() == ImageType.Family.PLANAR ) {
            motion = new PlToGrayMotion2D(motion2D, imageClass);
        } else {
            motion = motion2D;
        }
        // This fuses the images together
        StitchingFromMotion2D<T,Homography2D_F64>
                stabilize = FactoryMotion2D.createVideoStitch(0.5, motion, imageType);

        return new Tuple2<>("KLT:Homography",stabilize);
    }
}
