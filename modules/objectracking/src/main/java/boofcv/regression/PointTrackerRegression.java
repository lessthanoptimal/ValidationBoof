package boofcv.regression;

import boofcv.abst.feature.describe.ConfigTemplateDescribe;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.feature.orientation.ConfigOrientation2;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.common.*;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.metrics.point.EvaluateTrackerStability;
import boofcv.metrics.point.EvaluationTracker;
import boofcv.metrics.point.LogParseHomography;
import boofcv.metrics.point.WrapPointTracker;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.struct.GrowQueue_F64;
import org.ejml.UtilEjml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PointTrackerRegression extends BaseRegression implements ImageRegression {

	public static String pathToData = ValidationConstants.PATH_DATA+"abeles2012/";
	public static double tolerance = 5;
	String[] dataDirectories = new String[]{"bricks","carpet"};
	String[] dataSets = new String[]{"skew","rotate","move_out","move_in"};
	String[] variousSets = new String[]{"lighting","compressed","urban"};
	int[] skips = new int[]{1,4,8};

	RuntimeSummary outputSpeed;
	GrowQueue_F64 runtimeSummary = new GrowQueue_F64();

	// summary for a tracker
	double summaryMeanF,summaryMeanFA,summaryMeanPrecision,summaryMeanRecall,summaryMeanTracks,summaryImageArea;

	public PointTrackerRegression() {
		super(BoofRegressionConstants.TYPE_TRACKING);
	}

	@Override
	public void process(ImageDataType type) throws IOException {

		// A single file which summarizes runtime for all trackers
		outputSpeed = new RuntimeSummary();
		outputSpeed.initializeLog(directoryRuntime, getClass(),"RUN_PointTracker.txt");
		outputSpeed.printUnitsRow(true);

		List<Info> all = new ArrayList<>();
		Class bandType = ImageDataType.typeToSingleClass(type);

		all.add( createDefaultKlt(bandType));
		all.add( createHarrisNCC(bandType));
		all.add( createShiNCC(bandType));
		all.add( createFastNCC(bandType));
		all.add( createFhBrief(bandType));
		all.add( createFhKltSurf(bandType));

		for( Info info : all ) {
			try {
				process(info,bandType);
			} catch( RuntimeException e ) {
				errorLog.println("Tracker "+info.name);
				e.printStackTrace(errorLog);
				errorLog.println("----------------------------");
			}
		}

		outputSpeed.out.close();
	}

	protected void process( Info info , Class bandType ) throws FileNotFoundException {
		// reset summary statistics
		summaryMeanF=summaryMeanFA=summaryMeanPrecision=summaryMeanRecall=summaryMeanTracks=summaryImageArea=0;
		runtimeSummary.reset();

		PrintStream outSummary = new PrintStream(new File(directoryMetrics,"ACC_PointTracker_"+info.name+".txt"));
		BoofRegressionConstants.printGenerator(outSummary, getClass());
		outSummary.println("# Inlier Tolerance " + tolerance + "  Algorithm " + info.name);
		outSummary.println();
		outSummary.println("# (Data Set) (Skip) (F) (F all inside) (Precision) (Recall) (Recall all inside) (Tracks) (Image Area)");

		int totalTrials = 0;
		for( String directory : dataDirectories ) {
			for( String whichData : dataSets ) {
				for( int skip : skips )  {
					totalTrials++;
					String path = pathToData+directory+"/"+whichData;

					WrapPointTracker wrapped = new WrapPointTracker(info.tracker);
					computeResults(bandType, wrapped, path, skip, outSummary);
				}
			}
		}

		for( String whichData : variousSets ) {
			for( int skip : skips )  {
				totalTrials++;
				String path = pathToData+"various/"+whichData;

				WrapPointTracker wrapped = new WrapPointTracker(info.tracker);
				computeResults(bandType, wrapped, path, skip, outSummary);
			}
		}

		outputSpeed.printStatsRow(info.name,runtimeSummary);

		outSummary.println();
		summaryMeanF /= totalTrials;
		summaryMeanFA /= totalTrials;
		summaryMeanPrecision /= totalTrials;
		summaryMeanRecall /= totalTrials;
		summaryMeanTracks /= totalTrials;
		summaryImageArea /= totalTrials;
		outSummary.printf("SUMMARY: F=%6.3f FA=%6.3f PR=%6.3f RE=%6.3f MT=%6.1f AREA=%3d\n",
				summaryMeanF,summaryMeanFA,summaryMeanPrecision,summaryMeanRecall,summaryMeanTracks,(int)summaryImageArea);


		outSummary.close();
	}

	private void computeResults(Class imageType,
								EvaluationTracker tracker ,
								String pathData, int skip,
								PrintStream outSummary)
			throws FileNotFoundException
	{
		SimpleImageSequence sequence =
				DefaultMediaManager.INSTANCE.openVideo(pathData + "_undistorted.mjpeg", ImageType.single(imageType));

		// Remove redundant directory to make file name more compact
		String dataName = Paths.get("data/abeles2012").relativize(Paths.get(pathData)).toString();

		List<Homography2D_F64> groundTruth = LogParseHomography.parse(pathData + "_homography.txt");

		EvaluateTrackerStability app = new EvaluateTrackerStability(tolerance,skip);

		app.evaluate(tracker,sequence,groundTruth,null);

		outSummary.printf("%-24s %2d %6.3f %6.3f %6.3f %6.3f %6.3f %6.1f %3d\n", dataName, skip,
				app.getMeanF(), app.getMeanFA(), app.getMeanPrecision(), app.getMeanRecall(), app.getMeanRecallA(),
				app.getMeanTrackCount(),(int)app.getMeanImageArea());

		runtimeSummary.addAll(app.elapsedTimeMS);
		summaryMeanF = addOnlyIfCountable(summaryMeanF,app.getMeanF());
		summaryMeanFA = addOnlyIfCountable(summaryMeanFA,app.getMeanFA());
		summaryMeanPrecision = addOnlyIfCountable(summaryMeanPrecision,app.getMeanPrecision());
		summaryMeanRecall = addOnlyIfCountable(summaryMeanRecall,app.getMeanRecall());
		summaryMeanTracks = addOnlyIfCountable(summaryMeanTracks,app.getMeanTrackCount());
		summaryImageArea = addOnlyIfCountable(summaryImageArea,app.getMeanImageArea());
	}

	public static double addOnlyIfCountable( double value , double additive ) {
		if( UtilEjml.isUncountable(additive))
			return value;
		return value+additive;
	}

	public Info createFhKltSurf( Class bandType ) {

		ConfigPointTracker config = new ConfigPointTracker();
		config.typeTracker = ConfigPointTracker.TrackerType.HYBRID;
		config.hybrid.reactivateThreshold = 200;
		config.detDesc.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
		config.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
		config.detDesc.detectFastHessian.maxFeaturesPerScale = 200;
		config.detDesc.detectFastHessian.extract.radius = 2;
		config.detDesc.detectFastHessian.extract.threshold = 1;
		config.detDesc.orientation.type = ConfigOrientation2.Type.SLIDING;

		Info info = new Info();
		info.name = "FhKltSurf";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.tracker(config,bandType,null);

		return info;
	}

	public Info createFhBrief( Class bandType ) {

		ConfigPointTracker config = new ConfigPointTracker();
		config.typeTracker = ConfigPointTracker.TrackerType.DDA;
		config.detDesc.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.BRIEF;
		config.detDesc.describeBrief.fixed = false;
		config.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
		config.detDesc.detectFastHessian.maxFeaturesPerScale = 200;
		config.detDesc.orientation.type = ConfigOrientation2.Type.AVERAGE;

		Info info = new Info();
		info.name = "FhBrief";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.tracker(config,bandType,null);

		return info;
	}

	public Info createHarrisNCC( Class bandType ) {
		ConfigPointTracker config = new ConfigPointTracker();
		config.typeTracker = ConfigPointTracker.TrackerType.DDA;
		config.detDesc.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.TEMPLATE;
		config.detDesc.describeTemplate.type = ConfigTemplateDescribe.Type.NCC;
		config.detDesc.describeTemplate.width = 7;
		config.detDesc.describeTemplate.height = 7;
		config.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		config.detDesc.detectPoint.type = PointDetectorTypes.HARRIS;
		config.detDesc.detectPoint.harris.kappa = 0.04;
		config.detDesc.detectPoint.harris.radius = 3;
		config.detDesc.detectPoint.scaleRadius = 2.0;
		config.detDesc.detectPoint.general.radius = 10;
		config.detDesc.detectPoint.general.threshold = 0.01f;
		config.detDesc.detectPoint.general.maxFeatures = 600;

		Info info = new Info();
		info.name = "HarrisNCC";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.tracker(config,bandType,null);

		return info;
	}

	public Info createShiNCC( Class bandType ) {

		ConfigPointTracker config = new ConfigPointTracker();
		config.typeTracker = ConfigPointTracker.TrackerType.DDA;
		config.detDesc.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.TEMPLATE;
		config.detDesc.describeTemplate.type = ConfigTemplateDescribe.Type.NCC;
		config.detDesc.describeTemplate.width = 7;
		config.detDesc.describeTemplate.height = 7;
		config.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		config.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.detDesc.detectPoint.shiTomasi.radius = 3;
		config.detDesc.detectPoint.scaleRadius = 2.0;
		config.detDesc.detectPoint.general.radius = 10;
		config.detDesc.detectPoint.general.threshold = 0.01f;
		config.detDesc.detectPoint.general.maxFeatures = 600;

		Info info = new Info();
		info.name = "ShiTomasiNCC";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.tracker(config,bandType,null);

		return info;
	}

	public Info createFastNCC( Class bandType ) {

		ConfigPointTracker config = new ConfigPointTracker();
		config.typeTracker = ConfigPointTracker.TrackerType.DDA;
		config.detDesc.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.TEMPLATE;
		config.detDesc.describeTemplate.type = ConfigTemplateDescribe.Type.NCC;
		config.detDesc.describeTemplate.width = 7;
		config.detDesc.describeTemplate.height = 7;
		config.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		config.detDesc.detectPoint.type = PointDetectorTypes.FAST;
		config.detDesc.detectPoint.fast.pixelTol = 6;
		config.detDesc.detectPoint.fast.minContinuous = 9;
		config.detDesc.detectPoint.scaleRadius = 2.0;
		config.detDesc.detectPoint.general.radius = 10;
		config.detDesc.detectPoint.general.threshold = 6f;
		config.detDesc.detectPoint.general.maxFeatures = 300;

		Info info = new Info();
		info.name = "FastNCC";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.tracker(config,bandType,null);

		return info;
	}

	public Info createDefaultKlt(Class bandType) {

		ConfigPKlt configKlt = new ConfigPKlt();
//		configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4); <-- made it much worse!

		ConfigPointDetector configDet = new ConfigPointDetector();
		configDet.general.maxFeatures = 800;
		configDet.general.radius = 8;
		configDet.general.threshold = 1;

		Info info = new Info();
		info.name = "DefaultKLT";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.klt(configKlt, configDet,bandType,null);

		return info;
	}

	@SuppressWarnings("rawtypes")
	public static class Info {
		public String name;
		public ImageType imageType;
		public PointTracker tracker;
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{PointTrackerRegression.class.getName(),ImageDataType.F32.toString()});
		RegressionRunner.main(new String[]{PointTrackerRegression.class.getName(),ImageDataType.U8.toString()});
	}
}
