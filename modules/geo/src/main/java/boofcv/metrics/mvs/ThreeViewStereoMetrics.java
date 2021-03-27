package boofcv.metrics.mvs;

import boofcv.abst.disparity.StereoDisparity;
import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.sfm.structure.ThreeViewEstimateMetricScene;
import boofcv.core.image.ConvertImage;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.transform.census.CensusVariants;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import java.awt.image.BufferedImage;

/**
 * Evaluates uncalibrated three view reconstruction using stereo disparity. Structure of each scene is unknown.
 * Observations indicate that stereo disparity is a reasonable performance metric. if the sparse reconstruction
 * is poor the rectification will be poor too. Making stereo disparity calculation fail. A disparity is only accepted
 * if the region has sufficient texture and has a low enough error. There for, only regions which can yield
 * results are considered and the matches must be good.
 *
 * @author Peter Abeles
 */
public class ThreeViewStereoMetrics {
    GrayU8 image01,image02,image03;
    double cx,cy;

    DogArray<AssociatedTriple> associated = new DogArray<>(AssociatedTriple::new);

    double score;
    double areaFraction;
    // process time after loading the images and just after finishing
    long time0,time1;

    public boolean process( String path , String suffix ) {
        findTripplets(path,suffix);

        score = 0;
        int width = image01.width;
        int height = image01.height;

        // Run the reconstruction algorithm. This is the heart of what we are testing
        ThreeViewEstimateMetricScene alg = new ThreeViewEstimateMetricScene();

        alg.configRansac.iterations = 3000;
        alg.convergeSBA.maxIterations = 40;

        if( !alg.process(associated.toList(),width,height) )
            return false;

        // Stereo computation. This is where we evaluate the performance
        computeDisparityScore(width, height, alg.getStructure());

        return true;
    }

    private void computeDisparityScore(int width, int height, SceneStructureMetric structure) {
        BundlePinholeSimplified cp = structure.getCameras().data[0].getModel();
        CameraPinholeBrown intrinsic01 = new CameraPinholeBrown();
        intrinsic01.fsetK(cp.f,cp.f,0,cx,cy,width,height);
        intrinsic01.fsetRadial(cp.k1,cp.k2);

        cp = structure.getCameras().data[1].getModel();
        CameraPinholeBrown intrinsic02 = new CameraPinholeBrown();
        intrinsic02.fsetK(cp.f,cp.f,0,cx,cy,width,height);
        intrinsic02.fsetRadial(cp.k1,cp.k2);

        Se3_F64 leftToRight = structure.getParentToView(1);

        int max = 200;

        GrayU8 rectMask = new GrayU8(image01.width, image01.height);
        GrayF32 disparity = computeStereoDisparity(image01,image02,intrinsic01,intrinsic02,rectMask,leftToRight,0,max);

        time1 = System.currentTimeMillis();

        // count number of pixels with a disparity more than zero and less than the max
        int total = 0;
        for (int i = 0; i < disparity.height; i++) {
            int idx = disparity.startIndex + i*disparity.stride;
            for (int j = 0; j < disparity.width; j++) {
                float v = disparity.data[idx++];
                if( v >= 1 && v <= max ) {
                    total++;
                }
            }
        }

        // pixels are marked in 1 if the source image was reachable
        int area = ImageStatistics.sum(rectMask)+1; // +1 is to avoid divide by zero

        score = total/(double)area;
        areaFraction = area / (double)(disparity.width*disparity.height);
    }

