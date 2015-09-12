package validate.fiducial;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class RuntimePerformanceFiducialToCamera< T extends ImageBase> {

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	int numFrames = 300;

	FiducialDetector<T> detector;

	public RuntimePerformanceFiducialToCamera(FiducialDetector<T> detector) {
		this.detector = detector;
	}

	public void evaluate( File inputDirectory ) {
		File[] files = inputDirectory.listFiles();

		for( File f : files ) {
			if( f.isDirectory() ) {
				evaluateScenario(f);
			}
		}
	}

	private void evaluateScenario( File inputDirectory ) {

		List<T> frames = new ArrayList<T>();

		IntrinsicParameters intrinsic = FiducialCommon.parseIntrinsic(new File(inputDirectory,"intrinsic.txt"));
		detector.setIntrinsic(intrinsic);

		File[] files = inputDirectory.listFiles();
		for( File f : files ) {
			BufferedImage image = UtilImageIO.loadImage(f.getPath());
			if( image == null )
				continue;

			T frame = ConvertBufferedImage.convertFrom(image,true,detector.getInputType());
			frames.add( frame );
		}

		long startTime = System.currentTimeMillis();
		for (int trial = 0; trial < numFrames; trial++) {
			detector.detect(frames.get(trial % frames.size()));
		}
		long endTime = System.currentTimeMillis();

		double fps = numFrames/((endTime-startTime)/1000.0);

		outputResults.printf("%s %d %7.3f\n",inputDirectory.getPath(),numFrames,fps);
	}

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

	public static void main(String[] args) {
		FiducialDetector<ImageUInt8> detector =
				FactoryFiducial.squareImageRobust(new ConfigFiducialImage(), 20, ImageUInt8.class);

		RuntimePerformanceFiducialToCamera<ImageUInt8> benchmark =
				new RuntimePerformanceFiducialToCamera<ImageUInt8>(detector);

		benchmark.evaluate(new File("data/fiducials/image"));
	}
}
