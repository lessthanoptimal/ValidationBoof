package regression;

import boofcv.struct.image.ImageDataType;
import validate.threshold.EvaluateTextThresholdDIBCO;
import validate.threshold.FactoryThresholdAlgs;

import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class TextThresholdRegression extends BaseTextFileRegression {
	@Override
	public void process(ImageDataType type) throws IOException {
		EvaluateTextThresholdDIBCO app = new EvaluateTextThresholdDIBCO();

		app.setOutputDirectory(directory);

		app.addAlgorithm(FactoryThresholdAlgs.globalMean(),"global Mean");
		app.addAlgorithm(FactoryThresholdAlgs.globalOtsu(),"global Otsu");
		app.addAlgorithm(FactoryThresholdAlgs.globalEntropy(),"global Entropy");
		app.addAlgorithm(FactoryThresholdAlgs.localSquare(),"local Square");
		app.addAlgorithm(FactoryThresholdAlgs.localGaussian(),"local Gaussian");
		app.addAlgorithm(FactoryThresholdAlgs.localSauvola(),"local Sauvola");
		app.addAlgorithm(FactoryThresholdAlgs.localBlockMinMax(),"local Block Min-Max");

		app.evaluate();
	}
}