    private void findTripplets( String path , String suffix ) {
        BufferedImage buff01 = UtilImageIO.loadImage(path+"01."+suffix);
        BufferedImage buff02 = UtilImageIO.loadImage(path+"02."+suffix);
        BufferedImage buff03 = UtilImageIO.loadImage(path+"03."+suffix);

        Planar<GrayU8> color01 = ConvertBufferedImage.convertFrom(buff01,true, ImageType.pl(3,GrayU8.class));
        Planar<GrayU8> color02 = ConvertBufferedImage.convertFrom(buff02,true,ImageType.pl(3,GrayU8.class));
        Planar<GrayU8> color03 = ConvertBufferedImage.convertFrom(buff03,true,ImageType.pl(3,GrayU8.class));

        image01 = ConvertImage.average(color01,null);
        image02 = ConvertImage.average(color02,null);
        image03 = ConvertImage.average(color03,null);

        ConfigDetectDescribe configDetDesc = new ConfigDetectDescribe();
        configDetDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
        configDetDesc.detectFastHessian.extract.radius = 12;
        configDetDesc.detectFastHessian.maxFeaturesPerScale = 500;
//        configDetDesc.detectFastHessian.maxFeaturesAll = 1000;

        configDetDesc.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;

        DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc = FactoryDetectDescribe.generic(configDetDesc,GrayU8.class);

        DogArray<Point2D_F64> locations01 = new DogArray<>(Point2D_F64::new);
        DogArray<Point2D_F64> locations02 = new DogArray<>(Point2D_F64::new);
        DogArray<Point2D_F64> locations03 = new DogArray<>(Point2D_F64::new);

        DogArray<TupleDesc_F64> features01 = UtilFeature.createArray(detDesc,100);
        DogArray<TupleDesc_F64> features02 = UtilFeature.createArray(detDesc,100);
        DogArray<TupleDesc_F64> features03 = UtilFeature.createArray(detDesc,100);

        DogArray_I32 sets01 = new DogArray_I32();
        DogArray_I32 sets02 = new DogArray_I32();
        DogArray_I32 sets03 = new DogArray_I32();

        time0 = System.currentTimeMillis();

        int width = image01.width, height = image01.height;
//        System.out.println("Image Shape "+width+" x "+height);
        cx = width/2;
        cy = height/2;
//		double scale = Math.max(cx,cy);

        // COMMENT ON center point zero
        detDesc.detect(image01);
        copyResults(detDesc, locations01, features01, sets01);
        detDesc.detect(image02);
        copyResults(detDesc, locations02, features02, sets02);
        detDesc.detect(image03);
        copyResults(detDesc, locations03, features03, sets03);

        ConfigAssociate configAssociate = new ConfigAssociate();
        configAssociate.type = ConfigAssociate.AssociationType.GREEDY;
        configAssociate.greedy.scoreRatioThreshold = 0.95;
        configAssociate.greedy.forwardsBackwards = true;
        configAssociate.greedy.maxErrorThreshold = 0.1;

        AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.generic(configAssociate,detDesc);

        AssociateThreeByPairs<TupleDesc_F64> associateThree = new AssociateThreeByPairs<>(associate);

        associateThree.setFeaturesA(features01, sets01);
        associateThree.setFeaturesB(features02, sets02);
        associateThree.setFeaturesC(features03, sets03);

        associateThree.initialize(detDesc.getNumberOfSets());
        associateThree.associate();

        DogArray<AssociatedTripleIndex> associatedIdx = associateThree.getMatches();
//        System.out.println("Associated: "+associatedIdx.size);
        associated.reset();
        for (int i = 0; i < associatedIdx.size; i++) {
            AssociatedTripleIndex p = associatedIdx.get(i);
            associated.grow().setTo(locations01.get(p.a),locations02.get(p.b),locations03.get(p.c));
        }
    }

