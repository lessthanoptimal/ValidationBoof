package boofcv.metrics;

import boofcv.common.BoofRegressionConstants;
import georegression.metric.ClosestPoint2D_F32;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

/**
 * Evaluates hough line detectors or any other algorithm which fits lines without end points.
 *
 * @author Peter Abeles
 */
public class EvaluateHoughLineDetector {
	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	String detectorName;
	boolean detectorThin;


	public int summaryTruePositives;
	public int summaryExpected;
	public int summaryFalsePositive;
	double summaryError = 0;

	/**
	 * Distance a corner can be from the shape to be considered a match
	 */
	public static final double MATCH_DISTANCE_PIXELS = 6.0;
	public static final double FRACTION_MATCH = 0.75;


	public void setDetector( String name , boolean thin ) {
		this.detectorName = name;
		this.detectorThin = thin;
	}

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErr(PrintStream err) {
		this.err = err;
	}

	public void evaluate(File dataDir , File resultsDir ) {

		outputResults.println("# Match Tolerance Closest Point = "+MATCH_DISTANCE_PIXELS);
		outputResults.println("# Match Tolerance Fraction Match = "+FRACTION_MATCH);
		outputResults.println("# Directory (expected) (detected) (true positives) (false positives) (average error)");
		outputResults.printf("# %-18s %2s %2s %2s %2s %7s\n","Directory","E","D","TP","FP","Error");

		List<File> files = BoofRegressionConstants.listAndSort(dataDir);


		summaryExpected = 0;
		summaryError = 0;
		summaryTruePositives = 0;
		int totalImages = 0;
		for( File f : files ) {
			if( !(f.getName().endsWith("jpg")||f.getName().endsWith("png")))
				continue;

			String nameResults = UtilShapeDetector.imageToDetectedName(f.getName());
			String nameTruth = FilenameUtils.getBaseName(f.getName())+".txt";

			File truthFile = new File(dataDir, nameTruth);
			if( truthFile.exists() ) {
				totalImages++;
				List<LineSegment2D_F32> truth = UtilShapeDetector.loadTruthLineSegments(truthFile);
				List<LineParametric2D_F32> found = UtilShapeDetector.loadResultsLines(new File(resultsDir, nameResults));

				evaluateFile(f.getName(), truth, found);
			} else {
				outputResults.println("No truth for "+f.getName());
			}
		}

		outputResults.println();
		outputResults.printf("Summary: matched = %d / %d error = %7.3f FP = %d\n",
				summaryTruePositives, summaryExpected, summaryError/totalImages,summaryFalsePositive);
		outputResults.flush();
	}

	private void evaluateFile(String fileName, List<LineSegment2D_F32> truth, List<LineParametric2D_F32> found) {

		double averageError = 0;
		int truePositives = 0;

		for (LineSegment2D_F32 t : truth) {
			double bestScore = Double.MAX_VALUE;
			for (int i = 0; i < found.size(); i++) {
				double score = score(t,found.get(i));
				if( Double.isNaN(score))
					continue;

				if( score < bestScore ) {
					bestScore = score;
				}
			}

			if( bestScore != Double.MAX_VALUE ) {
				averageError += bestScore;
				truePositives++;
			}
		}

		averageError /= truePositives;
		int falsePositives = found.size()-truePositives;

		outputResults.printf("%-20s %2d %2d %2d %2d %7.4f\n", FilenameUtils.getBaseName(fileName), truth.size(), found.size(),
				truePositives, falsePositives, averageError);

		summaryError += averageError;
		summaryTruePositives += truePositives;
		summaryFalsePositive += falsePositives;
		summaryExpected += truth.size();
	}

	public double score( LineSegment2D_F32 segment , LineParametric2D_F32 line ) {
		int N = 20;
		float error = 0;
		int inside=0;

		Point2D_F32 ps = new Point2D_F32();
		Point2D_F32 pl = new Point2D_F32();

		for (int i = 0; i < N; i++) {
			// point on line segment
			ps.x = segment.a.x + i*(segment.b.x-segment.a.x)/(N-1);
			ps.y = segment.a.y + i*(segment.b.y-segment.a.y)/(N-1);

			ClosestPoint2D_F32.closestPoint(line,ps,pl);

			float d = ps.distance(pl);
			if( d <= MATCH_DISTANCE_PIXELS) {
				inside++;
				error += d;
			}
		}

		if( inside > FRACTION_MATCH*N ) {
			return error / inside;
		} else {
			return Double.NaN;
		}
	}

	public static void main(String[] args) {
		EvaluatePolygonDetector app = new EvaluatePolygonDetector();

		File dataDir = new File("data/shape/concave/");
		File resultsDir = new File("tmp");

		app.evaluate(dataDir,resultsDir);
	}
}
