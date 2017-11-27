package regression;

import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.struct.image.ImageDataType;
import validate.FactoryObject;
import validate.shape.DetectPolylineSaveToFile;
import validate.shape.EvaluatePolylineDetector;
import validate.shape.FactoryPolylineSplitMerge;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FitPolylineRegression extends BaseTextFileRegression {

	File workDirectory = new File("./tmp");
	File baseDataSetDirectory = new File("data/shape/polygon");

	String infoString;

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		process("SplitMerge_Global", false, new FactoryPolylineSplitMerge(),imageType);
		process("SplitMerge_Local", true, new FactoryPolylineSplitMerge(),imageType);
	}

	private void process(String name, boolean localBinary , FactoryObject<PointsToPolyline> factory, Class imageType )
			throws IOException {

		String outputName = "Polyline_"+name+".txt";


		EvaluatePolylineDetector evaluator = new EvaluatePolylineDetector();

		PrintStream output = new PrintStream(new File(directory,outputName));
		evaluator.setOutputResults(output);

		List<File> files = Arrays.asList(baseDataSetDirectory.listFiles());
		Collections.sort(files);

		int totalTruePositive = 0;
		int totalExpected = 0;
		int totalFalsePositive = 0;

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			output.println("# Data Set = "+f.getName());

			factory.configure(new File(f,"detector.txt"));

			DetectPolylineSaveToFile detection = new DetectPolylineSaveToFile(factory.newInstance(),localBinary,
					imageType);

			detection.processDirectory(f, workDirectory);
			evaluator.evaluate(f, workDirectory);
			output.println();

			totalTruePositive += evaluator.summaryTruePositive;
			totalExpected += evaluator.summaryExpected;
			totalFalsePositive += evaluator.summaryFalsePositive;
		}

		output.println();
		output.println(String.format("Final Summary: TP/(TP+FN) = %d / %d    FP = %d\n",totalTruePositive,totalExpected,totalFalsePositive));

		output.close();
	}

	public static void main(String[] args) throws IOException {
		FitPolylineRegression app = new FitPolylineRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
