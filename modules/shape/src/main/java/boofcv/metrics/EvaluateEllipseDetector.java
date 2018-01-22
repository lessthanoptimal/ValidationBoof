package boofcv.metrics;

import boofcv.common.BoofRegressionConstants;
import boofcv.common.misc.EllipseFileCodec;
import georegression.geometry.UtilEllipse_F64;
import georegression.metric.ClosestPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.struct.shapes.Polygon2D_F64;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateEllipseDetector {

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	/**
	 * Fraction of pixels which need to be close for two ellip[ses to be considered to be a match
	 */
	public static final double MATCH_TOLERANCE = 0.85;

	/**
	 * Maximum distance a pixel can be away from the ellipse to be considered a match
	 */
	public static final double MAX_PIXEL_DISTANCE = 3.0;

	/**
	 * Number of pixels around the ellipse which will be sampled when verifying a match
	 */
	public static final int NUM_SAMPLE = 30;

	/**
	 * Maximum two centers can be from each other to be considered for a match
	 */
	public static final double MAX_CENTER_DISTANCE = 5.0;

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErr(PrintStream err) {
		this.err = err;
	}

	public void evaluate( File dataDir , File resultsDir ) {


		outputResults.println("# Match Fraction        = "+MATCH_TOLERANCE);
		outputResults.println("# Match MAX DISTANCE    = "+MAX_PIXEL_DISTANCE);
		outputResults.println("# SAMPLES               = "+NUM_SAMPLE);
		outputResults.println("# MAX CENTER DISTANCE   = "+MAX_CENTER_DISTANCE);
		outputResults.println("# Image (expected) (detected) (multiple) (false positives) (false negative) (average error)");

		List<File> files = BoofRegressionConstants.listAndSort(dataDir);

		for( File f : files ) {
			if( !(f.getName().endsWith("jpg")||f.getName().endsWith("png")))
				continue;

			String nameResults = UtilShapeDetector.imageToDetectedName(f.getName());
			String nameTruth = f.getName().substring(0,f.getName().length()-3)+"txt";

			List<EllipseRotated_F64> truth = EllipseFileCodec.load(new File(dataDir, nameTruth).getPath());
			List<EllipseRotated_F64> found = EllipseFileCodec.load(new File(resultsDir,nameResults).getPath());

			evaluateFile(f.getName(), truth, found);
		}

		outputResults.flush();
	}

	private void evaluateFile(String fileName, List<EllipseRotated_F64> truth, List<EllipseRotated_F64> found) {


		int matchedTruth[] = new int[truth.size()];
		int matchedFound[] = new int[found.size()];

		double totalError = 0;


		for( EllipseRotated_F64 t : truth ) {

			MatchScore bestScore = new MatchScore();
			EllipseRotated_F64 bestF = null;

			for( int i = 0; i < found.size(); i++ ) {
				EllipseRotated_F64 f = found.get(i);

				MatchScore score = computeMatchScore(t,f);

				if( score.numMatches > bestScore.numMatches ) {
					bestF = f;
					bestScore = score;
				} else if( score.numMatches == bestScore.numMatches ) {
					if( score.averageDistance < bestScore.averageDistance ) {
						bestF = f;
						bestScore = score;
					}
				}
			}

			if( bestF != null ) {
				matchedFound[found.indexOf(bestF)]++;
				matchedTruth[truth.indexOf(t)]++;
				totalError += bestScore.averageDistance;
			}
		}


		int numMultiple = 0;
		int falsePositives = 0;
		int totalTruthMatched = 0;

		for (int i = 0; i < matchedTruth.length; i++) {
			if( matchedTruth[i] == 1 ) {
				totalTruthMatched++;
			} else if( matchedTruth[i] > 1 ) {
				numMultiple++;
			}
		}

		for (int i = 0; i < matchedFound.length; i++) {
			if( matchedFound[i] == 0 ) {
				falsePositives++;
			}
		}

		totalError /= totalTruthMatched;
		int numFalseNegative = truth.size()-totalTruthMatched;

		outputResults.printf("%-15s %2d %2d %2d %2d %2d %7.4f\n", fileName, truth.size(), found.size(),
				numMultiple, falsePositives, numFalseNegative, totalError);
	}

	private static double size( Polygon2D_F64 p ) {
		double total = 0;
		for (int i = 0,j = p.size()-1; i < p.size();j=i, i++) {
			total += p.get(i).distance(p.get(j));
		}
		return total;
	}

	protected MatchScore computeMatchScore(EllipseRotated_F64 a , EllipseRotated_F64 b  ) {
		MatchScore score = new MatchScore();

		if( a.center.distance(b.center) > MAX_CENTER_DISTANCE )
			return score;


		Point2D_F64 pa = new Point2D_F64();

		for (int i = 0; i < NUM_SAMPLE; i++) {
			double theta = i*Math.PI*2.0/NUM_SAMPLE;
			UtilEllipse_F64.computePoint(theta,a,pa);

			Point2D_F64 pb = ClosestPoint2D_F64.closestPoint(b,pa);

			double d = pa.distance(pb);

			if( d <= MAX_PIXEL_DISTANCE ) {
				score.averageDistance += d;
				score.numMatches++;
			}
		}

		if( score.numMatches >= NUM_SAMPLE*MATCH_TOLERANCE) {

			score.averageDistance /= score.numMatches;

			return score;
		} else {
			return new MatchScore();
		}
	}


	private static class MatchScore {
		int numMatches;
		double averageDistance;
	}

	public static void main(String[] args) {
		EvaluateEllipseDetector app = new EvaluateEllipseDetector();

		File dataDir = new File("data/fiducials/acircle_grid/standard/cardboard");
		File resultsDir = new File("tmp");

		app.evaluate(dataDir,resultsDir);
	}
}
