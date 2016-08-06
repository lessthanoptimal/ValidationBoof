package validate.shape;

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

	/**
	 * Fraction of contour size a corner needs to be within to be considered a match
	 */
	public static final double MATCH_TOLERANCE = 0.1;

	public static final double MATCH_BIAS_PIXELS = 2.0;

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErr(PrintStream err) {
		this.err = err;
	}

	public void evaluate( File dataDir , File resultsDir ) {


		outputResults.println("# Match Tolerance in contour fraction = "+MATCH_TOLERANCE);
		outputResults.println("# Match Tolerance bias pixels         = "+MATCH_BIAS_PIXELS);
		outputResults.println("# Image (expected) (detected) (multiple) (miss matched) (false positives) (false negative) (average error)");

		List<File> files = Arrays.asList(dataDir.listFiles());

		Collections.sort(files);

		for( File f : files ) {
			if( !(f.getName().endsWith("jpg")||f.getName().endsWith("png")))
				continue;

			String nameResults = UtilShapeDetector.imageToDetectedName(f.getName());
			String nameTruth = f.getName().substring(0,f.getName().length()-3)+"txt";

			List<Polygon2D_F64> truth = loadTruth(new File(dataDir, nameTruth));
			List<Polygon2D_F64> found = UtilShapeDetector.loadResults(new File(resultsDir,nameResults));

			evaluateFile(f.getName(), truth, found);
		}

		outputResults.flush();
	}

	private void evaluateFile(String fileName, List<Polygon2D_F64> truth, List<Polygon2D_F64> found) {
		int numMultiple = 0;
		int numMissMatched = 0;
		int falsePositives = 0;

		boolean matchedFound[] = new boolean[found.size()];

		double totalError = 0;
		int totalTruthMatched = 0;

		for( Polygon2D_F64 t : truth ) {
			double tolerancePixels = size(t)*MATCH_TOLERANCE+MATCH_BIAS_PIXELS;

			List<Polygon2D_F64> matches = new ArrayList<Polygon2D_F64>();

			int minMatches = t.size()/2 + 1;
			for( int i = 0; i < found.size(); i++ ) {
				Polygon2D_F64 f = found.get(i);

				if( countMatchedCorners(t,f,tolerancePixels) >= minMatches ) {
					matches.add(f);
					matchedFound[i] = true;
				}
			}

			boolean missMatched = true;
			double bestError = Double.MAX_VALUE;
			for( Polygon2D_F64 p : matches ) {
				if( p.size() == t.size() ) {
					missMatched = false;
				}

				double error = computeError(t,p,tolerancePixels);
				if( error < bestError ) {
					bestError = error;
				}
			}

			if( matches.size() > 1 ) {
				numMultiple += matches.size()-1;
			}
			if( matches.size() > 0 ) {
				totalError += bestError;
				totalTruthMatched++;
			}

			if( matches.size()>0 && missMatched ) {
				numMissMatched++;
			}

		}

		for (int i = 0; i < matchedFound.length; i++) {
			if( !matchedFound[i] ) {
				falsePositives++;
			}
		}

		totalError /= totalTruthMatched;
		int numFalseNegative = truth.size()-totalTruthMatched;

		outputResults.printf("%-15s %2d %2d %2d %2d %2d %2d %7.4f\n", fileName, truth.size(), found.size(),
				numMultiple, numMissMatched, falsePositives, numFalseNegative, totalError);
	}

	private static double size( Polygon2D_F64 p ) {
		double total = 0;
		for (int i = 0,j = p.size()-1; i < p.size();j=i, i++) {
			total += p.get(i).distance(p.get(j));
		}
		return total;
	}

	protected int countMatchedCorners( Polygon2D_F64 a , Polygon2D_F64 b , double tol ) {

		int totalMatched=0;
		for (int i = 0; i < a.size(); i++) {
			Point2D_F64 pt_a = a.get(i);
			for (int j = 0; j < b.size(); j++) {
				Point2D_F64 pt_b = b.get(j);

				if( pt_a.distance(pt_b) <= tol ) {
					totalMatched++;
					break;
				}
			}
		}

		return totalMatched;
	}

	protected double computeError( Polygon2D_F64 a , Polygon2D_F64 b , double tol ) {

		int totalMatched=0;
		double totalError=0;
		for (int i = 0; i < a.size(); i++) {
			Point2D_F64 pt_a = a.get(i);

			double bestError = Double.MAX_VALUE;
			for (int j = 0; j < b.size(); j++) {
				Point2D_F64 pt_b = b.get(j);

				double error = pt_a.distance(pt_b);
				if( error < bestError ) {
					bestError = error;
				}
			}
			if( bestError <= tol ) {
				totalError += bestError;
				totalMatched++;
			}
		}

		return totalError/totalMatched;
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
