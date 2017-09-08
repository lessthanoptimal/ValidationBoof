package validate.shape;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigEllipseDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;
import validate.misc.EllipseFileCodec;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Detects polygons inside an image and saves the results to a file
 *
 * @author Peter Abeles
 */
public class DetectEllipseSaveToFile<T extends ImageGray<T>> {

	BinaryEllipseDetector<T> detector;
	InputToBinary<T> inputToBinary;

	T gray;
	GrayU8 binary = new GrayU8(1,1);

	public DetectEllipseSaveToFile(BinaryEllipseDetector<T> detector , boolean binaryLocal) {

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

		FastQueue<EllipseRotated_F64> found = detector.getFoundEllipses();
//		System.out.println("Found = "+found.size);

		EllipseFileCodec.save(outputFile.getPath(),"Detected ellipses",found.toList());
	}

	public static void main(String[] args) {

		Class imageType = GrayU8.class;
		ConfigEllipseDetector config = UtilShapeDetector.configureEllipse(true);
		BinaryEllipseDetector detector = FactoryShapeDetector.ellipse(config,imageType);

		DetectEllipseSaveToFile app = new DetectEllipseSaveToFile(detector, false);

		app.processDirectory(new File("data/fiducials/acircle_grid/standard/cardboard"),new File("./tmp"));
	}
}
