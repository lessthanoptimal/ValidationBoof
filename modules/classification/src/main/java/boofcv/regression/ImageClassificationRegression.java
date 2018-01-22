package boofcv.regression;

import boofcv.abst.scene.ImageClassifier;
import boofcv.common.BaseImageRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.RegressionRunner;
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
 * This regression test is a change detection.  Makes sure it produces the same results as previous versions
 * but does not check the results quality.
 *
 * @author Peter Abeles
 */
public class ImageClassificationRegression extends BaseImageRegression {
	@Override
	public void process(ImageDataType type) throws IOException {

		if( type.isInteger() ) {
			throw new RuntimeException("Only F32 images supported.  Skipping");
		}

		PrintStream out = new PrintStream(new File(directory,"image_classification_change.txt"));
		out.println("# Regression tests which outputs image classification results.  A change indicates");
		out.println("# that the algorithm has changed in some way and should be inspected more closely.");

		List<ClassifierAndSource> classifiers = new ArrayList<>();
		classifiers.add(FactoryImageClassifier.nin_imagenet());
		classifiers.add(FactoryImageClassifier.vgg_cifar10());

		ClassifyImageSaveResults<Planar<GrayF32>> regression = new ClassifyImageSaveResults<>();
		regression.setOutput(out);

		for( ClassifierAndSource cas : classifiers ) {
			ImageClassifier<Planar<GrayF32>> classifier = cas.getClassifier();

			out.println("\nEvaluating "+classifier.getClass().getSimpleName());
			out.flush();
			System.out.println("Downloading model for "+classifier.getClass().getSimpleName());
			File path = DeepBoofDataBaseOps.downloadModel(cas.getSource(),new File("download_data"));

			System.out.println("Loading model");
			classifier.loadModel(path);

			regression.process(classifier);
			out.flush();
		}
		out.close();
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{ImageClassificationRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
