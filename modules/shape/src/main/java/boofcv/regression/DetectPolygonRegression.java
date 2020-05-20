package boofcv.regression;

import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.common.*;
import boofcv.metrics.DetectPolygonsSaveToFile;
import boofcv.metrics.EvaluatePolygonDetector;
import boofcv.metrics.FactoryBinaryPolygon;
import boofcv.struct.image.ImageDataType;
import org.ddogleg.struct.GrowQueue_F64;

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

	RuntimeSummary runtime;

	public DetectPolygonRegression() {
		super(BoofRegressionConstants.TYPE_SHAPE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		runtime = new RuntimeSummary();
		runtime.out = new PrintStream(new File(directoryRuntime,"RUN_PolygonDetector.txt"));
		BoofRegressionConstants.printGenerator(runtime.out, getClass());
		runtime.out.println("# Elapsed time in milliseconds");
		runtime.out.println();

		process("BinaryGlobal", false, new FactoryBinaryPolygon(imageType));
		process("BinaryLocal", true, new FactoryBinaryPolygon(imageType));

		runtime.printSummary();
		runtime.out.close();
	}

	private void process(String name, boolean localBinary , FactoryObject<DetectPolygonBinaryGrayRefine> factory)
			throws IOException {

		String outputAccuracyName = "ACC_PolygonDetector_"+name+".txt";

		EvaluatePolygonDetector evaluator = new EvaluatePolygonDetector();

		PrintStream outputAccuracy = new PrintStream(new File(directoryMetrics,outputAccuracyName));
		BoofRegressionConstants.printGenerator(outputAccuracy, getClass());
		evaluator.setOutputResults(outputAccuracy);

		GrowQueue_F64 summaryTimeMS = new GrowQueue_F64();
		runtime.out.println(name);
		runtime.printHeader(false);

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

			summaryTimeMS.addAll(detection.processingTimeMS);
			runtime.printStats(f.getName(),detection.processingTimeMS);
		}
		runtime.saveSummary(name,summaryTimeMS);
		runtime.out.println();

		outputAccuracy.println();
		outputAccuracy.println(String.format("Final Summary: TP/(TP+FN) = %d / %d    FP = %d\n",totalTruePositive,totalExpected,totalFalsePositive));
		outputAccuracy.close();

	}

	public static void main(String[] args)
			throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DetectPolygonRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
