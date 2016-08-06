package regression;

import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.struct.image.ImageDataType;
import validate.FactoryObject;
import validate.shape.DetectEllipseSaveToFile;
import validate.shape.EvaluateEllipseDetector;
import validate.shape.FactoryBinaryEllipse;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectEllipseRegression extends BaseTextFileRegression {

	File workDirectory = new File("./tmp");
	File baseDataSetDirectory = new File("data/shape/ellipse");

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		process("EllipseGlobal", false, new FactoryBinaryEllipse(imageType));
		process("EllipseLocal", true, new FactoryBinaryEllipse(imageType));
	}

	private void process(String name, boolean localBinary , FactoryObject<BinaryEllipseDetector> factory)
			throws IOException {

		String outputName = "ShapeDetector_"+name+".txt";


		EvaluateEllipseDetector evaluator = new EvaluateEllipseDetector();

		PrintStream output = new PrintStream(new File(directory,outputName));
		evaluator.setOutputResults(output);

		List<File> files = Arrays.asList(baseDataSetDirectory.listFiles());
		Collections.sort(files);

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			output.println("# Data Set = "+f.getName());

			factory.configure(new File(f,"detector.txt"));

			DetectEllipseSaveToFile detection = new DetectEllipseSaveToFile(factory.newInstance(),localBinary);

			detection.processDirectory(f, workDirectory);
			evaluator.evaluate(f, workDirectory);
			output.println();
		}

		output.close();
	}

	public static void main(String[] args) throws IOException {
		DetectEllipseRegression app = new DetectEllipseRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
