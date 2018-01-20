package validate.shape;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.shapes.Polygon2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Detects polygons inside an image and saves the results to a file
 *
 * @author Peter Abeles
 */
public class DetectPolygonsSaveToFile<T extends ImageGray<T>> {

	DetectPolygonBinaryGrayRefine<T> detector;
	InputToBinary<T> inputToBinary;

	T gray;
	GrayU8 binary = new GrayU8(1,1);

	// average amount of time in milliseconds to process each image in the directory
	public double averageProcessingTime;

	public DetectPolygonsSaveToFile( DetectPolygonBinaryGrayRefine<T> detector , boolean binaryLocal) {

		this.detector = detector;

		ConfigThreshold config;
		if( binaryLocal ) {
			config = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,10);
		} else {
			config = ConfigThreshold.global(ThresholdType.GLOBAL_OTSU);
		}
		inputToBinary = FactoryThresholdBinary.threshold(config,detector.getInputType());
		gray = GeneralizedImageOps.createSingleBand(detector.getInputType(), 1, 1);
	}

	public void processDirectory( File inputDir , File outputDir ) {

		if( !outputDir.exists() )
			outputDir.mkdirs();


		averageProcessingTime = 0;
		int total = 0;
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
			total++;
		}
		averageProcessingTime /= total;
	}

	private void process( BufferedImage buffered , File outputFile ) {

		gray.reshape(buffered.getWidth(),buffered.getHeight());
		binary.reshape(buffered.getWidth(), buffered.getHeight());

		ConvertBufferedImage.convertFrom(buffered, gray, true);

		inputToBinary.process(gray, binary);

		long startNano = System.nanoTime();
		detector.process(gray, binary);
		detector.refineAll();
		List<Polygon2D_F64> found = detector.getPolygons(null,null);
		long stopNano = System.nanoTime();
		averageProcessingTime += (stopNano-startNano)/1e6;

//		System.out.println("Found = "+found.size);

		UtilShapeDetector.saveResults(found,outputFile);
	}

	public static void main(String[] args) {

		File file = new File("data/shape/concave/detector.txt");
		Class imageType = GrayU8.class;
		ConfigPolygonDetector config = UtilShapeDetector.configurePolygon(file);
		DetectPolygonBinaryGrayRefine detector = FactoryShapeDetector.polygon(config,imageType);

		DetectPolygonsSaveToFile app = new DetectPolygonsSaveToFile(detector, false);

		app.processDirectory(new File("data/shape/concave/"),new File("./tmp"));
	}
}
