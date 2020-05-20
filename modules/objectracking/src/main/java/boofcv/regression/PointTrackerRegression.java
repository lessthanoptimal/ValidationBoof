package boofcv.regression;

import boofcv.abst.feature.associate.AssociateDescTo2D;
import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.abst.feature.orientation.OrientationIntegralToImage;
import boofcv.abst.tracker.ConfigTrackerDda;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.detect.selector.FeatureSelectLimit;
import boofcv.alg.feature.detect.selector.FeatureSelectNBest;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.common.*;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.metrics.point.EvaluateTrackerStability;
import boofcv.metrics.point.EvaluationTracker;
import boofcv.metrics.point.LogParseHomography;
import boofcv.metrics.point.WrapPointTracker;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.struct.GrowQueue_F64;
import org.ejml.UtilEjml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
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
	double summaryMeanF,summaryMeanFA,summaryMeanPrecision,summaryMeanRecall,summaryMeanTracks;

	public PointTrackerRegression() {
		super(BoofRegressionConstants.TYPE_TRACKING);
	}

	@Override
	public void process(ImageDataType type) throws IOException {

		// A single file which summarizes runtime for all trackers
		outputSpeed = new RuntimeSummary();
		outputSpeed.out = new PrintStream(new File(directoryMetrics, "RUN_PointTracker.txt"));
		BoofRegressionConstants.printGenerator(outputSpeed.out, getClass());
		outputSpeed.out.println("# Processing time statics across entire test set for each tracker");
		outputSpeed.out.println("# All times are in milliseconds");
		outputSpeed.printHeader(true);

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
		summaryMeanF=summaryMeanFA=summaryMeanPrecision=summaryMeanRecall=summaryMeanTracks=0;
		runtimeSummary.reset();

		PrintStream outSummary = new PrintStream(new FileOutputStream(new File(directoryMetrics,"ACC_PointTracker_"+info.name+".txt")));
		BoofRegressionConstants.printGenerator(outSummary, getClass());
		outSummary.println("# Inlier Tolerance " + tolerance + "  Algorithm " + info.name);
		outSummary.println("# (File) (Skip) (F) (F all inside) (Precision) (Recall) (Recall all inside) (Tracks)");

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

		outputSpeed.printStats(info.name,runtimeSummary);

		outSummary.println();
		summaryMeanF /= totalTrials;
		summaryMeanFA /= totalTrials;
		summaryMeanPrecision /= totalTrials;
		summaryMeanRecall /= totalTrials;
		summaryMeanTracks /= totalTrials;
		outSummary.printf("SUMMARY: F=%6.3f FA=%6.3f PR=%6.3f RE=%6.3f MT=%6.1f\n",
				summaryMeanF,summaryMeanFA,summaryMeanPrecision,summaryMeanRecall,summaryMeanTracks);


		outSummary.close();
	}

	private void computeResults(Class imageType,
								EvaluationTracker tracker ,
								String whichData, int skip,
								PrintStream outSummary)
			throws FileNotFoundException
	{
		SimpleImageSequence sequence =
				DefaultMediaManager.INSTANCE.openVideo(whichData + "_undistorted.mjpeg", ImageType.single(imageType));

		List<Homography2D_F64> groundTruth = LogParseHomography.parse(whichData + "_homography.txt");

		EvaluateTrackerStability app = new EvaluateTrackerStability(tolerance,skip);

		app.evaluate(tracker,sequence,groundTruth,null);

		outSummary.printf("%s %2d %6.3f %6.3f %6.3f %6.3f %6.3f %6.1f\n", whichData, skip,
				app.getMeanF(), app.getMeanFA(), app.getMeanPrecision(), app.getMeanRecall(), app.getMeanRecallA(),
				app.getMeanTrackCount());

		runtimeSummary.addAll(app.elapsedTimeMS);
		summaryMeanF = addOnlyIfCountable(summaryMeanF,app.getMeanF());
		summaryMeanFA = addOnlyIfCountable(summaryMeanFA,app.getMeanFA());
		summaryMeanPrecision = addOnlyIfCountable(summaryMeanPrecision,app.getMeanPrecision());
		summaryMeanRecall = addOnlyIfCountable(summaryMeanRecall,app.getMeanRecall());
		summaryMeanTracks = addOnlyIfCountable(summaryMeanTracks,app.getMeanTrackCount());
	}

	public static double addOnlyIfCountable( double value , double additive ) {
		if( UtilEjml.isUncountable(additive))
			return value;
		return value+additive;
	}

	public Info createFhKltSurf( Class bandType ) {

		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.maxFeaturesPerScale = 200;
		configFH.extract.radius = 2;
		configFH.extract.threshold = 1;

		Info info = new Info();
		info.name = "FhKltSurf";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.combined_FH_SURF_KLT(null,200,configFH,null,null,bandType);

		return info;
	}

	public Info createFhBrief( Class bandType ) {
		Class iiType = GIntegralImageOps.getIntegralType(bandType);

		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.maxFeaturesPerScale = 200;

		InterestPointDetector detector = FactoryInterestPoint.fastHessian(configFH,bandType);
		OrientationIntegral ori = FactoryOrientationAlgs.average_ii(null,iiType );
		OrientationImage orientation = new OrientationIntegralToImage(ori,bandType,iiType);

		DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(new ConfigBrief(false),bandType);
		ScoreAssociation scorer = FactoryAssociation.defaultScore(describe.getDescriptionType());
		AssociateDescription associate = DefaultConfigs.associateGreedy(scorer);
		AssociateDescription2D associate2D = new AssociateDescTo2D(associate);

		Info info = new Info();
		info.name = "FhBrief";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.dda(detector,orientation, describe , associate2D, new ConfigTrackerDda());

		return info;
	}

	public Info createHarrisNCC( Class bandType ) {
		Class derivType = GImageDerivativeOps.getDerivativeType(bandType);

		GeneralFeatureIntensity intensity = FactoryIntensityPoint.harris(3, 0.04f, false, derivType);
		ConfigExtract configExtract = new ConfigExtract(10,0.01f);
		configExtract.detectMaximums = intensity.localMaximums();
		configExtract.detectMinimums = intensity.localMinimums();
		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(configExtract);
		FeatureSelectLimit select = new FeatureSelectNBest();
		GeneralFeatureDetector detector = FactoryFeatureExtractor.general(intensity,nonmax,select,600);

		DescribeRegionPoint describe = FactoryDescribeRegionPoint.pixelNCC(7,7,bandType);
		ScoreAssociation scorer = FactoryAssociation.defaultScore(describe.getDescriptionType());
		AssociateDescription associate = DefaultConfigs.associateGreedy(scorer);
		AssociateDescription2D associate2D = new AssociateDescTo2D(associate);

		Info info = new Info();
		info.name = "HarrisNCC";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.dda(detector, describe , associate2D, 2,bandType);

		return info;
	}

	public Info createShiNCC( Class bandType ) {
		Class derivType = GImageDerivativeOps.getDerivativeType(bandType);

		GeneralFeatureIntensity intensity = FactoryIntensityPoint.shiTomasi(3, false, derivType);
		ConfigExtract configExtract = new ConfigExtract(10,0.01f);
		configExtract.detectMaximums = intensity.localMaximums();
		configExtract.detectMinimums = intensity.localMinimums();
		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(configExtract);
		FeatureSelectLimit select = new FeatureSelectNBest();
		GeneralFeatureDetector detector = FactoryFeatureExtractor.general(intensity,nonmax,select,600);

		DescribeRegionPoint describe = FactoryDescribeRegionPoint.pixelNCC(7,7,bandType);
		ScoreAssociation scorer = FactoryAssociation.defaultScore(describe.getDescriptionType());
		AssociateDescription associate = DefaultConfigs.associateGreedy(scorer);
		AssociateDescription2D associate2D = new AssociateDescTo2D(associate);

		Info info = new Info();
		info.name = "ShiTomasiNCC";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.dda(detector, describe , associate2D, 2,bandType);

		return info;
	}

	public Info createFastNCC( Class bandType ) {

		GeneralFeatureIntensity intensity = FactoryIntensityPoint.fast(6, 9, bandType);
		ConfigExtract configExtract = new ConfigExtract(10,6);
		configExtract.detectMaximums = intensity.localMaximums();
		configExtract.detectMinimums = intensity.localMinimums();
		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(configExtract);
		FeatureSelectLimit select = new FeatureSelectNBest();
		GeneralFeatureDetector detector = FactoryFeatureExtractor.general(intensity, nonmax, select,300);

		DescribeRegionPoint describe = FactoryDescribeRegionPoint.pixelNCC(7,7,bandType);
		ScoreAssociation scorer = FactoryAssociation.defaultScore(describe.getDescriptionType());
		AssociateDescription associate = DefaultConfigs.associateGreedy(scorer);
		AssociateDescription2D associate2D = new AssociateDescTo2D(associate);

		Info info = new Info();
		info.name = "FastNCC";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.dda(detector, describe , associate2D, 2,bandType);

		return info;
	}

	public Info createDefaultKlt(Class bandType) {

		ConfigPKlt configKlt = new ConfigPKlt();
		configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);

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

	public static class Info {
		public String name;
		public ImageType imageType;
		public PointTracker tracker;
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{PointTrackerRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
