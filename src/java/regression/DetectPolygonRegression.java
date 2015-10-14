package regression;

import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.struct.image.ImageDataType;
import validate.FactoryObject;
import validate.shape.DetectPolygonsSaveToFile;
import validate.shape.EvaluatePolygonDetector;
import validate.shape.FactoryBinaryPolygon;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectPolygonRegression extends BaseTextFileRegression {

	File workDirectory = new File("./tmp");
	File baseDataSetDirectory = new File("data/shape");

	String infoString;

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		process("PolygonLine", new FactoryBinaryPolygon(true,imageType));
		process("PolygonCorner", new FactoryBinaryPolygon(false,imageType));
	}

	private void process(String name, FactoryObject<BinaryPolygonDetector> factory) throws IOException {

		String outputName = "ShapeDetector_"+name+".txt";


		EvaluatePolygonDetector evaluator = new EvaluatePolygonDetector();

		PrintStream output = new PrintStream(new File(directory,outputName));
		evaluator.setOutputResults(output);

		List<File> files = Arrays.asList(baseDataSetDirectory.listFiles());
		Collections.sort(files);

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			output.println("# Data Set = "+f.getName());

			factory.configure(new File(f,"detector.txt"));

			DetectPolygonsSaveToFile detection = new DetectPolygonsSaveToFile(factory.newInstance());

			detection.processDirectory(f, workDirectory);
			evaluator.evaluate(f, workDirectory);
			output.println();
		}

		output.close();
	}

	public static void main(String[] args) throws IOException {
		DetectPolygonRegression app = new DetectPolygonRegression();
		app.setOutputDirectory(".");
		app.process(ImageDataType.F32);
	}
}
