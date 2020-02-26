package boofcv.metrics;

import boofcv.common.misc.PointFileCodec;
import boofcv.io.UtilIO;
import georegression.struct.point.Point2D_F64;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static boofcv.metrics.FiducialCommon.parseDetections;
import static boofcv.metrics.FiducialCommon.parseLandmarks;

/**
 * Uses previously computed transforms from fiducial to camera to reproject corner points back onto the image.
 * It then matches these up with hand selected corners and see if there are matches.  Errors and types of
 * mistakes are accumulated and printed out.
 *
 * @author Peter Abeles
 */
public class EvaluateFiducialToCamera extends BaseEvaluateFiducialToCamera {

	@Override
	public void evaluate( File resultsDirectory , File dataSetDir ) {
		initializeEvaluate(dataSetDir);

		List<String> results = UtilIO.listByPrefix(resultsDirectory.getAbsolutePath(),null,"csv");
		Collections.sort(results);

		outputResults.println("# "+dataSetDir.getName());
		outputResults.printf("# maxPixelError = %.2f\n",scaledMaxPixelError);

		if( !justSummary )
			outputResults.println("# (file) (detected ID) (matched id) (out of order) (match pixel mean error)");


		List<FiducialCommon.Landmarks> landmarks = parseLandmarks(new File(dataSetDir,"landmarks.txt"));

		resetStatistics();
		for (int i = 0; i < results.size(); i++) {
			String resultPath = results.get(i);
			String name = new File(resultPath).getName();
			String nameTruth = name.substring(0,name.length()-3) + "txt";

			try {
				List<List<Point2D_F64>> truthFiducials = loadTruthSets(new File(dataSetDir, nameTruth), expected.length);

				List<FiducialCommon.Detected> detected = parseDetections(new File(resultPath));
				evaluate(name,detected,truthFiducials,landmarks);
			} catch( RuntimeException e ) {
				err.println("Error evaluating "+resultPath+"  in data set "+dataSetDir.getName());
				e.printStackTrace(err);
			}
		}

		Arrays.sort(errors.data,0,errors.size);

		double error50 = errors.size() == 0 ? 0 : errors.get( (int)(errors.size()*0.5));
		double error90 = errors.size() == 0 ? 0 : errors.get( (int)(errors.size()*0.9));
		double error100 = errors.size() == 0 ? 0 : errors.get( errors.size()-1);

		outputResults.println();
		outputResults.println("Summary:");
		outputResults.printf (" correct            : %4d / %4d\n",totalCorrect,totalExpected);
		outputResults.println(" wrong order        : " + totalWrongOrder);
		outputResults.println(" wrong ID           : " + totalWrongID);
		outputResults.println(" duplicates         : " + totalDuplicates);
		outputResults.println(" false positives    : " + totalFalsePositive);
		outputResults.println(" false negative     : " + totalFalseNegative);
		outputResults.println("Corner errors:");
		outputResults.println(" precision 50%         : " + error50);
		outputResults.println(" precision 90%         : " + error90);
		outputResults.println(" precision 100%        : " + error100);
	}

	public static List<List<Point2D_F64>> loadTruthSets( File fileTruth , int expectedFiducials ) {
		List<List<Point2D_F64>> truthFiducials;
		if( PointFileCodec.isPointSet(fileTruth)) {
			truthFiducials = PointFileCodec.loadSets(fileTruth);
		} else {
			List<Point2D_F64> list = PointFileCodec.load(fileTruth);
			truthFiducials = extractIndividual(list,expectedFiducials );
		}
		return truthFiducials;
	}

	public static void main(String[] args) {
		EvaluateFiducialToCamera app = new EvaluateFiducialToCamera();

//		app.setMaxPixelError(10);
//		app.initialize(new File("data/fiducials/image"));
//		app.evaluate(new File("tmp"),"motion_blur");
	}
}
