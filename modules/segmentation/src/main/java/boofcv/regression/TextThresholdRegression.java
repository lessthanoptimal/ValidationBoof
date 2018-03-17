package boofcv.regression;

import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.metrics.threshold.EvaluateTextThresholdDIBCO;
import boofcv.metrics.threshold.FactoryThresholdAlgs;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class TextThresholdRegression extends BaseRegression implements ImageRegression {

	public TextThresholdRegression() {
		super(BoofRegressionConstants.TYPE_SEGMENTATION);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		System.out.println(getClass().getSimpleName());

		try {
			EvaluateTextThresholdDIBCO app = new EvaluateTextThresholdDIBCO();

			PrintStream out = new PrintStream(new File(directory,"ACC_text_threshold.txt"));
			BoofRegressionConstants.printGenerator(out, getClass());

			app.setOutputResults(out);

			app.addAlgorithm(FactoryThresholdAlgs.globalMean(), "global Mean");
			app.addAlgorithm(FactoryThresholdAlgs.globalOtsu(), "global Otsu");
			app.addAlgorithm(FactoryThresholdAlgs.globalEntropy(), "global Entropy");
			app.addAlgorithm(FactoryThresholdAlgs.localMean(), "local Mean");
			app.addAlgorithm(FactoryThresholdAlgs.localGaussian(), "local Gaussian");
			app.addAlgorithm(FactoryThresholdAlgs.localOtsu(), "local Otsu");
			app.addAlgorithm(FactoryThresholdAlgs.localSauvola(), "local Sauvola");
			app.addAlgorithm(FactoryThresholdAlgs.localBlockMinMax(), "block Min-Max");
			app.addAlgorithm(FactoryThresholdAlgs.localBlockMean(), "block Mean");
			app.addAlgorithm(FactoryThresholdAlgs.localBlockOtsu(), "block Otsu");

			app.evaluate();
		} catch( RuntimeException e ) {
			e.printStackTrace(errorLog);
		}
	}

	public static void main(String[] args) throws IOException {
		TextThresholdRegression regression = new TextThresholdRegression();
		regression.process(ImageDataType.U8);
	}
}
