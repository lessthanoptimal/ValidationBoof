package validate.fiducial;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.GrowQueue_F64;
import validate.misc.ParseHelper;
import validate.misc.PointFileCodec;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Uses previously computed transforms from fiducial to camera to reproject corner points back onto the image.
 * It then matches these up with hand selected corners and see if there are matches.  Errors and types of
 * mistakes are accumulated and printed out.
 *
 * @author Peter Abeles
 */
public class EvaluateFiducialToCamera {

	double maxPixelError = 5;

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	File baseDirectory;
	IntrinsicParameters intrinsic;
	double fiducialWidth;
	int expected[];
	boolean knownMatched[];

	List<Point3D_F64> fiducialPts = new ArrayList<Point3D_F64>();

	GrowQueue_F64 errors = new GrowQueue_F64();

	// everything correct
	int totalCorrect;
	// correct ID and corners are good, but orientation is bad
	int totalWrongOrientation;
	// corner match but ID doesn't
	int totalWrongID;
	// complete false positive
	int totalNoMatch;
	// nothing was matched to a fiducial
	int totalFalseNegative;

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErrorStream(PrintStream err) {
		this.err = err;
	}

	public void initialize( File baseDirectory ) {
		this.baseDirectory = baseDirectory;
		fiducialWidth = FiducialCommon.parseWidth(new File(baseDirectory, "fiducials.txt"));

		fiducialPts.add( new Point3D_F64(-fiducialWidth/2, fiducialWidth/2,0));
		fiducialPts.add( new Point3D_F64( fiducialWidth/2, fiducialWidth/2,0));
		fiducialPts.add( new Point3D_F64( fiducialWidth/2,-fiducialWidth/2,0));
		fiducialPts.add( new Point3D_F64(-fiducialWidth/2,-fiducialWidth/2,0));
	}

	public void evaluate( File resultsDirectory , String dataset ) {
		File dataSetDir = new File(baseDirectory,dataset);

		expected = FiducialCommon.parseExpectedIds(new File(dataSetDir, "expected.txt"));
		knownMatched = new boolean[expected.length];
		intrinsic = UtilIO.loadXML(new File(dataSetDir,"intrinsic.xml").toString());
		if( intrinsic.radial != null )
			throw new IllegalArgumentException("Expected no distortion");

		List<String> results = BoofMiscOps.directoryList(resultsDirectory.getAbsolutePath(),"csv");
		Collections.sort(results);

		errors.reset();
		totalNoMatch = 0;
		totalWrongOrientation = 0;
		totalWrongID = 0;
		totalNoMatch = 0;
		totalFalseNegative = 0;

		outputResults.println("# Data Set = "+dataset+" maxPixelError = "+maxPixelError);
		outputResults.println("# (file) (detected ID) (matched id) (matched ori) (match pixel error)");

		for (int i = 0; i < results.size(); i++) {
			String resultPath = results.get(i);
			String name = new File(resultPath).getName();
			String nameTruth = name.substring(0,name.length()-3) + "txt";

			try {
				List<Point2D_F64> truthCorners = PointFileCodec.load(new File(dataSetDir, nameTruth));
				List<Detected> detected = parseDetections(new File(resultPath));
				evaluate(name,detected,truthCorners);
			} catch( RuntimeException e ) {
				e.printStackTrace(err);
			}
		}

		Arrays.sort(errors.data,0,errors.size);

		double error50 = errors.get( (int)(errors.size()*0.5));
		double error90 = errors.get( (int)(errors.size()*0.9));
		double error100 = errors.get( errors.size()-1);

		outputResults.println();
		outputResults.println("Summary:");
		outputResults.println(" correct            : " + totalCorrect);
		outputResults.println(" wrong orientation  : " + totalWrongOrientation);
		outputResults.println(" wrong ID           : " + totalWrongID);
		outputResults.println(" no match           : " + totalNoMatch);
		outputResults.println(" false negative     : " + totalFalseNegative);
		outputResults.println(" errors 50%         : " + error50);
		outputResults.println(" errors 90%         : " + error90);
		outputResults.println(" errors 100%        : "+error100);
	}

	private void evaluate( String fileName , List<Detected> detected , List<Point2D_F64> truthCorners ) {

		Arrays.fill(knownMatched, false);

		for( int i = 0; i < detected.size(); i++ ) {
			Detected det = detected.get(i);
			List<Point2D_F64> corners = project(det.fiducialToCamera);

			Assignment match = findBestAssignment(corners,truthCorners);
			if( match == null ) {
				totalNoMatch++;
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
				outputResults.println(fileName+" "+det.id);
			}
		}

		for (int i = 0; i < knownMatched.length; i++) {
			if( !knownMatched[i] ) {
				outputResults.println(fileName+" false negative "+expected[i]);
				totalFalseNegative++;
			}
		}
	}

	private List<Point2D_F64> project( Se3_F64 fiducialToCamera ) {
		List<Point2D_F64> pixels = new ArrayList<Point2D_F64>();

		for (int i = 0; i < 4; i++) {
			Point3D_F64 f = fiducialPts.get(i);
			Point3D_F64 c = new Point3D_F64();

			SePointOps_F64.transform(fiducialToCamera,f,c);

			Point2D_F64 p = new Point2D_F64();
			PerspectiveOps.convertNormToPixel(intrinsic,c.x/c.z,c.y/c.z,p);

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
			knownMatched[bestExpectedIndex] = true;
			return best;
		} else
			return null;
	}

	private List<Detected> parseDetections( File file ) {
		try {
			List<Detected> ret = new ArrayList<Detected>();

			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			while( line != null ) {
				Detected detected = new Detected();
				detected.id = Integer.parseInt(line);
				detected.fiducialToCamera = ParseHelper.parseRigidBody(reader.readLine(),reader);
				line = reader.readLine();
				ret.add(detected);
			}

			return ret;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Assignment
	{
		int id;
		int ori;
		double error;
	}

	public static class Detected {
		int id;
		Se3_F64 fiducialToCamera;
	}

	public static void main(String[] args) {
		EvaluateFiducialToCamera app = new EvaluateFiducialToCamera();

		app.initialize(new File("data/fiducials/image"));
		app.evaluate(new File("tmp"),"distance_straight");
	}
}
