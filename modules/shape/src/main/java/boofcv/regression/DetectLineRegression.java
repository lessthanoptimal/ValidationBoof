package boofcv.regression;

import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.metrics.DetectLinesSaveToFile;
import boofcv.metrics.EvaluateHoughLineDetector;
import boofcv.metrics.FactoryLineDetector;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class DetectLineRegression extends BaseRegression implements ImageRegression {

	File workDirectory = new File("./tmp");
	File baseDirectory = new File("data/shape/line/");

	PrintStream outputSpeed;

	public DetectLineRegression() {
		super(BoofRegressionConstants.TYPE_SHAPE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		processFamily(imageType, "Thin", true,FactoryLineDetector.THIN);
		processFamily(imageType, "Edge", false,FactoryLineDetector.EDGE);
	}

	private void processFamily(Class imageType, String typeName, boolean thin, String[] algs ) throws IOException {
		outputSpeed = new PrintStream(new File(directory,"RUN_"+typeName+"LineDetector.txt"));
		BoofRegressionConstants.printGenerator(outputSpeed, getClass());
		for( String detectorName : algs ) {
			process(detectorName,thin,typeName,imageType);
		}
		outputSpeed.close();
	}

	private void process(String name , boolean thin , String FooName, Class imageType)
			throws IOException {

		String outputAccuracyName = "ACC_"+FooName+"LineDetector_"+name+".txt";

		EvaluateHoughLineDetector evaluator = new EvaluateHoughLineDetector();

		PrintStream outputAccuracy = new PrintStream(new File(directory,outputAccuracyName));
		BoofRegressionConstants.printGenerator(outputAccuracy, getClass());
		evaluator.setOutputResults(outputAccuracy);

		outputSpeed.println("# Average processing time of shape detector algorithm "+name);

		DetectLinesSaveToFile detection = new DetectLinesSaveToFile(thin,name,3,imageType);
		File f = new File(baseDirectory,FooName.toLowerCase());
		detection.processDirectory(f, workDirectory);
		evaluator.evaluate(f, workDirectory);
		outputAccuracy.println();

		outputSpeed.printf("%20s %9.4f (ms)\n",f.getName(),detection.averageProcessingTime);
		outputSpeed.println();

		outputAccuracy.close();

	}

	public static void main(String[] args) throws IOException {
		DetectLineRegression app = new DetectLineRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
