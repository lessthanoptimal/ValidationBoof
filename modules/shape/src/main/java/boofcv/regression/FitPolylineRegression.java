package boofcv.regression;

import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.common.*;
import boofcv.metrics.DetectPolylineSaveToFile;
import boofcv.metrics.EvaluatePolylineDetector;
import boofcv.metrics.FactoryPolylineSplitMerge;
import boofcv.metrics.FactoryPolylineSplitMergeOld;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FitPolylineRegression extends BaseRegression implements ImageRegression {

	File workDirectory = BoofRegressionConstants.tempDir();
	File baseDataSetDirectory = new File("data/shape/polygon");

	PrintStream outputSpeed;

	public FitPolylineRegression() {
		super(BoofRegressionConstants.TYPE_SHAPE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		outputSpeed = new PrintStream(new File(directory, "RUN_PolylineSpeed.txt"));
		BoofRegressionConstants.printGenerator(outputSpeed, getClass());

		process("SplitMerge_Global", false, new FactoryPolylineSplitMerge(),imageType);
		process("SplitMerge_Local", true, new FactoryPolylineSplitMerge(),imageType);
		process("SplitMergeOld_Global", false, new FactoryPolylineSplitMergeOld(),imageType);
		process("SplitMergeOld_Local", true, new FactoryPolylineSplitMergeOld(),imageType);

		outputSpeed.close();
	}

	private void process(String name, boolean localBinary , FactoryObject<PointsToPolyline> factory, Class imageType )
			throws IOException {

		String outputAccName = "ACC_Polyline_"+name+".txt";

		EvaluatePolylineDetector evaluator = new EvaluatePolylineDetector();

		PrintStream outputAccuracy = new PrintStream(new File(directory,outputAccName));
		BoofRegressionConstants.printGenerator(outputAccuracy, getClass());
		evaluator.setOutputResults(outputAccuracy);

		outputSpeed.println("# "+name);
		outputSpeed.println("# data set, average speed (ms)");
		List<File> files = BoofRegressionConstants.listAndSort(baseDataSetDirectory);

		int totalTruePositive = 0;
		int totalExpected = 0;
		int totalFalsePositive = 0;

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			outputAccuracy.println("# Data Set = "+f.getName());

			factory.configure(new File(f,"detector.txt"));

			DetectPolylineSaveToFile detection = new DetectPolylineSaveToFile(factory.newInstance(),localBinary,
					imageType);

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

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{FitPolylineRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
