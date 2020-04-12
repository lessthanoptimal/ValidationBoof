package boofcv.metrics.stabilization;

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

	PrintStream outputRuntime = System.out;
	PrintStream outputMetrics = System.out;
	PrintStream err = System.err;

	StitchingFromMotion2D<T,?> stitcher;
	ImageType<T> imageType;

	public RuntimePerformanceVideoStabilization(StitchingFromMotion2D<T,?> stitcher,
												ImageType<T> imageType ) {
		this.stitcher = stitcher;
		this.imageType = imageType;
	}

	public void evaluate( List<File> videos ) {
		outputRuntime.println("# Video Name, fps");
		outputMetrics.println("# Video Name, total frames, total faults");

		for( File f : videos ) {
			SimpleImageSequence<T> sequence = DefaultMediaManager.INSTANCE.openVideo(f.getPath(),imageType);
			sequence.setLoop(false);

			stitcher.configure(sequence.getWidth(), sequence.getHeight(), null);
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

				outputMetrics.printf("%20s  %4d %4d\n",f.getName(),totalFrames,totalFaults);
				outputRuntime.printf("%20s  %f\n",f.getName(),fps);

			} catch( RuntimeException e ) {
				err.println("Caught fault on "+f.getPath());
				err.println(e);
				e.printStackTrace(err);
				err.println();
			}
		}
	}

	public void setOutputRuntime(PrintStream outputRuntime) {
		this.outputRuntime = outputRuntime;
	}

	public void setOutputMetrics(PrintStream outputMetrics) {
		this.outputMetrics = outputMetrics;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

}
