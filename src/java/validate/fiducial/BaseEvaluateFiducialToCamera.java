package validate.fiducial;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.io.UtilIO;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for evaluating fiducial assignments
 *
 * True positives: Any detected fiducial which matched a hand selected one within tolerance.
 *                 * correct: Correct ID and orientation
 *                 * wrong orientation: Correct ID but had to be rotated to match
 *                 * wrong ID: The wrong ID was assigned to it.  Orientation isn't considered
 * False Positive: A detected fiducial which did not match any labeled fiducials in the image.
 * False Negative: No detected fiducial was found to be within tolerance.
 *
 * @author Peter Abeles
 */
public abstract class BaseEvaluateFiducialToCamera {
	double maxPixelError = 5;

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	File baseDirectory;
	IntrinsicParameters intrinsic;
	// ID's of the detected fiducials
	int expected[];
	// The number of times a fiducial was detected.  This will include wrong ID's and orientations
	int fiducialDetected[];
	// normal vector for a specific truth
	Vector3D_F64 fiducialNormal[];

	// if a fiducial was detected it's quad is stored here.  If there are multiple detections
	// the only the first detection is saved.
	List<Point2D_F64> detectedCorners[];

	// average errors for all 4 corners in a fiducial for each detected fiducials
	GrowQueue_F64 errors = new GrowQueue_F64();

	FiducialCommon.Scenario scenario;

	List<Point3D_F64> fiducialPts = new ArrayList<Point3D_F64>();

	// Fiducial IDs that were assigned to false positive
	GrowQueue_I32 falsePositiveIDs = new GrowQueue_I32();

	// Only print the summary results
	boolean justSummary = false;

	// everything correct
	int totalCorrect;
	// correct ID and corners are good, but orientation is bad
	int totalWrongOrientation;
	// wrong z-direction
	int totalWrongZ;
	// corner match but ID doesn't
	int totalWrongID;
	// complete false positive
	int totalFalsePositive;
	// nothing was matched to a fiducial
	int totalFalseNegative;
	// total number of times a fiducial was detected two or more times in the same image
	// each count past one is counted twice.
	// NOTE: Duplicate refers to physical fuducial not duplicate IDs
	int totalDuplicates;
	// transform from libraries fiducial coordinate system (prior to scaling by its width) into
	// the standard one.  Which is +z up, origin at fiducial center. sides aligned along axises, +y top +x right
	private Se3_F64 transformToStandard;

	public BaseEvaluateFiducialToCamera() {
		fiducialPts.add( new Point3D_F64());
		fiducialPts.add( new Point3D_F64());
		fiducialPts.add( new Point3D_F64());
		fiducialPts.add( new Point3D_F64());
	}

	protected File initialize(String dataset) {
		File dataSetDir = new File(baseDirectory,dataset);
		scenario = FiducialCommon.parseScenario(new File(dataSetDir, "expected.txt"));

		expected = scenario.getKnownIds();
		fiducialDetected = new int[expected.length];
		detectedCorners = new ArrayList[expected.length];
		fiducialNormal = new Vector3D_F64[expected.length];
		for (int i = 0; i < expected.length; i++) {
			detectedCorners[i] = new ArrayList<Point2D_F64>();
			fiducialNormal[i] = new Vector3D_F64();
		}
		intrinsic = UtilIO.loadXML(new File(dataSetDir, "intrinsic.xml").toString());
		if( intrinsic.radial != null )
			throw new IllegalArgumentException("Expected no distortion");
		return dataSetDir;
	}

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

	public void initialize( File baseDirectory ) {
		this.baseDirectory = baseDirectory;
	}

	protected void resetStatistics() {
		errors.reset();
		falsePositiveIDs.reset();
		totalFalsePositive = 0;
		totalWrongOrientation = 0;
		totalWrongID = 0;
		totalFalsePositive = 0;
		totalFalseNegative = 0;
	}

	/**
	 * Evaluates previously computed results in the specified directory using the specified dataset
	 * @param resultsDirectory Directory containing results
	 * @param dataset name of the data set being used
	 */
	public abstract void evaluate( File resultsDirectory , String dataset );

	private Se3_F64 adjustCoordinate( Se3_F64 foundF2C , double width ) {
		if( transformToStandard == null ) {
			return foundF2C;
		} else {
			Se3_F64 fidToStandard = transformToStandard.copy();
			fidToStandard.getT().scale(width);
			Se3_F64 foundC2F = foundF2C.invert(null);
			return foundC2F.concat(fidToStandard, null).invert(null);
		}
	}

