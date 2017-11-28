package validate.shape;

import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import validate.misc.PointFileCodec;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluatePolygonDetector {

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	public int summaryTruePositive;
	public int summaryFalsePositive;
	public int summaryExpected;
	double summaryError = 0;
	int summaryCount = 0;

	/**
	 * Distance a corner can be from the shape to be considered a match
	 */
	public static final double MATCH_DISTANCE_PIXELS = 3.0;

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErr(PrintStream err) {
		this.err = err;
	}

	public void evaluate( File dataDir , File resultsDir ) {


		outputResults.println("# Match Tolerance Corner to Polygon   = "+MATCH_DISTANCE_PIXELS);
		outputResults.println("# Directory (expected) (detected) (multiple) (miss matched) (true positives) (false positives) (false negative) (average error)");
		outputResults.printf("# %-13s %2s %2s %2s %2s %2s %2s %2s %7s\n","Directory","E","D","M","MM","TP","FP","FN","Error");

		List<File> files = Arrays.asList(dataDir.listFiles());

		Collections.sort(files);

		summaryTruePositive = 0;
		summaryFalsePositive = 0;
		summaryExpected = 0;
		summaryError = 0;
		summaryCount = 0;
		for( File f : files ) {
			if( !(f.getName().endsWith("jpg")||f.getName().endsWith("png")))
				continue;

			String nameResults = UtilShapeDetector.imageToDetectedName(f.getName());
			String nameTruth = f.getName().substring(0,f.getName().length()-3)+"txt";

			File truthFile = new File(dataDir, nameTruth);
			if( truthFile.exists() ) {
				List<Polygon2D_F64> truth = loadTruth(truthFile);
				List<Polygon2D_F64> found = UtilShapeDetector.loadResults(new File(resultsDir, nameResults));

				evaluateFile(f.getName(), truth, found);
			} else {
				outputResults.println("Not truth for "+f.getName());
			}
		}

		outputResults.printf("Summary: TP/E = %d / %d error = %7.3f FP = %d\n",
				summaryTruePositive, summaryExpected, summaryError/summaryCount, summaryFalsePositive);
		outputResults.flush();
	}

	private void evaluateFile(String fileName, List<Polygon2D_F64> truth, List<Polygon2D_F64> found) {
		int numMultiple = 0;
		int numMissMatched = 0;
		int falsePositives = 0;

		boolean matchedFound[] = new boolean[found.size()];

		double totalError = 0;
		int truePositives = 0;

		for (Polygon2D_F64 t : truth) {
			int minMatches = (int)(t.size()*0.9+0.5);

			List<Polygon2D_F64> matches = new ArrayList<Polygon2D_F64>();

			for (int i = 0; i < found.size(); i++) {
				Polygon2D_F64 f = found.get(i);

				if (countCornersNearPolygon(t, f, MATCH_DISTANCE_PIXELS) >= minMatches) {
					matches.add(f);
					matchedFound[i] = true;
				}
			}

			boolean missMatched = true;
			double bestError = Double.MAX_VALUE;
			for (Polygon2D_F64 p : matches) {
				if (p.size() == t.size()) {
					missMatched = false;
				}

				double error = computeError(t, p);
				if (error < bestError) {
					bestError = error;
				}
			}

			if (matches.size() > 1) {
				numMultiple += matches.size() - 1;
			}
			if (matches.size() > 0) {
				totalError += bestError;
				truePositives++;
			}

			if (matches.size() > 0 && missMatched) {
				numMissMatched++;
			}

		}

		for (int i = 0; i < matchedFound.length; i++) {
			if (!matchedFound[i]) {
				falsePositives++;
			}
		}

		totalError /= truePositives;
		int numFalseNegative = truth.size() - truePositives;

		outputResults.printf("%-15s %2d %2d %2d %2d %2d %2d %2d %7.4f\n", fileName, truth.size(), found.size(),
				numMultiple, numMissMatched, truePositives, falsePositives, numFalseNegative, totalError);

		if (truePositives > 0) {
			this.summaryError += totalError;
			summaryCount++;
		}

		summaryFalsePositive += falsePositives;
		summaryTruePositive += truePositives;
		summaryExpected += truth.size();
	}

	private static double size( Polygon2D_F64 p ) {
		double total = 0;
		for (int i = 0,j = p.size()-1; i < p.size();j=i, i++) {
			total += p.get(i).distance(p.get(j));
		}
		return total;
	}

	protected int countCornersNearPolygon(Polygon2D_F64 a , Polygon2D_F64 b , double tol ) {

		LineSegment2D_F64 line = new LineSegment2D_F64();

		int totalMatched=0;

		for (int k = 0; k < b.size(); k++) {
			Point2D_F64 pt_b = b.get(k);

			double best = Double.MAX_VALUE;
			for (int i = 0; i < a.size()+1; i++) {
				line.a = a.get(i%a.size());
				line.b = a.get((i+1)%a.size());

				double d = Distance2D_F64.distance(line,pt_b.x,pt_b.y);
				if( d < best ) {
					best = d;
				}
			}
			if( best <= tol )
				totalMatched++;
		}

		return totalMatched;
	}

	/**
	 * Compute the error of a and b as a function of the distance of N evenly spaced points on B to the closest
	 * point on A
	 * @param a
	 * @param b
	 * @return
	 */
	protected double computeError( Polygon2D_F64 a , Polygon2D_F64 b ) {
		LineSegment2D_F64 line = new LineSegment2D_F64();

		double cornerLocationsB[] = new double[b.size()+1];
		double totalLength = 0;
		for (int i = 0; i < b.size(); i++) {
			Point2D_F64 b0 = b.get(i%b.size());
			Point2D_F64 b1 = b.get((i+1)%b.size());

			cornerLocationsB[i] = totalLength;
			totalLength += b0.distance(b1);
		}
		cornerLocationsB[b.size()] = totalLength;

		int numberOfSamples = 100;

		Point2D_F64 pointOnB = new Point2D_F64();
		double error = 0;
		int cornerB = 0;
		for (int k = 0; k < numberOfSamples; k++) {
			// Find the point on B to match to a point on A
			double location = totalLength*k/numberOfSamples;

			while (location > cornerLocationsB[cornerB + 1]) {
				cornerB++;
			}
			Point2D_F64 b0 = b.get(cornerB);
			Point2D_F64 b1 = b.get((cornerB+1)%b.size());

			double locationCornerB = cornerLocationsB[cornerB];
			double fraction = (location-locationCornerB)/(cornerLocationsB[cornerB+1]-locationCornerB);

			if( fraction < 0 || fraction > 1)
				throw new RuntimeException("Egads");

			pointOnB.x = (b1.x-b0.x)*fraction + b0.x;
			pointOnB.y = (b1.y-b0.y)*fraction + b0.y;

			// find the best fit point on A to the point in B
			double best = Double.MAX_VALUE;
			for (int i = 0; i < a.size()+1; i++) {
				line.a = a.get(i%a.size());
				line.b = a.get((i+1)%a.size());

				double d = Distance2D_F64.distance(line,pointOnB);
				if( d < best ) {
					best = d;
				}
			}
			error += best;
		}

		return error/numberOfSamples;
	}


	protected List<Polygon2D_F64> loadTruth( File fileTruth ) {
		List<List<Point2D_F64>> sets = PointFileCodec.loadSets(fileTruth.getPath());

		List<Polygon2D_F64> polygons = new ArrayList<Polygon2D_F64>();

		for (int i = 0; i < sets.size(); i++) {
			List<Point2D_F64> set = sets.get(i);

			Polygon2D_F64 p = new Polygon2D_F64(set.size());

			for (int j = 0; j < p.size(); j++) {
				p.vertexes.data[j] = set.get(j);
			}

			polygons.add(p);
		}

		return polygons;
	}

	public static void main(String[] args) {
		EvaluatePolygonDetector app = new EvaluatePolygonDetector();

		File dataDir = new File("data/shape/concave/");
		File resultsDir = new File("tmp");

		app.evaluate(dataDir,resultsDir);
	}
}
