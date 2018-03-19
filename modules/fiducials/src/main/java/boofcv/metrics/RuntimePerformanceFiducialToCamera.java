package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.common.BoofRegressionConstants;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.ImageBase;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class RuntimePerformanceFiducialToCamera< T extends ImageBase<T>> {

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	int numFrames = 300;

	BaseEstimateSquareFiducialToCamera<T> factory;


	public RuntimePerformanceFiducialToCamera(BaseEstimateSquareFiducialToCamera<T> factory) {
		this.factory = factory;
	}

	public void evaluate( File baseDirectory ) {
		outputResults.println("# directory, number of frames, FPS");

		List<File> files = new ArrayList<File>();
		List<File> baseFiles = BoofRegressionConstants.listAndSort(baseDirectory);
		for( File f : baseFiles ) {
			if( f.isDirectory() && (f.getName().equals("standard") || f.getName().equals("static"))) {
				files.addAll(BoofRegressionConstants.listAndSort(f));
			}
		}

		Collections.sort(files);

		for( File f : files ) {
			if( f.isDirectory() ) {
				evaluateScenario(f);
			}
		}
	}

	private void evaluateScenario( File inputDirectory ) {

		List<T> frames = new ArrayList<T>();

		CameraPinholeRadial intrinsic = FiducialCommon.parseIntrinsic(new File(inputDirectory,"intrinsic.txt"));

		FiducialDetector<T> detector = factory.createDetector(inputDirectory);
		detector.setLensDistortion(new LensDistortionRadialTangential(intrinsic),intrinsic.width,intrinsic.height);

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

		String shortPath = new File(inputDirectory.getParentFile(),inputDirectory.getName()).toString();
		outputResults.printf("%s %d %7.3f\n",shortPath,numFrames,fps);
	}

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

}
