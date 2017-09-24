package validate.fiducial;

import boofcv.io.UtilIO;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.GrowQueue_F64;
import validate.misc.PointFileCodec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static validate.fiducial.FiducialCommon.parseDetections;
import static validate.fiducial.FiducialCommon.parseLandmarks;

/**
 * @author Peter Abeles
 */
public class EvaluateStaticFiducialSequence extends BaseEvaluateFiducialToCamera {

	Vector3D_F64 normalsPrev[];
	Se3_F64 posePrev[];

	public EvaluateStaticFiducialSequence() {
		maxPixelError = 10;
	}

	@Override
	public void evaluate( File resultsDirectory , File dataSetDir )
	{
		initializeEvaluate(dataSetDir);

		List<String> results = UtilIO.directoryList(resultsDirectory.getAbsolutePath(), "csv");
		Collections.sort(results);

		outputResults.println("# Data Set = " + dataSetDir.getName());
		outputResults.println("# maxPixelError = "+maxPixelError);

		if( !justSummary )
			outputResults.println("# (file) (detected ID) (matched id) (out of order) (match pixel error)");

		// list of all detections for each fiducial.  used to compute precision
		List<List<Point2D_F64>> allDetections[] = new ArrayList[expected.length];
		for (int i = 0; i < allDetections.length; i++) {
			allDetections[i] = new ArrayList<>();
		}

		// hand selected corners only in the first image
		String nameFirst = new File(results.get(0)).getName();
		String nameTruth = nameFirst.substring(0,nameFirst.length()-3) + "txt";
		File fileTruth = new File(dataSetDir, nameTruth);
		List<Point2D_F64> truthCorners;

		if( fileTruth.exists() ) {
			// if hand selected truth exists use it
			truthCorners = PointFileCodec.load(fileTruth);
		} else {
			// create it from the first image
			truthCorners = new ArrayList<>();

			List<FiducialCommon.Detected> detected = parseDetections(new File(results.get(0)));
			List<FiducialCommon.Landmarks> landmarks = parseLandmarks(new File(dataSetDir, "landmarks.txt"));
			for( int i = 0; i < detected.size(); i++ ) {
				FiducialCommon.Detected det = detected.get(i);
				FiducialCommon.Landmarks landmark = lookupLandmark(landmarks,det.id);
				List<Point2D_F64> corners = project(adjustCoordinate(det.fiducialToCamera),landmark);
				truthCorners.addAll(corners);
			}
		}

		normalsPrev = new Vector3D_F64[ expected.length ];
		posePrev = new Se3_F64[ expected.length ];
		for (int i = 0; i < expected.length; i++) {
			normalsPrev[i] = new Vector3D_F64();
			posePrev[i] = new Se3_F64();
		}

		GrowQueue_F64 errorNormals = new GrowQueue_F64();
		GrowQueue_F64 errorLocation = new GrowQueue_F64();

		resetStatistics();
		for (int i = 0; i < results.size(); i++) {
			String resultPath = results.get(i);
			String name = new File(resultPath).getName();

			// mark them all as not observed
			for (int j = 0; j < expected.length; j++) {
				fiducialNormal[j].set(0,0,0);
			}

			try {
				List<FiducialCommon.Detected> detected = parseDetections(new File(resultPath));
				List<FiducialCommon.Landmarks> landmarks = parseLandmarks(new File(dataSetDir, "landmarks.txt"));
				evaluate(name,detected,truthCorners,landmarks);

//				if( detected.size() == 0 ) {
//					System.out.println("no detection "+resultPath);
//				}

				for (int j = 0; j < expected.length; j++) {
					if( detectedCorners[j] != null ) {
						allDetections[j].add( detectedCorners[j]);

						if( i > 0 ) {
							if( normalsPrev[j].normSq() > 0 ) {
								double angle = normalsPrev[j].acute(fiducialNormal[j]);
								errorNormals.add(UtilAngle.radianToDegree(angle));

								double location = posePrev[j].getT().distance(fiducialPose[j].getT());
								errorLocation.add(location);
							}
						}
					}
					normalsPrev[j].set(fiducialNormal[j]);
					posePrev[j].set(fiducialPose[j]);
				}

			} catch( RuntimeException e ) {
				e.printStackTrace(err);
			}
		}

		Arrays.sort(errors.data, 0, errors.size);

		double accuracy50 = errors.size == 0 ? 0 : errors.get( (int)(errors.size()*0.5));
		double accuracy90 = errors.size == 0 ? 0 : errors.get( (int)(errors.size()*0.9));
		double accuracy100 = errors.size == 0 ? 0 :errors.get( errors.size()-1);

		GrowQueue_F64 precision = computePrecision(allDetections);
		Arrays.sort(precision.data, 0, precision.size);

		double precision50 = precision.size ==0 ? 0 : precision.get( (int)(precision.size()*0.5));
		double precision90 = precision.size ==0 ? 0 : precision.get( (int)(precision.size()*0.9));
		double precision100 = precision.size ==0 ? 0 : precision.get( precision.size()-1);

		// normal angles should not change much if stable
		Arrays.sort(errorNormals.data, 0, errorNormals.size);
		double normal50 = errorNormals.size ==0 ? 0 : errorNormals.get( (int)(errorNormals.size()*0.5));
		double normal90 = errorNormals.size ==0 ? 0 : errorNormals.get( (int)(errorNormals.size()*0.9));
		double normal100 = errorNormals.size ==0 ? 0 : errorNormals.get( errorNormals.size()-1);

		Arrays.sort(errorLocation.data, 0, errorLocation.size);
		double location50 = errorLocation.size ==0 ? 0 : errorLocation.get( (int)(errorLocation.size()*0.5));
		double location90 = errorLocation.size ==0 ? 0 : errorLocation.get( (int)(errorLocation.size()*0.9));
		double location100 = errorLocation.size ==0 ? 0 : errorLocation.get( errorLocation.size()-1);

		outputResults.println();
		outputResults.println("Summary:");
		outputResults.println(" correct            : " + totalCorrect);
		outputResults.println(" wrong order        : " + totalWrongOrder);
		outputResults.println(" wrong ID           : " + totalWrongID);
		outputResults.println(" duplicates         : " + totalDuplicates);
		outputResults.println(" false positive     : " + totalFalsePositive);
		outputResults.println(" false negative     : " + totalFalseNegative);
		outputResults.println("Normal Angle (deg):     "+errorNormals.size);
		outputResults.println(" errors 50%         : " + normal50);
		outputResults.println(" errors 90%         : " + normal90);
		outputResults.println(" errors 100%        : " + normal100);
		outputResults.println("Location            : "+errorLocation.size);
		outputResults.println(" errors 50%         : " + location50);
		outputResults.println(" errors 90%         : " + location90);
		outputResults.println(" errors 100%        : " + location100);
		outputResults.println("Precision:              "+precision.size);
		outputResults.println(" errors 50%         : " + precision50);
		outputResults.println(" errors 90%         : " + precision90);
		outputResults.println(" errors 100%        : " + precision100);
		outputResults.println("Accuracy:               "+errors.size);
		outputResults.println(" errors 50%         : " + accuracy50);
		outputResults.println(" errors 90%         : " + accuracy90);
		outputResults.println(" errors 100%        : " + accuracy100);
	}

