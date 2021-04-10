package validation;

import boofcv.abst.scene.ConfigFeatureToSceneRecognition;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.io.UtilIO;

import java.io.File;

/**
 * Generate configs to test. Easier than manually creating them
 *
 * @author Peter Abeles
 */
public class GenerateConfigs {
    public static ConfigFeatureToSceneRecognition createSift() {
        ConfigFeatureToSceneRecognition config = new ConfigFeatureToSceneRecognition();

        config.features.typeDescribe = ConfigDescribeRegion.Type.SIFT;
        config.features.typeDetector = ConfigDetectInterestPoint.Type.SIFT;

        return config;
    }

    public static ConfigFeatureToSceneRecognition createSurf() {
        ConfigFeatureToSceneRecognition config = new ConfigFeatureToSceneRecognition();

        config.features.typeDescribe = ConfigDescribeRegion.Type.SURF_STABLE;
        config.features.typeDetector = ConfigDetectInterestPoint.Type.FAST_HESSIAN;

        return config;
    }

    public static ConfigFeatureToSceneRecognition createBrief() {
        ConfigFeatureToSceneRecognition config = new ConfigFeatureToSceneRecognition();

        config.features.typeDescribe = ConfigDescribeRegion.Type.BRIEF;
        config.features.typeDetector = ConfigDetectInterestPoint.Type.FAST_HESSIAN;

        return config;
    }

    public static void main(String[] args) {
        ConfigFeatureToSceneRecognition canonical = new ConfigFeatureToSceneRecognition();

        UtilIO.saveConfig(createSift(), canonical, new File("config_sift.yaml"));
        UtilIO.saveConfig(createSurf(), canonical, new File("config_surf.yaml"));
        UtilIO.saveConfig(createBrief(), canonical, new File("config_brief.yaml"));
    }
}
