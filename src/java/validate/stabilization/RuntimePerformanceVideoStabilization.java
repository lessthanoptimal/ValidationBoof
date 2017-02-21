package validate.stabilization;

import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class RuntimePerformanceVideoStabilization< T extends ImageBase<T>> {

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	StitchingFromMotion2D<T,?> stitcher;
	ImageType<T> imageType;

	public RuntimePerformanceVideoStabilization(StitchingFromMotion2D<T,?> stitcher,
												ImageType<T> imageType ) {
		this.stitcher = stitcher;
		this.imageType = imageType;
	}

	public void evaluate( List<File> videos ) {
		outputResults.println("# Video Name, total frames, total faults, FPS");

		for( File f : videos ) {
			SimpleImageSequence<T> sequence = DefaultMediaManager.INSTANCE.openVideo(f.getPath(),imageType);
			sequence.setLoop(false);

			stitcher.configure(sequence.getNextWidth(), sequence.getNextHeight(), null);
			stitcher.reset();

			try {
				long timeStart = System.currentTimeMillis();
				int totalFrames = 0;
				int totalFaults = 0;

				while (sequence.hasNext()) {
					totalFrames++;
					T frame = sequence.next();
					if (!stitcher.process(frame)) {
						stitcher.reset();
						totalFaults++;
					}
				}

				double seconds = (System.currentTimeMillis() - timeStart) / 1000.0;
				double fps = totalFrames / seconds;

				outputResults.printf("%20s  %4d %4d %f\b",f.getName(),totalFrames,totalFaults,fps);
			} catch( RuntimeException e ) {
				err.println("Caught fault on "+f.getPath());
				err.println(e);
				e.printStackTrace(err);
				err.println();
			}
		}
	}

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

}
