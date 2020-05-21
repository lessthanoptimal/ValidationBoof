package boofcv.regression;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.common.*;
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
public class DescribeImageDenseRegression extends BaseRegression implements ImageRegression {


	public DescribeImageDenseRegression() {
		super(BoofRegressionConstants.TYPE_FEATURE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		List<Info> algs = new ArrayList<>();

		Class imageType = ImageDataType.typeToSingleClass(type);

		DenseSampling sampling = new DenseSampling(10,10);

		ConfigDenseHoG hogFast = new ConfigDenseHoG();
		hogFast.fastVariant = true;
		ConfigDenseHoG hogOrig = new ConfigDenseHoG();
		hogOrig.fastVariant = false;


		algs.add(new Info("HOG-SB-"+type, FactoryDescribeImageDense.hog(hogOrig,ImageType.single(imageType))));
		algs.add(new Info("HOG-PL-"+type, FactoryDescribeImageDense.hog(hogOrig,ImageType.pl(3,imageType))));
		algs.add(new Info("HOG-F-SB-"+type, FactoryDescribeImageDense.hog(hogFast,ImageType.single(imageType))));
		algs.add(new Info("HOG-F-PL-"+type, FactoryDescribeImageDense.hog(hogFast,ImageType.pl(3,imageType))));
		algs.add(new Info("SURF-F-"+type, FactoryDescribeImageDense.surfFast(new ConfigDenseSurfFast(sampling),imageType)));
		algs.add(new Info("SURF-S-"+type, FactoryDescribeImageDense.surfStable(new ConfigDenseSurfStable(sampling),imageType)));
		algs.add(new Info("SIFT-"+type, FactoryDescribeImageDense.sift(new ConfigDenseSift(sampling),imageType)));

		// provide a couple of convenient images.  Results shouldn't vary too much
		List<String> images = new ArrayList<String>();
		images.add("data/fiducials/square_border_image/standard/distance_angle/image00001.png");
		images.add("data/calibration_mono/chessboard/distant/image00000.jpg");

		PrintStream outputAccuracy = new PrintStream(new FileOutputStream(new File(directoryMetrics,"ACC_dense_image_descriptors.txt")));
		BoofRegressionConstants.printGenerator(outputAccuracy,getClass());

		RuntimeSummary outputRuntime = new RuntimeSummary();
		outputRuntime.out = new PrintStream(new File(directoryRuntime, "RUN_dense_image_descriptors.txt"));
		BoofRegressionConstants.printGenerator(outputRuntime.out, getClass());
		outputRuntime.out.println("# All times are in milliseconds");
		outputRuntime.out.println();
		outputRuntime.printUnitsRow(true);

		for( Info info : algs) {
			System.out.println("Working on "+info.name);
			outputAccuracy.println();
			outputAccuracy.println("Algorithm:  "+info.name);
			EvalauteDescribeImageDense evaluator = new EvalauteDescribeImageDense(images,info.desc.getImageType());

			try {
				evaluator.setOutputStream(outputAccuracy);
				evaluator.evaluate(info.desc);
				outputRuntime.printStatsRow(info.name, evaluator.processingTimeMS);
			} catch( RuntimeException e ) {
				e.printStackTrace();
				errorLog.println(e);
			}
			outputAccuracy.println();
		}
		outputAccuracy.close();
		outputRuntime.out.close();
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
