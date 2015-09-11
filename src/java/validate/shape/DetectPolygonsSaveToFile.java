package validate.shape;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Detects polygons inside an image and saves the results to a file
 *
 * @author Peter Abeles
 */
public class DetectPolygonsSaveToFile<T extends ImageSingleBand> {

	BinaryPolygonConvexDetector<T> detector;
	InputToBinary<T> inputToBinary;

	T gray;
	ImageUInt8 binary = new ImageUInt8(1,1);

	public DetectPolygonsSaveToFile( BinaryPolygonConvexDetector<T> detector) {

		this.detector = detector;

		inputToBinary = FactoryThresholdBinary.globalOtsu(0, 255, true, detector.getInputType());
//		inputToBinary = FactoryThresholdBinary.adaptiveSquare(10,0,true,detector.getInputType());

		gray = GeneralizedImageOps.createSingleBand(detector.getInputType(), 1, 1);
	}

	public void processDirectory( File inputDir , File outputDir ) {

		if( !outputDir.exists() )
			outputDir.mkdirs();

		File files[] = inputDir.listFiles();

		for( File f : files ) {
			if( !f.isFile() || f.getName().endsWith("txt"))
				continue;

			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null )
				continue;

			String name = UtilShapeDetector.imageToDetectedName(f.getName());
			File outputFile = new File(outputDir,name);
			process(buffered,outputFile);
		}
	}

	private void process( BufferedImage buffered , File outputFile ) {

		gray.reshape(buffered.getWidth(),buffered.getHeight());
		binary.reshape(buffered.getWidth(), buffered.getHeight());

		ConvertBufferedImage.convertFrom(buffered, gray, true);

		inputToBinary.process(gray, binary);
		detector.process(gray, binary);

		FastQueue<Polygon2D_F64> found = detector.getFound();
//		System.out.println("Found = "+found.size);

		UtilShapeDetector.saveResults(found.toList(),outputFile);
	}

	public static void main(String[] args) {

		Class imageType = ImageUInt8.class;
		BinaryPolygonConvexDetector detector = UtilShapeDetector.createPolygonLine(imageType);

		DetectPolygonsSaveToFile app = new DetectPolygonsSaveToFile(detector);

		app.processDirectory(new File("data/shape/set01"),new File("./tmp"));
	}
}
