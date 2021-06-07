package boofcv.regression;

import boofcv.abst.scene.ImageClassifier;
import boofcv.common.*;
import boofcv.factory.scene.ClassifierAndSource;
import boofcv.factory.scene.FactoryImageClassifier;
import boofcv.metrics.ClassifyImageSaveResults;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.Planar;
import deepboof.io.DeepBoofDataBaseOps;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This regression test is a change detection. Makes sure it produces the same results as previous versions
 * but does not check the results quality.
 *
 * @author Peter Abeles
 */
public class ImageClassificationRegression extends BaseRegression implements ImageRegression {

	public ImageClassificationRegression() {
		super(BoofRegressionConstants.TYPE_CLASSIFICATION);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		if( type.isInteger() ) {
			throw new ImageTypeNotSupportedException("Only F32 images supported. Skipping");
		}

		RuntimeSummary runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime,getClass(),"RUN_image_classification_change.txt");

		PrintStream out = new PrintStream(new File(directoryMetrics,"ACC_image_classification_change.txt"));
		BoofRegressionConstants.printGenerator(out,getClass());
		out.println("# Regression tests which outputs image classification results. A change indicates");
		out.println("# that the algorithm has changed in some way and should be inspected more closely.");

		List<Info> classifiers = new ArrayList<>();
		classifiers.add(new Info(FactoryImageClassifier.nin_imagenet(),"NiN_ImageNet"));
		classifiers.add(new Info(FactoryImageClassifier.vgg_cifar10(),"VGG_CIFAR10"));

		ClassifyImageSaveResults<Planar<GrayF32>> regression = new ClassifyImageSaveResults<>();
		regression.setOutput(out);

		for( Info info : classifiers ) {
			String name = info.name;
			ImageClassifier<Planar<GrayF32>> classifier = info.cas.getClassifier();

			out.println("\nEvaluating "+name);
			out.flush();
			System.out.println("Downloading model for "+classifier.getClass().getSimpleName());
			File path = DeepBoofDataBaseOps.downloadModel(info.cas.getSource(),new File("download_data"));

			System.out.println("Loading model");
			classifier.loadModel(path);

			regression.process(classifier);
			runtime.saveSummary(name,regression.processingTimeMS);
			out.flush();
		}

		runtime.printSummaryResults();
		runtime.out.close();
		out.close();
	}

	public static class Info {
		ClassifierAndSource cas;
		String name;

		public Info(ClassifierAndSource cas, String name) {
			this.cas = cas;
			this.name = name;
		}
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{ImageClassificationRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
