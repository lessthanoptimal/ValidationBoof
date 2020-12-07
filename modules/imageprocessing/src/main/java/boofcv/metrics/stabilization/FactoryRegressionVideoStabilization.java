package boofcv.metrics.stabilization;

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.PlToGrayMotion2D;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.struct.Tuple2;

/**
 * @author Peter Abeles
 */
public class FactoryRegressionVideoStabilization {
    public static <T extends ImageBase<T>>
    Tuple2<String,StitchingFromMotion2D<T,?>> createKlt(ImageType<T> imageType )
    {
        // Configure the feature detector
        ConfigPointDetector configDet = new ConfigPointDetector();
        configDet.general.threshold = 10;
        configDet.general.radius = 2;

        ConfigPKlt configKlt = new ConfigPKlt();
        configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);
        configKlt.templateRadius = 3;
        configKlt.maximumTracks.setFixed(300);

        Class imageClass = imageType.getImageClass();

        // Use a KLT tracker
        PointTracker<T> tracker = FactoryPointTracker.klt(configKlt,configDet,imageClass,null);

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

        return new Tuple2<>("KLT_Homography",stabilize);
    }
}
