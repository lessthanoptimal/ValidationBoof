package boofcv.regression;

import boofcv.common.BaseImageRegression;
import boofcv.metrics.threshold.EvaluateTextThresholdDIBCO;
import boofcv.metrics.threshold.FactoryThresholdAlgs;
import boofcv.struct.image.ImageDataType;

import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class TextThresholdRegression extends BaseImageRegression {

	@Override
	public void process(ImageDataType type) throws IOException {
		System.out.println(getClass().getSimpleName());

		try {
			EvaluateTextThresholdDIBCO app = new EvaluateTextThresholdDIBCO();

			app.setOutputDirectory(directory);

			app.addAlgorithm(FactoryThresholdAlgs.globalMean(), "global Mean");
			app.addAlgorithm(FactoryThresholdAlgs.globalOtsu(), "global Otsu");
			app.addAlgorithm(FactoryThresholdAlgs.globalEntropy(), "global Entropy");
			app.addAlgorithm(FactoryThresholdAlgs.localSquare(), "local Square");
			app.addAlgorithm(FactoryThresholdAlgs.localGaussian(), "local Gaussian");
			app.addAlgorithm(FactoryThresholdAlgs.localSauvola(), "local Sauvola");
			app.addAlgorithm(FactoryThresholdAlgs.localBlockMinMax(), "local Block Min-Max");

			app.evaluate();
		} catch( RuntimeException e ) {
			e.printStackTrace(errorLog);
		}
	}
}
