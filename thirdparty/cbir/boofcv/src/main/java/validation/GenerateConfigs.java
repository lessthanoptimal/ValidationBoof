package validation;

import boofcv.abst.scene.nister2006.ConfigImageRecognitionNister2006;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.io.UtilIO;

import java.io.File;

/**
 * Generate configs to test. Easier than manually creating them
 *
 * @author Peter Abeles
 */
public class GenerateConfigs {
    public static ConfigImageRecognitionNister2006 createSift() {
        ConfigImageRecognitionNister2006 config = new ConfigImageRecognitionNister2006();

        config.features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SIFT;
        config.features.typeDetector = ConfigDetectInterestPoint.DetectorType.SIFT;
        config.features.detectSift.maxFeaturesAll = 500;
        config.features.detectSift.extract.threshold = 0.0f;
        config.features.detectSift.extract.radius = 2;

        return config;
    }

    public static ConfigImageRecognitionNister2006 createSurf() {
        ConfigImageRecognitionNister2006 config = new ConfigImageRecognitionNister2006();

        config.features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
        config.features.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
        config.features.detectFastHessian.maxFeaturesAll = 500;
        config.features.detectFastHessian.extract.threshold = 0.0f;
        config.features.detectFastHessian.extract.radius = 2;

        return config;
    }

    public static ConfigImageRecognitionNister2006 createBrief() {
        ConfigImageRecognitionNister2006 config = new ConfigImageRecognitionNister2006();

        config.features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.BRIEF;
        config.features.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
        config.features.detectFastHessian.maxFeaturesAll = 500;
        config.features.detectFastHessian.extract.threshold = 0.0f;
        config.features.detectFastHessian.extract.radius = 2;

        return config;
    }

    public static void main(String[] args) {
        ConfigImageRecognitionNister2006 canonical = new ConfigImageRecognitionNister2006();

        UtilIO.saveConfig(createSift(), canonical, new File("config_sift.yaml"));
        UtilIO.saveConfig(createSurf(), canonical, new File("config_surf.yaml"));
        UtilIO.saveConfig(createBrief(), canonical, new File("config_brief.yaml"));
    }
}