    private void copyResults(DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc,
                             DogArray<Point2D_F64> locations,
                             DogArray<TupleDesc_F64> features,
                             DogArray_I32 sets)
    {
        final int N = detDesc.getNumberOfFeatures();
        locations.resize(N);
        features.resize(N);
        sets.resize(N);
        for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
            Point2D_F64 pixel = detDesc.getLocation(i);
            locations.get(i).setTo(pixel.x - cx, pixel.y - cy);
            features.get(i).setTo(detDesc.getDescription(i));
            sets.data[i] = detDesc.getSet(i);
        }
    }

    public GrayF32 computeStereoDisparity( GrayU8 distortedLeft, GrayU8 distortedRight ,
                                           CameraPinholeBrown intrinsicLeft ,
                                           CameraPinholeBrown intrinsicRight ,
                                           GrayU8 rectMask,
                                           Se3_F64 leftToRight ,
                                           int minDisparity , int maxDisparity) {

//		drawInliers(origLeft, origRight, intrinsic, inliers);
        // Rectify and remove lens distortion for stereo processing
        DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
        DMatrixRMaj rectifiedR = new DMatrixRMaj(3, 3);

        // rectify a colored image
        GrayU8 rectifiedLeft = distortedLeft.createSameShape();
        GrayU8 rectifiedRight = distortedRight.createSameShape();
        rectifyImages(distortedLeft, distortedRight, leftToRight, intrinsicLeft, intrinsicRight,
                rectifiedLeft, rectifiedRight,rectMask, rectifiedK, rectifiedR);

        // compute disparity
        ConfigDisparityBMBest5 config5 = new ConfigDisparityBMBest5();
        config5.errorType = DisparityError.CENSUS;
        config5.configCensus.variant = CensusVariants.BLOCK_5_5;
        config5.regionRadiusX = config5.regionRadiusY = 6;
        config5.maxPerPixelError = 30;
        config5.validateRtoL = 3;
        config5.texture = 0.05;
        config5.subpixel = true;
        config5.disparityMin = minDisparity;
        config5.disparityRange = maxDisparity-minDisparity+1;
        StereoDisparity<GrayU8, GrayF32> disparityAlg =
                FactoryStereoDisparity.blockMatchBest5(config5,GrayU8.class,GrayF32.class);

        // process and return the results
        disparityAlg.process(rectifiedLeft, rectifiedRight);
        return disparityAlg.getDisparity();
    }

    public static <T extends ImageBase<T>>
    void rectifyImages(T distortedLeft,
                       T distortedRight,
                       Se3_F64 leftToRight,
                       CameraPinholeBrown intrinsicLeft,
                       CameraPinholeBrown intrinsicRight,
                       T rectifiedLeft,
                       T rectifiedRight,
                       GrayU8 rectMask,
                       DMatrixRMaj rectifiedK,
                       DMatrixRMaj rectifiedR) {
        RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

        // original camera calibration matrices
        DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(intrinsicLeft, (DMatrixRMaj)null);
        DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(intrinsicRight, (DMatrixRMaj)null);

        rectifyAlg.process(K1, new Se3_F64(), K2, leftToRight);

        // rectification matrix for each image
        DMatrixRMaj rect1 = rectifyAlg.getUndistToRectPixels1();
        DMatrixRMaj rect2 = rectifyAlg.getUndistToRectPixels2();
        rectifiedR.setTo(rectifyAlg.getRectifiedRotation());

        // New calibration matrix,
        rectifiedK.setTo(rectifyAlg.getCalibrationMatrix());

        // Adjust the rectification to make the view area more useful
        ImageDimension rectShape = new ImageDimension();
        RectifyImageOps.fullViewLeft(intrinsicLeft, rect1, rect2, rectifiedK,rectShape);

        // undistorted and rectify images
        FMatrixRMaj rect1_F32 = new FMatrixRMaj(3,3);
        FMatrixRMaj rect2_F32 = new FMatrixRMaj(3,3);
        ConvertMatrixData.convert(rect1, rect1_F32);
        ConvertMatrixData.convert(rect2, rect2_F32);

        ImageDistort<T,T> distortLeft =
                RectifyDistortImageOps.rectifyImage(intrinsicLeft, rect1_F32, BorderType.SKIP, distortedLeft.getImageType());
        ImageDistort<T,T> distortRight =
                RectifyDistortImageOps.rectifyImage(intrinsicRight, rect2_F32, BorderType.SKIP, distortedRight.getImageType());

        rectifiedLeft.reshape(rectShape.width,rectShape.height);
        rectifiedRight.reshape(rectShape.width,rectShape.height);
        distortLeft.apply(distortedLeft, rectifiedLeft,rectMask);
        distortRight.apply(distortedRight, rectifiedRight);
    }

    public double getScore() {
        return score;
    }

    public double getAreaFraction() {
        return areaFraction;
    }

    public long getElapsedTime() {
        return time1-time0;
    }
}
