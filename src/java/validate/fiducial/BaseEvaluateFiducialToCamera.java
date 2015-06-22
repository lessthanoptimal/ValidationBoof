package validate.fiducial;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.io.UtilIO;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
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
 * False Positive: A detected fiducial which did not match any labeled fiducials in the image.
 *
 * @author Peter Abeles
 */
public class BaseEvaluateFiducialToCamera {
	double maxPixelError = 5;

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	File baseDirectory;
	IntrinsicParameters intrinsic;
	// ID's of the detected fiducials
	int expected[];
	// The number of times a fiducial was detected.  This will include wrong ID's and orientations
	int fiducialDetected[];

	// if a fiducial was detected it's quad is stored here.  If there are multiple detections
	// the only the first detection is saved.
	List<Point2D_F64> detectedCorners[];

	// average errors for all 4 corners in a fiducial for each detected fiducials
	GrowQueue_F64 errors = new GrowQueue_F64();

	FiducialCommon.Scenario scenario;

	List<Point3D_F64> fiducialPts = new ArrayList<Point3D_F64>();

	// Fiducial IDs that were assigned to false positive
	GrowQueue_I32 falsePositiveIDs = new GrowQueue_I32();

	// everything correct
	int totalCorrect;
	// correct ID and corners are good, but orientation is bad
	int totalWrongOrientation;
	// corner match but ID doesn't
	int totalWrongID;
	// complete false positive
	int totalFalsePositive;
	// nothing was matched to a fiducial
	int totalFalseNegative;
	// total number of times a fiducial was detected two or more times in the same image
	// each count past one is counted twice
	int totalDuplicates;


	protected File initialize(String dataset) {
		File dataSetDir = new File(baseDirectory,dataset);

		expected = FiducialCommon.parseExpectedIds(new File(dataSetDir, "expected.txt"), scenario);
		fiducialDetected = new int[expected.length];
		detectedCorners = new ArrayList[expected.length];
		for (int i = 0; i < expected.length; i++) {
			detectedCorners[i] = new ArrayList<Point2D_F64>();
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
		scenario = FiducialCommon.parseScenario(new File(baseDirectory, "fiducials.txt"));
		double fiducialWidth = scenario.getWidth();

		fiducialPts.add( new Point3D_F64(-fiducialWidth/2, fiducialWidth/2,0));
		fiducialPts.add( new Point3D_F64( fiducialWidth/2, fiducialWidth/2,0));
		fiducialPts.add( new Point3D_F64( fiducialWidth/2,-fiducialWidth/2,0));
		fiducialPts.add( new Point3D_F64(-fiducialWidth/2,-fiducialWidth/2,0));
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

	protected void evaluate( String fileName , List<FiducialCommon.Detected> detected , List<Point2D_F64> truthCorners ) {

		for (int i = 0; i < expected.length; i++) {
			fiducialDetected[i] = 0;
			detectedCorners[i] = null;
		}

		for( int i = 0; i < detected.size(); i++ ) {
			FiducialCommon.Detected det = detected.get(i);
			List<Point2D_F64> corners = project(det.fiducialToCamera);

			Assignment match = findBestAssignment(corners,truthCorners);
			if( match == null ) {
				falsePositiveIDs.add(det.id);
				totalFalsePositive++;
			} else if( match.id == det.id ) {
				if( match.ori == 0 )
					totalCorrect++;
				else
					totalWrongOrientation++;
			} else {
				totalWrongID++;
			}

			if( match != null ) {
				errors.add( match.error );
				outputResults.println(fileName+" "+det.id + " " + match.id + " " + match.ori + " "+match.error);
			} else {
				outputResults.println(fileName+" "+det.id +" false positive");
			}
		}

		for (int i = 0; i < fiducialDetected.length; i++) {
			if( fiducialDetected[i] == 0 ) {
				outputResults.println(fileName+" false negative for ID "+expected[i]);
				totalFalseNegative++;
			}
			totalDuplicates += Math.max(0,fiducialDetected[i]-1);
		}
	}

	private List<Point2D_F64> project( Se3_F64 fiducialToCamera ) {
		List<Point2D_F64> pixels = new ArrayList<Point2D_F64>();

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
		best.error = maxPixelError;

		int bestExpectedIndex=-1;

		for (int i = 0; i < truthCorners.size(); i += 4 ) {
			for (int ori = 0; ori < 4; ori++) {
				double error = 0;
				for (int k = 0; k < 4; k++) {
					int index = (ori+k)%4;
					double d = corners.get(k).distance(truthCorners.get(i+index));
					error += d;
				}
				error /= 4;

				if( error < best.error ) {
					best.error = error;
					best.ori = ori;
					best.id = expected[i/4];
					bestExpectedIndex = i/4;
				}
			}
		}

		if( bestExpectedIndex >= 0 ) {
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

	public static class Assignment
	{
		int id;
		int ori;
		double error;
	}
}
