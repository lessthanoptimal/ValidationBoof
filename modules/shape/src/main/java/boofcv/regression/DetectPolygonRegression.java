package boofcv.regression;

import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.common.BaseImageRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FactoryObject;
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
public class DetectPolygonRegression extends BaseImageRegression {

	File workDirectory = new File("./tmp");
	File baseDataSetDirectory = new File("data/shape/polygon");

	String infoString;

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		process("PolygonLineGlobal", false, new FactoryBinaryPolygon(imageType));
		process("PolygonLineLocal", true, new FactoryBinaryPolygon(imageType));
	}

	private void process(String name, boolean localBinary , FactoryObject<DetectPolygonBinaryGrayRefine> factory)
			throws IOException {

		String outputName = "ShapeDetector_"+name+".txt";
		String outputSpeedName = "ShapeDetectorSpeed_"+name+".txt";

		EvaluatePolygonDetector evaluator = new EvaluatePolygonDetector();

		PrintStream output = new PrintStream(new File(directory,outputName));
		evaluator.setOutputResults(output);

		PrintStream outputSpeed = new PrintStream(new File(directory,outputSpeedName));
		outputSpeed.println("# Average processing time of shape detector algorithm "+name);

		List<File> files = BoofRegressionConstants.listAndSort(baseDataSetDirectory);

		int totalTruePositive = 0;
		int totalExpected = 0;
		int totalFalsePositive = 0;

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			output.println("# Data Set = "+f.getName());

			factory.configure(new File(f,"detector.txt"));

			DetectPolygonsSaveToFile detection = new DetectPolygonsSaveToFile(factory.newInstance(),localBinary);

			detection.processDirectory(f, workDirectory);
			evaluator.evaluate(f, workDirectory);
			output.println();
			totalTruePositive += evaluator.summaryTruePositive;
			totalExpected += evaluator.summaryExpected;
			totalFalsePositive += evaluator.summaryFalsePositive;

			outputSpeed.printf("%20s %9.4f (ms)\n",f.getName(),detection.averageProcessingTime);
		}

		output.println();
		output.println(String.format("Final Summary: TP/(TP+FN) = %d / %d    FP = %d\n",totalTruePositive,totalExpected,totalFalsePositive));
		output.close();

		outputSpeed.println();
		outputSpeed.close();
	}

	public static void main(String[] args) throws IOException {
		DetectPolygonRegression app = new DetectPolygonRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
