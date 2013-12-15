package regression;

import boofcv.abst.feature.associate.AssociateDescTo2D;
import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.detect.interest.WrapFHtoInterestPoint;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import georegression.struct.homo.Homography2D_F64;
import validate.ValidationConstants;
import validate.tracking.EvaluateTrackerStability;
import validate.tracking.EvaluationTracker;
import validate.tracking.LogParseHomography;
import validate.tracking.WrapPointTracker;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class PointTrackerRegression extends BaseTextFileRegression {

	public static String pathToData = ValidationConstants.PATH_DATA+"abeles2012/";
	public static double tolerance = 5;
	String dataDirectories[] = new String[]{"bricks","carpet","various"};
	String dataSets[] = new String[]{"skew","rotate","move_out","move_in"};
	int skips[] = new int[]{1,4,8};

	@Override
	public void process(ImageDataType type) throws IOException {
		List<Info> all = new ArrayList<Info>();
		Class bandType = ImageDataType.typeToClass(type);

		all.add( createDefaultKlt(bandType));
		all.add( createShiBrief(bandType));

		for( Info info : all ) {
			try {
				process(info,bandType);
			} catch( RuntimeException e ) {
				errorLog.println("Tracker "+info.name);
				e.printStackTrace(errorLog);
				errorLog.println("----------------------------");
			}
		}
	}

	protected void process( Info info , Class bandType ) throws FileNotFoundException {
		PrintStream outSummary = new PrintStream(new FileOutputStream(directory+"PointTracker_"+info.name+".txt"));
		outSummary.println("# Inlier Tolerance " + tolerance + "  Algorithm " + info.name);
		outSummary.println("# (File) (Skip) (F) (F all inside) (Precision) (Recall) (Recall all inside) (Tracks)");

		for( String directory : dataDirectories ) {
			for( String whichData : dataSets ) {
				for( int skip : skips )  {
					String path = pathToData+directory+"/"+whichData;

					WrapPointTracker wrapped = new WrapPointTracker(info.tracker);
					computeResults(bandType, wrapped, path, skip, outSummary, null);
				}
			}
		}

		outSummary.close();
	}

	private static void computeResults(Class imageType,
									   EvaluationTracker tracker ,
									   String whichData, int skip,
									   PrintStream outSummary, PrintStream outTime)
			throws FileNotFoundException
	{
		SimpleImageSequence sequence =
				DefaultMediaManager.INSTANCE.openVideo(whichData + "_undistorted.mjpeg", ImageType.single(imageType));

		List<Homography2D_F64> groundTruth = LogParseHomography.parse(whichData + "_homography.txt");

		EvaluateTrackerStability app = new EvaluateTrackerStability(tolerance,skip);

		app.evaluate(tracker,sequence,groundTruth,outTime);

		outSummary.printf("%s %2d %6.3f %6.3f %6.3f %6.3f %6.3f %6.1f\n", whichData, skip,
				app.getMeanF(), app.getMeanFA(), app.getMeanPrecision(), app.getMeanRecall(), app.getMeanRecallA(),
				app.getMeanTrackCount());
	}

	// TODO trackers for Shi-Tomasi brief, harris NCC, FH-KLT-SURF

	public Info createShiBrief( Class bandType ) {
		Class derivType = GImageDerivativeOps.getDerivativeType(bandType);

		int radius = 3;

		GeneralFeatureIntensity intensity = FactoryIntensityPoint.shiTomasi(radius,false,derivType);
		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(new ConfigExtract(radius,10));
		GeneralFeatureDetector detector = FactoryFeatureExtractor.general(intensity,nonmax,800);

		DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(null,bandType);
		ScoreAssociation score = FactoryAssociation.defaultScore(describe.getDescriptionType());
		AssociateDescription associate = FactoryAssociation.greedy(score,Double.MAX_VALUE,true);
		AssociateDescription2D associate2D = new AssociateDescTo2D(associate);

		Info info = new Info();
		info.name = "ShiBrief";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.dda(detector, describe , associate2D, 2,bandType);

		return info;
	}

	public Info createDefaultKlt(Class bandType) {
		Info info = new Info();
		info.name = "DefaultKLT";
		info.imageType = ImageType.single(bandType);
		info.tracker = FactoryPointTracker.klt(null, null);

		return info;
	}

	private InterestPointDetector createDetectorFH(Class bandType) {
		float detectThreshold = 1;
		int extractRadius = 2;
		int maxFeaturesPerScale = 200;
		int initialSampleSize = 1;
		int initialSize = 9;
		int numberScalesPerOctave = 4;
		int numberOfOctaves = 4;

		NonMaxSuppression extractor = FactoryFeatureExtractor.
				nonmax(new ConfigExtract(extractRadius, detectThreshold, 5, true));
		FastHessianFeatureDetector feature = new FastHessianFeatureDetector(extractor,maxFeaturesPerScale,
				initialSampleSize, initialSize,numberScalesPerOctave,numberOfOctaves);

		return new WrapFHtoInterestPoint(feature);
	}

	public static class Info {
		public String name;
		public ImageType imageType;
		public PointTracker tracker;
	}

	public static void main(String[] args) throws IOException {

		PointTrackerRegression app = new PointTrackerRegression();

		app.setOutputDirectory(GenerateRegressionData.CURRENT_DIRECTORY+"/"+ImageDataType.U8+"/");
		app.process(ImageDataType.U8);
	}
}
