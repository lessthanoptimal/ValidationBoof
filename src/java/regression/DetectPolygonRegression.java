package regression;

import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.struct.image.ImageDataType;
import validate.FactoryObject;
import validate.shape.DetectPolygonsSaveToFile;
import validate.shape.EvaluatePolygonDetector;
import validate.shape.UtilShapeDetector;

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

		FactoryObject factory = new FactoryObject() { @Override public Object newInstance()
			{return UtilShapeDetector.createPolygonLine(imageType);}};
		process("PolygonLine", factory);

		factory = new FactoryObject() { @Override public Object newInstance()
		{return UtilShapeDetector.createPolygonCorner(imageType);}};
		process("PolygonCorner", factory);
	}

	private void process(String name, FactoryObject<BinaryPolygonConvexDetector> factory) throws IOException {

		String outputName = "ShapeDetector_"+name+".txt";

		DetectPolygonsSaveToFile detection = new DetectPolygonsSaveToFile(factory.newInstance());
		EvaluatePolygonDetector evaluator = new EvaluatePolygonDetector();

		PrintStream output = new PrintStream(new File(directory,outputName));
		evaluator.setOutputResults(output);

		List<File> files = Arrays.asList(baseDataSetDirectory.listFiles());
		Collections.sort(files);

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			output.println("# Data Set = "+f.getName());
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
