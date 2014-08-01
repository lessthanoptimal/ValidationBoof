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

		app.addAlgorithm(FactoryThresholdAlgs.mean(),"mean");
		app.addAlgorithm(FactoryThresholdAlgs.otsu(),"otsu");
		app.addAlgorithm(FactoryThresholdAlgs.entropy(),"entropy");
		app.addAlgorithm(FactoryThresholdAlgs.localSquare(),"local square");
		app.addAlgorithm(FactoryThresholdAlgs.localGaussian(),"local gaussian");
		app.addAlgorithm(FactoryThresholdAlgs.adaptiveSauvola(),"Sauvola");

		app.evaluate();
	}
}
