package validate.fiducial.benchmark;

import georegression.struct.se.Se3_F64;
import validate.fiducial.EvaluateFiducialToCamera;
import validate.fiducial.EvaluateStaticFiducialSequence;
import validate.misc.ParseHelper;

import java.io.*;

/**
 * Processes pre-computed results from a thirdparty library and outputs metrics
 *
 * @author Peter Abeles
 */
public class BenchmarkFiducialImageLibrary {
	File baseFiducial = new File("data/fiducials/image");
	String dataSetsStandard[] = new String[]{"rotation","distance_straight","distance_angle","hard"};
	String dataSetsBlur[] = new String[]{"motion_blur"};
	String dataSetsStatic[] = new String[]{"static_scene","static_front_close","static_front_far"};

	Se3_F64 libToStandard;
	PrintStream errorLog = System.err;
	PrintStream outputFile;

	public void process( String pathToResults ) throws IOException {

		outputFile = new PrintStream("results.txt");

		File fileTransform = new File(pathToResults,"libToStandard.txt");
		if( !fileTransform.exists() )
			throw new RuntimeException("A transform from the libraries coordinate into standard must be provided");
		else {
			BufferedReader reader = new BufferedReader(new FileReader(fileTransform));
			libToStandard = ParseHelper.parseRigidBody(reader.readLine(), reader);
		}

		for( String dataset : dataSetsStandard ) {
			computeStandardMetrics(dataset,new File(pathToResults,dataset),5);
		}
		for( String dataset : dataSetsBlur ) {
			computeStandardMetrics(dataset,new File(pathToResults,dataset),10);
		}

		for( String dataset : dataSetsStatic ) {
			computeStaticMetrics(dataset, new File(pathToResults, dataset),5);
		}
	}

	private void computeStandardMetrics(String dataset,  File resultsDir, double maxPixelError )
			throws IOException
	{

		EvaluateFiducialToCamera evaluate = new EvaluateFiducialToCamera();
		evaluate.setJustSummary(true);
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.initialize(baseFiducial);
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(outputFile);
		evaluate.setTransformToStandard(libToStandard);

		evaluate.evaluate(resultsDir, dataset);
		outputFile.println();
	}

	private void computeStaticMetrics(String dataset,  File resultsDir, double maxPixelError  )
			throws IOException
	{
		EvaluateStaticFiducialSequence evaluate = new EvaluateStaticFiducialSequence();
		evaluate.setJustSummary(true);
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.initialize(baseFiducial);
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(outputFile);
		evaluate.setTransformToStandard(libToStandard);

		evaluate.evaluate(resultsDir, dataset);
		outputFile.println();
	}

	public static void main(String[] args) throws IOException {
		BenchmarkFiducialImageLibrary app = new BenchmarkFiducialImageLibrary();

		app.process("output");
	}
}
