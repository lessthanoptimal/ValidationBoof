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

	List<PolygonTruthIndexes> descriptions;

	PrintStream outputResults = System.out;
	PrintStream err = System.err;

	/**
	 * Maximum number of pixels away two points can be to be matched
	 */
	public static final double MATCH_TOLERANCE = 10.0;

	public void setOutputResults(PrintStream outputResults) {
		this.outputResults = outputResults;
	}

	public void setErr(PrintStream err) {
		this.err = err;
	}

	public void evaluate( File dataDir , File resultsDir ) {
		outputResults.println("# Image (multiple) (false positives) (false negative) (average error)");
		descriptions = UtilShapeDetector.loadDescription(new File(dataDir,"description.txt"));

		List<File> files = Arrays.asList(dataDir.listFiles());

		Collections.sort(files);

		for( File f : files ) {
			if( !f.getName().endsWith("jpg"))
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
		int falsePositives = 0;

		boolean matchedFound[] = new boolean[found.size()];

		double totalError = 0;
		int totalTruthMatched = 0;

		for( Polygon2D_F64 t : truth ) {
			int numMatch = 0;
			double bestError = MATCH_TOLERANCE;

			for( int i = 0; i < found.size(); i++ ) {
				Polygon2D_F64 f = found.get(i);

				if( t.size() == f.size() ) {
					double error = error(t,f);
					if( error < MATCH_TOLERANCE ) {
						matchedFound[i] = true;
						numMatch++;
						if (error < bestError) {
							bestError = error;
						}
					}
				}
			}

			if( numMatch > 1 ) {
				numMultiple += numMatch-1;
			}
			if( numMatch > 0 ) {
				totalError += bestError;
				totalTruthMatched++;
			}
		}

		for (int i = 0; i < matchedFound.length; i++) {
			if( !matchedFound[i] ) {
				falsePositives++;
			}
		}

		totalError /= totalTruthMatched;
		int numFalseNegative = truth.size()-totalTruthMatched;

		outputResults.printf("%15s %2d %2d %2d %7.4f\n",fileName,numMultiple,falsePositives,numFalseNegative,totalError);
	}

	protected double error( Polygon2D_F64 a , Polygon2D_F64 b ) {
		b = b.copy();

		// make sure they are both rotating the same way
		if( a.isCCW() != b.isCCW() )
			b.flip();

		double bestError = Double.MAX_VALUE;
		for (int offset = 0; offset < a.size(); offset++) {
			double totalError = 0;
			for (int i = 0; i < a.size(); i++) {
				int j = (i+offset)%a.size();

				totalError += a.get(i).distance(b.get(j));
			}
			if( totalError < bestError )
				bestError = totalError;
		}
		return bestError/a.size();
	}

	protected List<Polygon2D_F64> loadTruth( File fileTruth ) {
		List<Point2D_F64> points = PointFileCodec.load(fileTruth);

		List<Polygon2D_F64> polygons = new ArrayList<Polygon2D_F64>();

		for (int i = 0; i < descriptions.size(); i++) {
			int[] indexes = descriptions.get(i).indexes;

			Polygon2D_F64 p = new Polygon2D_F64(indexes.length);

			for (int j = 0; j < p.size(); j++) {
				p.vertexes.data[j] = points.get(indexes[j]);
			}

			polygons.add(p);
		}

		return polygons;
	}

	public static void main(String[] args) {
		EvaluatePolygonDetector app = new EvaluatePolygonDetector();

		File dataDir = new File("data/shape/set01");
		File resultsDir = new File("tmp");

		app.evaluate(dataDir,resultsDir);
	}
}
