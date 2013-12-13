package regression;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociateEuclideanSq_F64;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import validate.fast.DetectFast;
import validate.features.homography.BenchmarkFeatureDescribeStability;
import validate.features.homography.BenchmarkFeatureDetectStability;
import validate.features.homography.CreateDetectDescribeFile;
import validate.features.homography.LoadHomographyBenchmarkFiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectDescribeRegression implements TextFileRegression {

	public static final String path = "data/affinevgg/";

	String directory;
	PrintStream errorLog;

	// association tolerance for detection score
	double detectTolerance = 1.5;
	// association tolerance for describe score
	double describeTolerance = 3;

	@Override
	public void setOutputDirectory(String directory) {
		this.directory = directory;
		try {
			errorLog = new PrintStream(directory+"ERRORLOG_DetectDescribeRegression.txt");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		File tmp = new File("tmp");
		if( !tmp.exists() ) {
			if( !tmp.mkdir() )
				throw new RuntimeException("Can't create tmp directory");
		}
	}

	public void process( ImageDataType type ) throws IOException {

		List<Info> all = new ArrayList<Info>();

		Class bandType = ImageDataType.typeToClass(type);

		all.add(surf(false,false,bandType));
		all.add(surf(false,true,bandType));
		all.add(surf(true,false,bandType));
		all.add(surf(true,true,bandType));
		all.add(sift(bandType));

		ScoreAssociation score = new ScoreAssociateEuclideanSq_F64();
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.greedy(score, Double.MAX_VALUE, true);

		BenchmarkFeatureDescribeStability describeStability =
				new BenchmarkFeatureDescribeStability(assoc, directory,"./tmp/",describeTolerance);
		BenchmarkFeatureDetectStability detectorStability =
				new BenchmarkFeatureDetectStability(assoc, directory, "./tmp/",detectTolerance);

		for( String d : LoadHomographyBenchmarkFiles.DATA_SETS ) {
			detectorStability.addDirectory(path + d);
			describeStability.addDirectory(path + d);
		}

		for( Info i : all ) {
			CreateDetectDescribeFile creator = new CreateDetectDescribeFile(i.detdesc,i.imageType,i.name);
			for( String d : LoadHomographyBenchmarkFiles.DATA_SETS ) {
				try {
					creator.directory(path+d,"./tmp/");
				} catch( RuntimeException e ) {
					errorLog.println("FAILED "+i.name+" on "+d);
					errorLog.println(e);
					e.printStackTrace(errorLog);
				}
			}
			detectorStability.evaluate(i.name);
			describeStability.evaluate(i.name);
		}
	}

	@Override
	public List<String> getFileNames() {
		List<String> names = new ArrayList<String>();

		names.add(DetectFast.FILE_NAME);

		return names;
	}

	public static <T extends ImageSingleBand>
	Info surf( boolean stable , boolean color , Class<T> bandType  ) {

		ConfigFastHessian configDetect = new ConfigFastHessian(3, 2, -1,1, 9, 4, 4);

		ImageType imageType;
		String variant;
		DetectDescribePoint<T,SurfFeature> detdesc;

		if( stable )
			variant="Stable";
		else
			variant="Fast";
		if( color )
			variant+="_Color";

		if( color ) {
			imageType = ImageType.ms(3,bandType);
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

	public static <T extends ImageSingleBand>
	Info sift( Class<T> bandType  ) {

		ImageType imageType = ImageType.single(bandType);

		ConfigSiftScaleSpace confSS = new ConfigSiftScaleSpace(1.6f,5,4,false);

		DetectDescribePoint<ImageFloat32,SurfFeature> sift =
				FactoryDetectDescribe.sift(confSS, new ConfigSiftDetector(3, 10, -1, 32), null, null);

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

	public static void main(String[] args) throws IOException {

		DetectDescribeRegression app = new DetectDescribeRegression();

		app.setOutputDirectory(RegressionManagerApp.CURRENT_DIRECTORY+"/"+ImageDataType.U8+"/");
		app.process(ImageDataType.U8);
	}
}
