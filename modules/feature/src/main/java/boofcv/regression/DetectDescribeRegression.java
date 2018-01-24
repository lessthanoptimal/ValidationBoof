package boofcv.regression;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociateEuclideanSq_F64;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.ConfigCompleteSift;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.abst.feature.orientation.OrientationIntegralToImage;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.common.RegressionRunner;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.metrics.homography.BenchmarkFeatureDescribeStability;
import boofcv.metrics.homography.BenchmarkFeatureDetectStability;
import boofcv.metrics.homography.CreateDetectDescribeFile;
import boofcv.metrics.homography.LoadHomographyBenchmarkFiles;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectDescribeRegression extends BaseRegression implements ImageRegression {

	public static final String path = "data/affinevgg/";

	// association tolerance for detection score
	double detectTolerance = 1.5;
	// association tolerance for describe score
	double describeTolerance = 3;

	public DetectDescribeRegression() {
		super(BoofRegressionConstants.TYPE_FEATURE);
	}

	@Override
	public void process( ImageDataType type ) throws IOException {

		List<Info> all = new ArrayList<Info>();

		Class bandType = ImageDataType.typeToSingleClass(type);

		all.add(surf(false,false,bandType));
		all.add(surf(false,true,bandType));
		all.add(surf(true,false,bandType));
		all.add(surf(true,true,bandType));
//		all.add(briefSoFH(bandType));  // TODO add support for binary descriptors
		all.add(sift(bandType));

		ScoreAssociation score = new ScoreAssociateEuclideanSq_F64();
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.greedy(score, Double.MAX_VALUE, true);

		String tmp = BoofRegressionConstants.tempDir().toString();

		BenchmarkFeatureDescribeStability describeStability =
				new BenchmarkFeatureDescribeStability(assoc, directory,tmp,describeTolerance);
		BenchmarkFeatureDetectStability detectorStability =
				new BenchmarkFeatureDetectStability(directory, tmp,detectTolerance);

		for( String d : LoadHomographyBenchmarkFiles.DATA_SETS )
		{
			detectorStability.addDirectory(new File(path,d).getPath());
			describeStability.addDirectory(new File(path,d).getPath());
		}

		PrintStream outputRuntime = new PrintStream(new File(directory,"RUN_detect_describe.txt"));
		BoofRegressionConstants.printGenerator(outputRuntime,getClass());
		outputRuntime.println("# Runtime for feature detect describe");
		outputRuntime.println("# <directory> <average time in ms>\n");
		for( Info i : all ) {
			outputRuntime.println(i.name);
			CreateDetectDescribeFile creator = new CreateDetectDescribeFile(i.detdesc,i.imageType,i.name);
			for( String d : LoadHomographyBenchmarkFiles.DATA_SETS ) {
				try {
					creator.directory(new File(path,d).getPath(),tmp);
					outputRuntime.printf(" %10s %.2f\n",d,creator.getAverageProcessingTime() );
				} catch( RuntimeException e ) {
					errorLog.println("FAILED "+i.name+" on "+d);
					errorLog.println(e);
					e.printStackTrace(errorLog);
				}
			}
			outputRuntime.println();
			detectorStability.evaluate(i.name);
			describeStability.evaluate(i.name);
		}
		outputRuntime.close();
	}

	public static <T extends ImageGray<T>>
	Info surf( boolean stable , boolean color , Class<T> bandType  ) {

		ConfigFastHessian configDetect = new ConfigFastHessian(3, 2, -1,1, 9, 4, 4);

		ImageType imageType;
		String variant;
		DetectDescribePoint<T,BrightFeature> detdesc;

		if( stable )
			variant="Stable";
		else
			variant="Fast";
		if( color )
			variant+="_Color";

		if( color ) {
			imageType = ImageType.pl(3,bandType);
			if( stable )
				detdesc = FactoryDetectDescribe.surfColorStable(configDetect, null, null, imageType);
			else
				detdesc = FactoryDetectDescribe.surfColorFast(configDetect,null,null,imageType);
		} else {
			imageType = ImageType.single(bandType);
			if( stable )
				detdesc = FactoryDetectDescribe.surfStable(configDetect,null,null,bandType);
			else
				detdesc = FactoryDetectDescribe.surfFast(configDetect,null,null,bandType);
		}

		Info ret = new Info();
		ret.name = "BoofSURF-"+variant;
		ret.detdesc = detdesc;
		ret.imageType = imageType;

		return ret;
	}

	public static <T extends ImageGray<T>>
	Info briefSoFH( Class<T> bandType  ) {

		ConfigFastHessian configDetect = new ConfigFastHessian(3, 2, -1,1, 9, 4, 4);
		ConfigBrief configDesc = new ConfigBrief(false);

		InterestPointDetector<T> detector = FactoryInterestPoint.fastHessian(configDetect);
		DescribeRegionPoint<T,TupleDesc_B> describe = FactoryDescribeRegionPoint.brief(configDesc,bandType);

		Class iiType = GIntegralImageOps.getIntegralType(bandType);
		OrientationIntegral ori = FactoryOrientationAlgs.average_ii(null,iiType );
		OrientationImage<T> orientation = new OrientationIntegralToImage(ori,bandType,iiType);

		DetectDescribePoint<T,TupleDesc_B> detdesc =
				FactoryDetectDescribe.fuseTogether(detector,orientation,describe);

		Info ret = new Info();
		ret.name = "BriefSO-FastHess";
		ret.detdesc = detdesc;
		ret.imageType = ImageType.single(bandType);

		return ret;
	}

	public static <T extends ImageGray<T>>
	Info sift( Class<T> bandType  ) {

		ImageType imageType = ImageType.single(bandType);

		ConfigCompleteSift config = new ConfigCompleteSift();
//		ConfigSiftScaleSpace configSS = config.scaleSpace;
		ConfigSiftDetector configDet = config.detector;
		configDet.extract.radius = 3;
		configDet.extract.threshold = 0f;
		configDet.maxFeaturesPerScale = 3500;

//		ConfigSiftOrientation configOri = config.orientation;
//		ConfigSiftDescribe configDesc = config.describe;

		DetectDescribePoint<T,BrightFeature> sift = FactoryDetectDescribe.sift(config);

		Info ret = new Info();
		ret.name = "BoofSIFT";
		ret.detdesc = sift;
		ret.imageType = imageType;

		return ret;
	}

	public static class Info {
		public String name;
		public ImageType imageType;
		public DetectDescribePoint detdesc;
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DetectDescribeRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