	/**
	 * Computes the precision by finding the average corner for each detection.  Then it computes
	 * the error for all corners
	 */
	public static GrowQueue_F64 computePrecision( List<List<Point2D_F64>> allDetections[] ) {
		GrowQueue_F64 errors = new GrowQueue_F64();

		int numPoints = allDetections[0].get(0).size();

		Point2D_F64 average[] = new Point2D_F64[numPoints];
		for (int i = 0; i < numPoints; i++) {
			average[i] = new Point2D_F64();
		}
		for (int fid = 0; fid < allDetections.length; fid++) {
			List<List<Point2D_F64>> detections = allDetections[fid];
			if( detections.size() <= 2 )
				continue;

			for (int i = 0; i < numPoints; i++) {
				average[i].set(0,0);
			}

			for( List<Point2D_F64> corners : detections ) {
				for (int i = 0; i < numPoints; i++) {
					Point2D_F64 p = corners.get(i);
					Point2D_F64 a = average[i];

					a.x += p.x;
					a.y += p.y;
				}
			}

			for (int i = 0; i < numPoints; i++) {
				average[i].x /= detections.size();
				average[i].y /= detections.size();
			}

			for( List<Point2D_F64> corners : detections ) {
				for (int i = 0; i < numPoints; i++) {
					Point2D_F64 p = corners.get(i);
					Point2D_F64 a = average[i];

					errors.add( p.distance(a) );
				}
			}
		}

		return errors;
	}

	public static void main(String[] args) {
		EvaluateStaticFiducialSequence app = new EvaluateStaticFiducialSequence();

//		app.initialize(new File("data/fiducials/image"));
//		app.evaluate(new File("tmp"),"static_front_close");
	}
}