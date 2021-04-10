package boofcv.regression;

import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.common.*;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.sfm.ConfigStereoDualTrackPnP;
import boofcv.factory.sfm.ConfigStereoQuadPnP;
import boofcv.factory.sfm.ConfigVisOdomTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.factory.transform.census.CensusVariants;
import boofcv.metrics.vo.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class StereoVisualOdometryRegression extends BaseRegression implements ImageRegression {

	RuntimeSummary runtime;
	DogArray_F64 summaryRuntimeMS = new DogArray_F64();

	PrintStream out;

	public StereoVisualOdometryRegression() {
		super(BoofRegressionConstants.TYPE_GEOMETRY);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		List<Info> all = new ArrayList<>();

		Class bandType = ImageDataType.typeToSingleClass(type);

		all.add( createMonoDepth(bandType));
		all.add( createDualTrackerPnP(bandType));
		all.add( createQuadPnP(bandType));

		runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime,getClass(),"RUN_StereoVisOdom.txt");

		for( Info a : all ) {
			summaryRuntimeMS.reset();
			runtime.out.println(a.name);
			runtime.printUnitsRow(false);

			out = new PrintStream(new File(directoryMetrics,"ACC_StereoVisOdom_"+a.name+".txt"));
			BoofRegressionConstants.printGenerator(out, getClass());
			out.println("# Evaluating: "+a.name);

			try {
				SequenceStereoImages data = new WrapParseLeuven07(new ParseLeuven07("data/leuven07"));
				evaluate(a,data,"Leuven07");
				for( int i = 0; i < 1; i++ ) { // can do up to 11
					String sequence = String.format("%02d",i);
					data = new WrapParseKITTI("data/KITTI",sequence);
					evaluate(a,data,"KITTI"+sequence);
				}
			} catch( RuntimeException e ) {
				errorLog.println("FAILED to process "+a.name);
				e.printStackTrace(errorLog);
				errorLog.println("---------------------------------------------------");
			}
			runtime.out.println();
			runtime.saveSummary(a.name,summaryRuntimeMS);

			out.close();
		}

		runtime.out.println();
		runtime.printSummaryResults();

		runtime.out.close();
	}

	private void evaluate( Info vo , SequenceStereoImages data , String dataName ) {

		out.println("\n###################################################################################");
		out.println("# Dataset: "+dataName);
		out.println();

		try {
			EvaluateVisualOdometryStereo evaluator = new EvaluateVisualOdometryStereo(data,vo.vo,vo.imageType);

			evaluator.setOutputStream(out);
			evaluator.initialize();
			while( evaluator.nextFrame() ){}
			summaryRuntimeMS.addAll(evaluator.processingTimeMS);
			runtime.printStatsRow(dataName,evaluator.processingTimeMS);
		} catch( RuntimeException e ) {
			errorLog.println("FAILED "+vo.name+" on "+dataName);
			e.printStackTrace(errorLog);
			errorLog.println("---------------------------------------------------");
		}
		out.flush();
	}

	public static Info createMonoDepth(Class bandType ) {
		Class derivType = GImageDerivativeOps.getDerivativeType(bandType);

		ConfigDisparityBM configDisparity = new ConfigDisparityBM();
		configDisparity.errorType = DisparityError.CENSUS;
		configDisparity.configCensus.variant = CensusVariants.BLOCK_5_5;
		configDisparity.disparityMin = 0;
		configDisparity.disparityRange = 50;
		configDisparity.maxPerPixelError = 30;
		configDisparity.regionRadiusX = 3;
		configDisparity.regionRadiusY = 3;
		configDisparity.texture = 0.05;
		configDisparity.validateRtoL = 3;
		configDisparity.subpixel = true;

		ConfigPKlt configKlt = new ConfigPKlt();
		configKlt.pyramidLevels = ConfigDiscreteLevels.minSize(40);
		configKlt.templateRadius = 4;
		configKlt.pruneClose = true;
		configKlt.config.maxIterations = 25;
		configKlt.config.maxPerPixelError = 25;
		configKlt.toleranceFB = 3;
		configKlt.maximumTracks.setFixed(300);

		ConfigPointDetector configDet = new ConfigPointDetector();
		configDet.type = PointDetectorTypes.SHI_TOMASI;
		configDet.shiTomasi.radius = 6;
		configDet.general.radius = 5;
		configDet.general.threshold = 1f;
		configDet.general.selector = ConfigSelectLimit.selectBestN();

		ConfigVisOdomTrackPnP configVO = new ConfigVisOdomTrackPnP();
		configVO.ransac.iterations = 500;
		configVO.ransac.inlierThreshold = 0.5;
		configVO.dropOutlierTracks = 2;
		configVO.maxKeyFrames = 5;
		configVO.bundleConverge.maxIterations = 1;
		configVO.bundleMaxFeaturesPerFrame = 200;
		configVO.bundleMinObservations = 3;
		configVO.keyframes.geoMinCoverage = 0.4;

		StereoDisparitySparse<GrayF32> disparity = FactoryStereoDisparity.sparseRectifiedBM(configDisparity, bandType);
		PointTracker tracker = FactoryPointTracker.klt(configKlt, configDet,bandType, derivType);
		StereoVisualOdometry visodom = FactoryVisualOdometry.stereoMonoPnP(configVO,disparity,tracker,bandType);

		Info ret = new Info();
		ret.name = "StereoDepth";
		ret.imageType = ImageType.single(bandType);
		ret.vo = visodom;

		return ret;
	}

	public static Info createDualTrackerPnP( Class bandType ) {

		ConfigStereoDualTrackPnP config = new ConfigStereoDualTrackPnP();
		config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		config.tracker.klt.pyramidLevels = ConfigDiscreteLevels.minSize(40);
		config.tracker.klt.templateRadius = 4; // 3 = 17.2 fps
		config.tracker.klt.pruneClose = true;
		config.tracker.klt.config.maxIterations = 25;
		config.tracker.klt.config.maxPerPixelError = 25;
		config.tracker.klt.toleranceFB = 3;
		config.tracker.klt.maximumTracks.setFixed(400);

		// for stereo associations
		config.tracker.detDesc.typeDescribe = ConfigDescribeRegion.Type.BRIEF;
		config.tracker.detDesc.describeBrief.fixed = true;

		config.tracker.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
		config.tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.tracker.detDesc.detectPoint.scaleRadius = 11.0;
		config.tracker.detDesc.detectPoint.shiTomasi.radius = 4;
		config.tracker.detDesc.detectPoint.general.radius = 4;
		config.tracker.detDesc.detectPoint.general.threshold = 1;

		config.epipolarTol = 1.5;

		config.scene.ransac.iterations = 400;
		config.scene.ransac.inlierThreshold = 0.5;

		config.scene.refineIterations = 25;
		config.scene.bundleConverge.maxIterations = 1;
		config.scene.bundleMaxFeaturesPerFrame = 200;
		config.scene.bundleMinObservations = 3;
		config.scene.keyframes.geoMinCoverage = 0.3;


		Info ret = new Info();
		ret.name = "DualPnP";
		ret.imageType = ImageType.single(bandType);
		ret.vo = FactoryVisualOdometry.stereoDualTrackerPnP(config,bandType);

		return ret;
	}

	public static Info createQuadPnP( Class bandType ) {
		ConfigStereoQuadPnP config = new ConfigStereoQuadPnP();

//		config.detectDescribe.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
//		config.detectDescribe.detectFastHessian.extract.radius = 3;
//		config.detectDescribe.detectFastHessian.maxFeaturesPerScale = 400;
//		config.detectDescribe.detectFastHessian.numberScalesPerOctave = 3;
//		config.detectDescribe.detectFastHessian.numberOfOctaves = 3;
//		config.detectDescribe.detectFastHessian.maxFeaturesPerScale = 400;
//		config.detectDescribe.detectFastHessian.numberScalesPerOctave = 3;
//		config.detectDescribe.detectFastHessian.numberOfOctaves = 3;

		// TODO How is it possible that it used SURF before? Just describing SURF features slows it down too much

		config.ransac.iterations = 400;
		config.ransac.inlierThreshold = 1.5;
		config.refineIterations = 50;
		config.bundleConverge.maxIterations = 0; // yes turning it off made it slightly more accurate

		config.detectDescribe.typeDetector = ConfigDetectInterestPoint.Type.POINT;
		config.detectDescribe.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.detectDescribe.detectPoint.scaleRadius = 11;
		config.detectDescribe.detectPoint.shiTomasi.radius = 2;
		config.detectDescribe.detectPoint.general.radius = 2;
		config.detectDescribe.detectPoint.general.maxFeatures = 600;
		config.detectDescribe.detectPoint.general.threshold = 50;

		config.detectDescribe.typeDescribe = ConfigDescribeRegion.Type.BRIEF;
		config.detectDescribe.describeBrief.fixed = true;
//		config.detectDescribe.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_FAST;

		config.associateF2F.greedy.forwardsBackwards = false;
		config.associateF2F.greedy.scoreRatioThreshold = 1.0;
		config.associateF2F.maximumDistancePixels.setRelative(0.15,0);

		config.epipolarTol = 0.5;

		Info ret = new Info();
		ret.name = "QuadPnP";
		ret.imageType = ImageType.single(bandType);
		ret.vo = FactoryVisualOdometry.stereoQuadPnP(config, bandType);

		return ret;
	}

	public static class Info {
		public String name;
		public ImageType imageType;
		public StereoVisualOdometry vo;
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{StereoVisualOdometryRegression.class.getName(),ImageDataType.F32.toString()});
//		RegressionRunner.main(new String[]{StereoVisualOdometryRegression.class.getName(),ImageDataType.U8.toString()});
	}

}