	protected void evaluate( String fileName , List<FiducialCommon.Detected> detected , List<Point2D_F64> truthCorners ) {

		for (int i = 0; i < expected.length; i++) {
			fiducialDetected[i] = 0;
			detectedCorners[i] = null;
		}

		for( int i = 0; i < detected.size(); i++ ) {
			FiducialCommon.Detected det = detected.get(i);
			double fiducialWidth = scenario.getWidth(det.id);
			List<Point2D_F64> corners = project(adjustCoordinate(det.fiducialToCamera,fiducialWidth),fiducialWidth);

			Assignment match = findBestAssignment(corners,truthCorners);
			if( match == null ) {
				falsePositiveIDs.add(det.id);
				totalFalsePositive++;
			} else {
				Vector3D_F64 normal = fiducialNormal[match.index];
				normal.x = det.fiducialToCamera.getR().get(0,2);
				normal.y = det.fiducialToCamera.getR().get(1,2);
				normal.z = det.fiducialToCamera.getR().get(2,2);

				if( match.id == det.id ) {
					if( match.reverseOrder )
						totalWrongZ++;
					if( match.ori == 0 )
						totalCorrect++;
					else
						totalWrongOrientation++;
				} else {
					totalWrongID++;
				}
			}

			if( match != null ) {
				for (int j = 0; j < 4; j++) {
					errors.add( match.errors[j] );
				}

				if( !justSummary )
					outputResults.println(fileName+" "+det.id + " " + match.id + " " + match.ori + " "+match.meanError);
			} else {
				if( !justSummary )
					outputResults.println(fileName+" "+det.id +" false positive");
			}
		}

		for (int i = 0; i < fiducialDetected.length; i++) {
			if( fiducialDetected[i] == 0 ) {
				if( !justSummary )
					outputResults.println(fileName+" false negative for ID "+expected[i]);
				totalFalseNegative++;
			}
			totalDuplicates += Math.max(0,fiducialDetected[i]-1);
		}
	}

	private List<Point2D_F64> project( Se3_F64 fiducialToCamera , double fiducialWidth ) {
		List<Point2D_F64> pixels = new ArrayList<Point2D_F64>();

		fiducialPts.get(0).set(-fiducialWidth / 2, fiducialWidth / 2, 0);
		fiducialPts.get(1).set(fiducialWidth / 2, fiducialWidth / 2, 0);
		fiducialPts.get(2).set(fiducialWidth / 2, -fiducialWidth / 2, 0);
		fiducialPts.get(3).set(-fiducialWidth / 2, -fiducialWidth/2,0);

		for (int i = 0; i < 4; i++) {
			Point3D_F64 f = fiducialPts.get(i);
			Point3D_F64 c = new Point3D_F64();

			SePointOps_F64.transform(fiducialToCamera, f, c);

			Point2D_F64 p = new Point2D_F64();
			PerspectiveOps.convertNormToPixel(intrinsic, c.x / c.z, c.y / c.z, p);

			pixels.add(p);
		}

		return pixels;
	}

	private Assignment findBestAssignment( List<Point2D_F64> corners , List<Point2D_F64> truthCorners ) {
		Assignment best = new Assignment();
		best.id = -1;
		best.meanError = maxPixelError;

		int bestExpectedIndex=-1;

		double errors[] = new double[4];
		for (int i = 0; i < truthCorners.size(); i += 4 ) {
			for (int ori = 0; ori < 4; ori++) {
				double meanError = 0;
				for (int k = 0; k < 4; k++) {
					int index = (ori+k)%4;
					errors[k] = corners.get(k).distance(truthCorners.get(i+index));
					meanError += errors[k];
				}
				meanError /= 4;

				if( meanError < best.meanError ) {
					best.meanError = meanError;
					best.ori = ori;
					best.id = expected[i/4];
					best.reverseOrder = false;
					System.arraycopy(errors,0,best.errors,0,4);
					bestExpectedIndex = i/4;
				}
			}

			// try going around in the different direction
			for (int ori = 0; ori < 4; ori++) {
				double meanError = 0;
				for (int k = 0; k < 4; k++) {
					int index = (ori-k)%4;
					if( index < 0 ) index = 4 + index;
					errors[k] = corners.get(k).distance(truthCorners.get(i+index));
					meanError += errors[k];
				}
				meanError /= 4;

				if( meanError < best.meanError ) {
					best.meanError = meanError;
					best.ori = ori;
					best.id = expected[i/4];
					best.reverseOrder = true;
					System.arraycopy(errors,0,best.errors,0,4);
					bestExpectedIndex = i/4;
				}
			}
		}

		if( bestExpectedIndex >= 0 ) {
			best.index = bestExpectedIndex;
			fiducialDetected[bestExpectedIndex]++;
			if( fiducialDetected[bestExpectedIndex] == 1 ) {
				// reorder the points to make comparision easier later on
				List<Point2D_F64> reordered = new ArrayList<Point2D_F64>();
				for (int i = 0; i < 4; i++) {
					int index = (best.ori+i)%4;
					reordered.add(corners.get(index));
				}
				detectedCorners[bestExpectedIndex] = reordered;
			}
			return best;
		} else
			return null;
	}

	public void setMaxPixelError(double maxPixelError) {
		this.maxPixelError = maxPixelError;
	}

	public void setTransformToStandard(Se3_F64 transformToStandard) {
		this.transformToStandard = transformToStandard;
	}

	public void setJustSummary(boolean justSummary) {
		this.justSummary = justSummary;
	}

	public static class Assignment
	{
		int index;
		int id;
		int ori;
		boolean reverseOrder;
		double meanError;
		double errors[] = new double[4];
	}
}
