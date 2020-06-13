package boofcv.metrics.stabilization;

import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.common.RuntimeSummary;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.GrowQueue_F64;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class RuntimePerformanceVideoStabilization< T extends ImageBase<T>> {

	RuntimeSummary outputRuntime = new RuntimeSummary();
	PrintStream outputMetrics = System.out;
	PrintStream err = System.err;

	StitchingFromMotion2D<T,?> stitcher;
	ImageType<T> imageType;

	public final GrowQueue_F64 timesMS = new GrowQueue_F64();
	public final GrowQueue_F64 summaryTimesMS = new GrowQueue_F64();

	public RuntimePerformanceVideoStabilization(StitchingFromMotion2D<T,?> stitcher,
												ImageType<T> imageType ) {
		this.stitcher = stitcher;
		this.imageType = imageType;
	}

	public void evaluate( List<File> videos ) {

		summaryTimesMS.reset();
		outputRuntime.printUnitsRow(false);
		outputMetrics.println("# Video Name, total frames, total faults");

		for( File f : videos ) {
			SimpleImageSequence<T> sequence = DefaultMediaManager.INSTANCE.openVideo(f.getPath(),imageType);
			sequence.setLoop(false);

			timesMS.reset();

			stitcher.configure(sequence.getWidth(), sequence.getHeight(), null);
			stitcher.reset();

			try {
				int totalFrames = 0;
				int totalFaults = 0;

				while (sequence.hasNext()) {
					totalFrames++;
					T frame = sequence.next();

					long time0 = System.nanoTime();
					if (!stitcher.process(frame)) {
						stitcher.reset();
						totalFaults++;
					}
					long time1 = System.nanoTime();
					timesMS.add((time1-time0)*1e-6);
				}

				summaryTimesMS.addAll(timesMS);
				outputRuntime.printStatsRow(f.getName(),timesMS);
				outputMetrics.printf("%20s  %4d %4d\n",f.getName(),totalFrames,totalFaults);

			} catch( RuntimeException e ) {
				err.println("Caught fault on "+f.getPath());
				err.println(e);
				e.printStackTrace(err);
				err.println();
			}
		}
	}

	public void setOutputRuntime(RuntimeSummary outputRuntime) {
		this.outputRuntime = outputRuntime;
	}

	public void setOutputMetrics(PrintStream outputMetrics) {
		this.outputMetrics = outputMetrics;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

}
