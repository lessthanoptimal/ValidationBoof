package validate.fiducial;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
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
public abstract class BaseEvaluateFiducialToCamera implements FiducialEvaluateInterface {
	double maxPixelError = 5;

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	CameraPinholeRadial intrinsic;
	// ID's of the detected fiducials
	long expected[];
	// The number of times a fiducial was detected.  This will include wrong ID's and orientations
	long fiducialDetected[];

	@Override
	public int getTotalExpected() {
		return totalExpected;
	}

	@Override
	public int getTotalCorrect() {
		return totalCorrect;
	}

	// normal vector for a specific truth
	Vector3D_F64 fiducialNormal[];
	Se3_F64 fiducialPose[];

	// if a fiducial was detected it's quad is stored here.  If there are multiple detections
	// the only the first detection is saved.
	List<Point2D_F64> detectedCorners[];

	// average errors for all 4 corners in a fiducial for each detected fiducials
	GrowQueue_F64 errors = new GrowQueue_F64();

	FiducialCommon.Library library;
	List<String> visible;


	// Fiducial IDs that were assigned to false positive
	GrowQueue_I32 falsePositiveIDs = new GrowQueue_I32();

	// Only print the summary results
	boolean justSummary = false;

	// expected number of fiducials
	public int totalExpected;
	// everything correct
	public int totalCorrect;
	// correct ID and corners are good, but order of the corners is bad
	int totalWrongOrder;
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
	}

	protected void initializeEvaluate(File dataSetDir) {
		library = FiducialCommon.parseScenario(new File(dataSetDir, "library.txt"));
		List<String> visible = FiducialCommon.parseVisibleFile(new File(dataSetDir,"visible.txt"));

		expected = new long[visible.size()];
		for( int i = 0; i < visible.size(); i++ ) {
			expected[i] = library.nameToID(visible.get(i));
		}

		fiducialDetected = new long[expected.length];
		detectedCorners = new ArrayList[expected.length];
		fiducialNormal = new Vector3D_F64[expected.length];
		fiducialPose = new Se3_F64[expected.length];
		for (int i = 0; i < expected.length; i++) {
			detectedCorners[i] = new ArrayList<Point2D_F64>();
			fiducialNormal[i] = new Vector3D_F64();
			fiducialPose[i] = new Se3_F64();
		}
		intrinsic = FiducialCommon.parseIntrinsic(new File(dataSetDir,"intrinsic.txt"));
	}

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

	protected void resetStatistics() {
		errors.reset();
		falsePositiveIDs.reset();
		totalExpected = 0;
		totalCorrect = 0;
		totalWrongID = 0;
		totalFalsePositive = 0;
		totalFalseNegative = 0;
		totalDuplicates = 0;
	}

	/**
	 * Evaluates previously computed results in the specified directory using the specified dataset
	 * @param resultsDirectory Directory containing results
	 * @param dataset name of the data set being used
	 */
	public abstract void evaluate( File resultsDirectory , File dataset );

	private Se3_F64 adjustCoordinate( Se3_F64 foundF2C ) {
		if( transformToStandard == null ) {
			return foundF2C;
		} else {
			Se3_F64 fidToStandard = transformToStandard.copy();
			Se3_F64 foundC2F = foundF2C.invert(null);
			return foundC2F.concat(fidToStandard, null).invert(null);
		}
	}

	protected void evaluate( String fileName , List<FiducialCommon.Detected> detected ,
							 List<Point2D_F64> truthCorners, List<FiducialCommon.Landmarks> landmarks ) {

		totalExpected += expected.length;
		for (int i = 0; i < expected.length; i++) {
			fiducialDetected[i] = 0;
			detectedCorners[i] = null;
		}

		List<List<Point2D_F64>> truthFiducialCorners = extractIndividual(truthCorners,expected.length );

		for( int i = 0; i < detected.size(); i++ ) {
			FiducialCommon.Detected det = detected.get(i);
			FiducialCommon.Landmarks landmark = lookupLandmark(landmarks,det.id);
			List<Point2D_F64> corners = project(adjustCoordinate(det.fiducialToCamera),landmark);

			Assignment match = findBestAssignment(corners,truthFiducialCorners);
			if( match == null ) {
				falsePositiveIDs.add(det.id);
				totalFalsePositive++;
			} else {
				Vector3D_F64 normal = fiducialNormal[match.index];
				normal.x = det.fiducialToCamera.getR().get(0,2);
				normal.y = det.fiducialToCamera.getR().get(1,2);
				normal.z = det.fiducialToCamera.getR().get(2,2);
				fiducialPose[match.index].set(det.fiducialToCamera);

				if( match.id == det.id ) {
					if( match.outOfOrder )
						totalWrongOrder++;
					else
						totalCorrect++;
				} else {
					totalWrongID++;
				}
			}

			if( match != null ) {
				for (int j = 0; j < match.errors.length; j++) {
					errors.add( match.errors[j] );
				}

				if( !justSummary )
					outputResults.println(fileName+" "+det.id + " " + match.id + " " +match.outOfOrder+ " "+match.meanError);
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

	protected List<List<Point2D_F64>> extractIndividual(List<Point2D_F64> truthCorners, int totalFiducials) {
		List<List<Point2D_F64>> ret = new ArrayList<List<Point2D_F64>>();

		int N = truthCorners.size()/totalFiducials;

		for (int i = 0; i < truthCorners.size(); i += N) {
			List<Point2D_F64> list = new ArrayList<Point2D_F64>();
			for (int j = 0; j < N; j++) {
				list.add( truthCorners.get(i+j));
			}

			ret.add(list);
		}
		return ret;
	}

	private static FiducialCommon.Landmarks lookupLandmark( List<FiducialCommon.Landmarks> landmarks, int id ) {
		for( FiducialCommon.Landmarks landmark : landmarks ) {
			if( landmark.id == id )
				return landmark;
		}
		// unknown, use the default
		return landmarks.get(0);
	}

	private List<Point2D_F64> project( Se3_F64 fiducialToCamera , FiducialCommon.Landmarks landmark ) {
		List<Point2D_F64> pixels = new ArrayList<Point2D_F64>();

		WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(intrinsic, fiducialToCamera);

		for (int i = 0; i < landmark.points.size(); i++) {
			Point3D_F64 f = landmark.points.get(i);

			pixels.add(worldToPixel.transform(f));
		}

		return pixels;
	}

	private Assignment findBestAssignment( List<Point2D_F64> corners , List<List<Point2D_F64>> truthFiducials ) {
		Assignment best = new Assignment();
		best.id = -1;
		best.meanError = maxPixelError;
		best.errors = new double[ corners.size() ];

		double errorsInOrder[] = new double[ corners.size() ];
		double errorsOutOfOrder[] = new double[ corners.size() ];

		List<Point2D_F64> reordered = new ArrayList<Point2D_F64>();
		List<Point2D_F64> bestOrdered = new ArrayList<Point2D_F64>();

		for (int i = 0; i < truthFiducials.size(); i++) {
			double errorInOrder = scoreInOrder(corners,truthFiducials.get(i),errorsInOrder);
			double errorOutOfOrder = scoreOutOfOrder(corners,truthFiducials.get(i),reordered,errorsOutOfOrder);

			if( errorInOrder > best.meanError && errorOutOfOrder > best.meanError ) {
				continue;
			}

			if( errorOutOfOrder*1.5 < errorInOrder ) {
				best.index = i;
				best.id = expected[i];
				best.meanError = errorOutOfOrder;
				best.outOfOrder = true;
				bestOrdered.clear();
				bestOrdered.addAll(reordered);
				System.arraycopy(errorsOutOfOrder,0,best.errors,0,corners.size());
			} else if( errorInOrder < best.meanError ) {
				best.index = i;
				best.id = expected[i];
				best.meanError = errorInOrder;
				best.outOfOrder = false;
				bestOrdered.clear();
				bestOrdered.addAll(corners);
				System.arraycopy(errorsInOrder,0,best.errors,0,corners.size());
			}
		}

		if( best.id != -1 ) {
			fiducialDetected[best.index]++;
			if( fiducialDetected[best.index] == 1 ) {
				List<Point2D_F64> detected = new ArrayList<Point2D_F64>();
				for (int i = 0; i < bestOrdered.size(); i++) {
					detected.add( bestOrdered.get(i).copy() );
				}

				detectedCorners[best.index] = detected;
			}
			return best;
		} else {
			return null;
		}
	}

	public double scoreInOrder( List<Point2D_F64> found , List<Point2D_F64> truth , double errors[] ) {
		double meanError = 0;
		if( truth.size() != found.size() ) {
			System.out.println("Egads.  Incorrect number of fiducials in visible file?");
		}
		for (int i = 0; i < found.size(); i++) {
			Point2D_F64 c = found.get(i);
			Point2D_F64 t = truth.get(i);

			meanError += errors[i] = c.distance(t);
		}
		return meanError / found.size();
	}

	public double scoreOutOfOrder( List<Point2D_F64> found , List<Point2D_F64> truth , List<Point2D_F64> reordered, double errors[] ) {
		reordered.clear();
		double outOfOrderError = 0;
		for (int j = 0; j < truth.size(); j++) {
			Point2D_F64 t = truth.get(j);

			double bestMatch = Double.MAX_VALUE;
			Point2D_F64 matched = null;
			for (int i = 0; i < found.size(); i++) {
				double d = found.get(i).distance(t);
				if( d < bestMatch ) {
					bestMatch = d;
					matched = found.get(i);
				}
			}
			reordered.add(matched);
			outOfOrderError += errors[j] = bestMatch;
		}
		return outOfOrderError / truth.size();
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
		long id;
		boolean outOfOrder;
		double meanError;
		double errors[];
	}
}
