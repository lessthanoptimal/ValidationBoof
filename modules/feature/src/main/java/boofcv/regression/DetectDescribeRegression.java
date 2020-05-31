package boofcv.regression;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociateEuclideanSq;
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
import boofcv.common.*;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.metrics.homography.BenchmarkFeatureDescribeStability;
import boofcv.metrics.homography.BenchmarkFeatureDetectStability;
import boofcv.metrics.homography.CreateDetectDescribeFile;
import boofcv.metrics.homography.LoadHomographyBenchmarkFiles;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.GrowQueue_F64;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
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
	public void process( ImageDataType dataType ) throws IOException {

		List<Info> all = new ArrayList<>();

		all.add(surf(false,false));
		all.add(surf(false,true));
		all.add(surf(true,false));
		all.add(surf(true,true));
//		all.add(briefSoFH());  // TODO add support for binary descriptors
		all.add(sift());

		ScoreAssociation<TupleDesc_F64> score = new ScoreAssociateEuclideanSq.F64();
		AssociateDescription<TupleDesc_F64> assoc = DefaultConfigs.associateGreedy(score);

		String tmp = BoofRegressionConstants.tempDir().toString();

		BenchmarkFeatureDescribeStability describeStability =
				new BenchmarkFeatureDescribeStability(assoc, directoryMetrics,tmp,describeTolerance);
		BenchmarkFeatureDetectStability detectorStability =
				new BenchmarkFeatureDetectStability(directoryMetrics, tmp,detectTolerance);

		for( String d : LoadHomographyBenchmarkFiles.DATA_SETS )
		{
			detectorStability.addDirectory(new File(path,d).getPath());
			describeStability.addDirectory(new File(path,d).getPath());
		}

		RuntimeSummary runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime,getClass(),"RUN_detect_describe.txt");

		for( Info i : all ) {
			// Print header info for per dataset results
			GrowQueue_F64 summaryTimeMS = new GrowQueue_F64();
			runtime.out.println(i.name);
			runtime.printUnitsRow(false);

			CreateDetectDescribeFile creator = new CreateDetectDescribeFile(i.factory,i.imageFamily,dataType,i.name);
			for( String d : LoadHomographyBenchmarkFiles.DATA_SETS ) {
				try {
					creator.directory(new File(path,d).getPath(),tmp);
					summaryTimeMS.addAll(creator.processingTimeMS);
					runtime.printStatsRow(d,creator.processingTimeMS);
				} catch( RuntimeException e ) {
					errorLog.println("FAILED "+i.name+" on "+d);
					errorLog.println(e);
					e.printStackTrace(errorLog);
				}
			}
			// save summary runtime statistics for this algorithm to print later
			runtime.saveSummary(i.name,summaryTimeMS);
			runtime.out.println();
			detectorStability.evaluate(i.name);
			describeStability.evaluate(i.name);
		}
		runtime.out.println();
		runtime.printSummaryResults();
		runtime.out.close();
	}

	public static <T extends ImageGray<T>>
	Info surf( boolean stable , boolean color ) {

		ConfigFastHessian configDetect = new ConfigFastHessian(3, 2, -1,1, 9, 4, 4);

		String variant;

		if( stable )
			variant="Stable";
		else
			variant="Fast";
		if( color )
			variant+="_Color";


		Info ret = new Info();
		ret.name = "BoofSURF-"+variant;
		ret.factory = new CreateDetectDescribeFile.Factory() {
			@Override
			public <IT extends ImageBase<IT>, D extends TupleDesc_F64>
			DetectDescribePoint<IT, D> create(ImageType<IT> imageType) {
				Class type = imageType.getImageClass();
				if( color ) {
					if( stable )
						return FactoryDetectDescribe.surfColorStable(configDetect, null, null, (ImageType)imageType);
					else
						return FactoryDetectDescribe.surfColorFast(configDetect,null,null,(ImageType)imageType);
				} else {
					if( stable )
						return FactoryDetectDescribe.surfStable(configDetect,null,null,type);
					else
						return FactoryDetectDescribe.surfFast(configDetect,null,null,type);
				}
			}
		};
		ret.imageFamily = color ? ImageType.Family.PLANAR : ImageType.Family.GRAY;

		return ret;
	}

	public static <T extends ImageGray<T>>
	Info briefSoFH() {
		ConfigFastHessian configDetect = new ConfigFastHessian(3, 2, -1,1, 9, 4, 4);
		ConfigBrief configDesc = new ConfigBrief(false);


		Info ret = new Info();
		ret.name = "BriefSO-FastHess";
		ret.factory = new CreateDetectDescribeFile.Factory() {
			@Override
			public <IT extends ImageBase<IT>, D extends TupleDesc_F64>
			DetectDescribePoint<IT, D> create(ImageType<IT> imageType) {
				Class type = imageType.getImageClass();
				InterestPointDetector detector = FactoryInterestPoint.fastHessian(configDetect,type);
				DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(configDesc,type);

				Class iiType = GIntegralImageOps.getIntegralType(type);
				OrientationIntegral ori = FactoryOrientationAlgs.average_ii(null,iiType );
				OrientationImage orientation = new OrientationIntegralToImage(ori,type,iiType);
				return FactoryDetectDescribe.fuseTogether(detector,orientation,describe);
			}
		};
		ret.imageFamily = ImageType.Family.GRAY;

		return ret;
	}

	public static <T extends ImageGray<T>>
	Info sift() {

		ConfigCompleteSift config = new ConfigCompleteSift();
		ConfigSiftDetector configDet = config.detector;
		configDet.extract.radius = 3;
		configDet.extract.threshold = 0f;
		configDet.maxFeaturesPerScale = 3500;

		Info ret = new Info();
		ret.name = "BoofSIFT";
		ret.factory = new CreateDetectDescribeFile.Factory() {
			@Override
			public <IT extends ImageBase<IT>, D extends TupleDesc_F64>
			DetectDescribePoint<IT, D> create(ImageType<IT> imageType) {
				return FactoryDetectDescribe.sift(config,imageType.getImageClass());
			}
		};
		ret.imageFamily = ImageType.Family.GRAY;

		return ret;
	}

	public static class Info {
		public String name;
		public ImageType.Family imageFamily;
		public CreateDetectDescribeFile.Factory factory;
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DetectDescribeRegression.class.getName(),ImageDataType.F32.toString()});
//		RegressionRunner.main(new String[]{DetectDescribeRegression.class.getName(),ImageDataType.U8.toString()});
	}
}
