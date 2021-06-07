package boofcv.metrics.point;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

import static boofcv.metrics.point.BatchEvaluateSummaryAndTime.pathToData;

/**
 * @author Peter Abeles
 */
public class EvaluateTrackerRuntime<T extends ImageGray<T>> implements Serializable {

	public double meanTimeMS;
	EvaluatedAlgorithm alg;
	String inputPath;
	Class<T> imageType;

	public EvaluateTrackerRuntime(EvaluatedAlgorithm alg,Class<T> imageType,String inputPath) {
		this.alg = alg;
		this.imageType = imageType;
		this.inputPath = inputPath;
	}

	public double evaluate() {

		SimpleImageSequence<T> sequence =
				DefaultMediaManager.INSTANCE.openVideo(inputPath, ImageType.single(imageType));

		FactoryEvaluationTrackers<T> factory = new FactoryEvaluationTrackers<T>(sequence.getImageType().getDataType().getDataType());
		EvaluationTracker<T> tracker = factory.create(alg);

		long totalRuntime = 0;
		int totalFrames = 0;

		while( sequence.hasNext() ) {

			T image = sequence.next();

			long before = System.nanoTime();
			tracker.track(image);
			long after = System.nanoTime();

			totalRuntime += after-before;
			totalFrames++;
//			System.out.println("Frame: " + totalFrames + "  tracks " + tracker.getCurrent().size());
		}
		meanTimeMS = (1e-6*totalRuntime)/totalFrames;

		return meanTimeMS;
	}

	public double evaluateExec() {

		String classPath = System.getProperty("java.class.path");
		String app = System.getProperty("java.home")+"/bin/java";

		String[] params = new String[10];
		params[0] = app;
		params[1] = "-server";
		params[2] = "-Xms200M";
		params[3] = "-Xmx500M";
		params[4] = "-classpath";
		params[5] = classPath;
		params[6] = EvaluateTrackerRuntime.class.getCanonicalName();
		params[7] = alg.name();
		params[8] = inputPath;
		params[9] = imageType.getName();

		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(params);

			if( System.in == pr.getInputStream() ) {
				System.out.println("Egads");
			}

			BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

			// print the output from the slave
			return monitorSlave(pr, input, error);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Prints out the standard out and error from the slave and checks its health. Exits if
	 * the slave has finished or is declared frozen.
	 */
	private double monitorSlave(Process pr, BufferedReader input, BufferedReader error)
			throws IOException, InterruptedException {

		// flush the input buffer
//		System.in.skip(System.in.available());

		String output = "";

		for(;;) {
			printError(error);

			while( input.ready() ) {
				output += input.readLine();
			}

			try {
				// exit value throws an exception is the process has yet to stop
				pr.exitValue();
				break;
			} catch( IllegalThreadStateException e) {
			}
			Thread.sleep(50);
		}

		while( input.ready() ) {
			output += input.readLine();
		}
//		System.out.println("slave output = "+output);

		return Double.parseDouble(output);
	}

	private void printError(BufferedReader error) throws IOException {
		while( error.ready() ) {
			int val = error.read();
			if( val < 0 ) break;

			System.out.print(Character.toChars(val));
		}
	}

	public static void main( String args[] ) throws ClassNotFoundException, InterruptedException {

		if( args.length > 0 ) {
//			System.out.println("Inside slave!");
			EvaluatedAlgorithm alg = EvaluatedAlgorithm.valueOf(args[0]);
			String inputPath = args[1];
			Class imageType = Class.forName(args[2]);

			EvaluateTrackerRuntime app = new EvaluateTrackerRuntime(alg,imageType,inputPath);
			double meanTimeMS = app.evaluate();
			System.out.println(meanTimeMS);
			return;
		}

		Class imageType = GrayF32.class;

		String dirs[] = new String[]{"bricks","carpet"};
		String dataSets[] = new String[]{"skew","rotate","move_out","move_in"};

		System.out.print("scenario");
		for( EvaluatedAlgorithm alg : EvaluatedAlgorithm.values() ) {
			System.out.print(" "+alg);
		}
		System.out.println();

		for( String dir : dirs ) {
			for( String dataSet : dataSets ) {
				System.out.print(dir+"_"+dataSet);
				for( EvaluatedAlgorithm alg : EvaluatedAlgorithm.values() ) {
					String inputPath = pathToData+dir+"/"+dataSet+"_undistorted.mjpeg";

					EvaluateTrackerRuntime app = new EvaluateTrackerRuntime(alg,imageType,inputPath);
//					double meanTimeMS = app.evaluate();
					double meanTimeMS = app.evaluateExec();
					System.out.printf(" %.2f", meanTimeMS);
				}
				System.out.println();
			}
		}
	}
}
