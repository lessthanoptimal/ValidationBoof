package boofcv.regression;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.common.BaseImageRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.RegressionRunner;
import boofcv.factory.feature.dense.*;
import boofcv.metrics.dense.EvalauteDescribeImageDense;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Regression test for dense image descriptors
 *
 * @author Peter Abeles
 */
public class DescribeImageDenseRegression extends BaseImageRegression {


	public DescribeImageDenseRegression() {
		super(BoofRegressionConstants.TYPE_FEATURE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {

		List<Info> algs = new ArrayList<Info>();

		Class imageType = ImageDataType.typeToSingleClass(type);

		DenseSampling sampling = new DenseSampling(10,10);

		ConfigDenseHoG hogFast = new ConfigDenseHoG();
		hogFast.fastVariant = true;
		ConfigDenseHoG hogOrig = new ConfigDenseHoG();
		hogOrig.fastVariant = false;


		algs.add(new Info("HOG-SB-"+type, FactoryDescribeImageDense.hog(hogOrig,ImageType.single(imageType))));
		algs.add(new Info("HOG-MS-"+type, FactoryDescribeImageDense.hog(hogOrig,ImageType.pl(3,imageType))));
		algs.add(new Info("HOG-F-SB-"+type, FactoryDescribeImageDense.hog(hogFast,ImageType.single(imageType))));
		algs.add(new Info("HOG-F-MS-"+type, FactoryDescribeImageDense.hog(hogFast,ImageType.pl(3,imageType))));
		algs.add(new Info("SURF-F-"+type, FactoryDescribeImageDense.surfFast(new ConfigDenseSurfFast(sampling),imageType)));
		algs.add(new Info("SURF-S-"+type, FactoryDescribeImageDense.surfStable(new ConfigDenseSurfStable(sampling),imageType)));
		algs.add(new Info("SIFT-"+type, FactoryDescribeImageDense.sift(new ConfigDenseSift(sampling),imageType)));

		// provide a couple of convenient images.  Results shouldn't vary too much
		List<String> images = new ArrayList<String>();
		images.add("data/fiducials/square_border_image/standard/distance_angle/image00001.png");
		images.add("data/calibration_mono/chessboard/distant/image00000.jpg");

		PrintStream out = new PrintStream(new FileOutputStream(new File(directory,"ACC_dense_image_descriptors.txt")));
		BoofRegressionConstants.printGenerator(out,getClass());

		for( Info info : algs) {
			System.out.println("Working on "+info.name);
			out.println();
			out.println("Algorithm:  "+info.name);
			EvalauteDescribeImageDense evaluator = new EvalauteDescribeImageDense(images,info.desc.getImageType());

			try {
				evaluator.setOutputStream(out);
				evaluator.evaluate(info.desc);
			} catch( RuntimeException e ) {
				e.printStackTrace();
				errorLog.println(e);
			}
		}
		System.out.println("   done");

	}

	public static class Info {
		public String name;
		public DescribeImageDense desc;

		public Info(String name, DescribeImageDense desc) {
			this.name = name;
			this.desc = desc;
		}
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DescribeImageDenseRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
