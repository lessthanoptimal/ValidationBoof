package boofcv.metrics;

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.GrowQueue_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Detects polygons inside an image and saves the results to a file
 *
 * @author Peter Abeles
 */
public class DetectLinesSaveToFile<T extends ImageGray<T>> {

	boolean thinDetector;
	String detectorName;
	int blurRadius;

	T gray;
	T blurred;

	Class<T> imageType;

	public final GrowQueue_F64 processingTimeMS = new GrowQueue_F64();

	public DetectLinesSaveToFile(boolean thinDetector, String detectorName , int blurRadius ,
								 Class<T> imageType ) {

		this.thinDetector = thinDetector;
		this.detectorName = detectorName;
		this.blurRadius = blurRadius;
		this.imageType = imageType;

		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);
		blurred = gray.createSameShape();
	}

	public void processDirectory( File inputDir , File outputDir ) {

		if( !inputDir.exists() )
			throw new RuntimeException("Input directory doesn't exist. "+inputDir.getPath());

		if( !outputDir.exists() )
			outputDir.mkdirs();

		List<File> files = Arrays.asList(inputDir.listFiles());
		Collections.sort(files);

		processingTimeMS.reset();
		for( File f : files ) {
			if( !f.isFile() || f.getName().endsWith("txt"))
				continue;

			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null )
				continue;

			String truthName = FilenameUtils.getBaseName(f.getName())+".txt";

			int expectedLines = UtilShapeDetector.loadTruthLineSegments(new File(f.getParent(),truthName)).size();

			String outputName = UtilShapeDetector.imageToDetectedName(f.getName());
			File outputFile = new File(outputDir,outputName);
			process(buffered,expectedLines,outputFile);
		}

	}

	private void process( BufferedImage buffered , int expectedLines, File outputFile ) {
		DetectLine<T> lineDetector = thinDetector ?
				FactoryLineDetector.createThin(detectorName,expectedLines,imageType) :
				FactoryLineDetector.createEdge(detectorName,expectedLines,imageType);

		ConvertBufferedImage.convertFrom(buffered, gray, true);

		// Blur image to improve detection
		GBlurImageOps.gaussian(gray,blurred,-1,blurRadius,null);

		lineDetector.detect(blurred);// warm up for speed
		lineDetector.detect(blurred);// warm up for speed
		lineDetector.detect(blurred);// warm up for speed
		long startNano = System.nanoTime();
		List<LineParametric2D_F32> lines = lineDetector.detect(blurred);
		long stopNano = System.nanoTime();

		UtilShapeDetector.saveResultsLines(lines,outputFile);

		processingTimeMS.add((stopNano-startNano)*1e-6);
	}

}
