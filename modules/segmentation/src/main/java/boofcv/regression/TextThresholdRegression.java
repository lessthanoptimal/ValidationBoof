package boofcv.regression;

import boofcv.common.*;
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

		RuntimeSummary runtime = new RuntimeSummary();
		try {
			EvaluateTextThresholdDIBCO app = new EvaluateTextThresholdDIBCO();

			runtime.initializeLog(directoryRuntime,getClass(),"RUN_text_threshold.txt");

			PrintStream out = new PrintStream(new File(directoryMetrics,"ACC_text_threshold.txt"));
			BoofRegressionConstants.printGenerator(out, getClass());

			app.setOutputResults(out);
			app.runtime = runtime;

			app.addAlgorithm(FactoryThresholdAlgs.globalMean(), "Global_Mean");
			app.addAlgorithm(FactoryThresholdAlgs.globalOtsu(), "Global_Otsu");
			app.addAlgorithm(FactoryThresholdAlgs.globalEntropy(), "Global_Entropy");
			app.addAlgorithm(FactoryThresholdAlgs.globalLi(), "Global_Li");
			app.addAlgorithm(FactoryThresholdAlgs.globalHuang(), "Global_Huang");
			app.addAlgorithm(FactoryThresholdAlgs.localMean(), "Local_Mean");
			app.addAlgorithm(FactoryThresholdAlgs.localGaussian(), "Local_Gaussian");
			app.addAlgorithm(FactoryThresholdAlgs.localOtsu(), "Local_Otsu");
			app.addAlgorithm(FactoryThresholdAlgs.localSauvola(), "Local_Sauvola");
			app.addAlgorithm(FactoryThresholdAlgs.localNick(), "Local_NICK");
			app.addAlgorithm(FactoryThresholdAlgs.localBlockMinMax(), "Block_Min-Max");
			app.addAlgorithm(FactoryThresholdAlgs.localBlockMean(), "Block_Mean");
			app.addAlgorithm(FactoryThresholdAlgs.localBlockOtsu(), "Block_Otsu");

			app.evaluate();
		} catch( RuntimeException e ) {
			e.printStackTrace(errorLog);
		} finally {
			runtime.out.close();
		}
	}

	public static void main(String[] args)
			throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException
	{
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{TextThresholdRegression.class.getName(),ImageDataType.U8.toString()});
	}
}
