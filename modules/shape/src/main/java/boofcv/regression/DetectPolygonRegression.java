package boofcv.regression;

import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FactoryObject;
import boofcv.common.ImageRegression;
import boofcv.metrics.DetectPolygonsSaveToFile;
import boofcv.metrics.EvaluatePolygonDetector;
import boofcv.metrics.FactoryBinaryPolygon;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectPolygonRegression extends BaseRegression implements ImageRegression {

	File workDirectory = new File("./tmp");
	File baseDataSetDirectory = new File("data/shape/polygon");

	PrintStream outputSpeed;

	public DetectPolygonRegression() {
		super(BoofRegressionConstants.TYPE_SHAPE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		outputSpeed = new PrintStream(new File(directory,"RUN_PolygonDetector.txt"));
		BoofRegressionConstants.printGenerator(outputSpeed, getClass());

		process("BinaryGlobal", false, new FactoryBinaryPolygon(imageType));
		process("BinaryLocal", true, new FactoryBinaryPolygon(imageType));

		outputSpeed.close();
	}

	private void process(String name, boolean localBinary , FactoryObject<DetectPolygonBinaryGrayRefine> factory)
			throws IOException {

		String outputAccuracyName = "ACC_PolygonDetector_"+name+".txt";

		EvaluatePolygonDetector evaluator = new EvaluatePolygonDetector();

		PrintStream outputAccuracy = new PrintStream(new File(directory,outputAccuracyName));
		BoofRegressionConstants.printGenerator(outputAccuracy, getClass());
		evaluator.setOutputResults(outputAccuracy);

		outputSpeed.println("# Average processing time of shape detector algorithm "+name);

		List<File> files = BoofRegressionConstants.listAndSort(baseDataSetDirectory);

		int totalTruePositive = 0;
		int totalExpected = 0;
		int totalFalsePositive = 0;

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			outputAccuracy.println("# Data Set = "+f.getName());

			factory.configure(new File(f,"detector.txt"));

			DetectPolygonsSaveToFile detection = new DetectPolygonsSaveToFile(factory.newInstance(),localBinary);

			detection.processDirectory(f, workDirectory);
			evaluator.evaluate(f, workDirectory);
			outputAccuracy.println();
			totalTruePositive += evaluator.summaryTruePositive;
			totalExpected += evaluator.summaryExpected;
			totalFalsePositive += evaluator.summaryFalsePositive;

			outputSpeed.printf("%20s %9.4f (ms)\n",f.getName(),detection.averageProcessingTime);
		}
		outputSpeed.println();

		outputAccuracy.println();
		outputAccuracy.println(String.format("Final Summary: TP/(TP+FN) = %d / %d    FP = %d\n",totalTruePositive,totalExpected,totalFalsePositive));
		outputAccuracy.close();

	}

	public static void main(String[] args) throws IOException {
		DetectPolygonRegression app = new DetectPolygonRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
