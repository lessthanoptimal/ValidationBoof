package boofcv.regression;

import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FactoryObject;
import boofcv.common.ImageRegression;
import boofcv.metrics.DetectEllipseSaveToFile;
import boofcv.metrics.EvaluateEllipseDetector;
import boofcv.metrics.FactoryBinaryEllipse;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectEllipseRegression extends BaseRegression implements ImageRegression {

	File workDirectory = new File("./tmp");
	File baseDataSetDirectory = new File("data/shape/ellipse");

	PrintStream outputRuntime;

	public DetectEllipseRegression() {
		super(BoofRegressionConstants.TYPE_SHAPE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		outputRuntime = new PrintStream(new File(directory,"RUN_EllipseDetector.txt"));
		BoofRegressionConstants.printGenerator(outputRuntime, getClass());
		outputRuntime.println("# Runtime for black ellipse detectors");

		process("Global", false, new FactoryBinaryEllipse(true,imageType));
		process("Local", true, new FactoryBinaryEllipse(true,imageType));
		process("LocalPixel", true, new FactoryBinaryEllipse(false,imageType));

		outputRuntime.close();
	}

	private void process(String name, boolean localBinary , FactoryObject<BinaryEllipseDetector> factory)
			throws IOException {

		String outputName = "ACC_EllipseDetector_"+name+".txt";

		EvaluateEllipseDetector evaluator = new EvaluateEllipseDetector();

		PrintStream outputAccuracy = new PrintStream(new File(directory,outputName));
		BoofRegressionConstants.printGenerator(outputAccuracy, getClass());
		evaluator.setOutputResults(outputAccuracy);

		List<File> files = BoofRegressionConstants.listAndSort(baseDataSetDirectory);

		outputRuntime.println("Detector "+name);

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			outputAccuracy.println("# Data Set = "+f.getName());

			factory.configure(new File(f,"detector.txt"));

			DetectEllipseSaveToFile detection = new DetectEllipseSaveToFile(factory.newInstance(),localBinary);

			detection.processDirectory(f, workDirectory);
			evaluator.evaluate(f, workDirectory);
			outputAccuracy.println();

			outputRuntime.printf("%20s %9.4f (ms)\n",f.getName(),detection.averageProcessingTime);
		}
		outputRuntime.println();

		outputAccuracy.close();
	}

	public static void main(String[] args) throws IOException {
		DetectEllipseRegression app = new DetectEllipseRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
