package boofcv.regression;

import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.common.*;
import boofcv.metrics.DetectPolylineSaveToFile;
import boofcv.metrics.EvaluatePolylineDetector;
import boofcv.metrics.FactoryPolylineSplitMerge;
import boofcv.metrics.FactoryPolylineSplitMergeOld;
import boofcv.struct.image.ImageDataType;
import org.ddogleg.struct.GrowQueue_F64;

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

	RuntimeSummary runtime;

	public FitPolylineRegression() {
		super(BoofRegressionConstants.TYPE_SHAPE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		runtime = new RuntimeSummary();
		runtime.out = new PrintStream(new File(directoryRuntime,"RUN_Polyline.txt"));
		BoofRegressionConstants.printGenerator(runtime.out, getClass());
		runtime.out.println("# Elapsed time in milliseconds");
		runtime.out.println();

		process("SplitMerge_Global", false, new FactoryPolylineSplitMerge(),imageType);
		process("SplitMerge_Local", true, new FactoryPolylineSplitMerge(),imageType);
		process("SplitMergeOld_Global", false, new FactoryPolylineSplitMergeOld(),imageType);
		process("SplitMergeOld_Local", true, new FactoryPolylineSplitMergeOld(),imageType);

		runtime.printSummary();
		runtime.out.close();
	}

	private void process(String name, boolean localBinary , FactoryObject<PointsToPolyline> factory, Class imageType )
			throws IOException {

		String outputAccName = "ACC_Polyline_"+name+".txt";

		EvaluatePolylineDetector evaluator = new EvaluatePolylineDetector();

		PrintStream outputAccuracy = new PrintStream(new File(directoryMetrics,outputAccName));
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

			DetectPolylineSaveToFile detection = new DetectPolylineSaveToFile(factory.newInstance(),localBinary,
					imageType);

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

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{FitPolylineRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
