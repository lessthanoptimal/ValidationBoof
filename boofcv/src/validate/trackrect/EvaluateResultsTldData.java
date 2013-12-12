package validate.trackrect;

import georegression.struct.shapes.RectangleCorner2D_F64;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class EvaluateResultsTldData {
	public void evaluate(String dataName, String inputFile , PrintStream out ) throws IOException {
		System.out.println("Processing "+dataName);

		String path = "data/track_rect/TLD/"+dataName;

		RectangleCorner2D_F64 expected = new RectangleCorner2D_F64();
		RectangleCorner2D_F64 found = new RectangleCorner2D_F64();

		BufferedReader readerTruth = new BufferedReader(new FileReader(path+"/gt.txt"));
		BufferedReader readerRect;
		try {
			readerRect = new BufferedReader(new FileReader(inputFile));
		} catch( IOException e ) {
			System.err.println("Can't find file. Skipping.  "+inputFile);
			return;
		}

		TldResults stats = new TldResults();

		int imageNum = 0;

		while( true ) {
			String truth = readerTruth.readLine();
			if( truth == null)
				break;

			String foundString = readerRect.readLine();
			if( foundString == null) {
				System.err.println("Partial results file");
				return;
			}


			UtilTldData.parseRect(truth,expected);
			UtilTldData.parseRect(foundString,found);

			UtilTldData.updateStatistics(expected,found,stats);

			System.out.println(imageNum+"  "+found.x0);
			imageNum++;
		}

		out.printf("%s %5.3f %04d %04d %04d %04d\n", dataName, UtilTldData.computeFMeasure(stats), stats.truePositives,
				stats.trueNegatives, stats.falsePositives, stats.falseNegatives);

		System.out.println();
	}

	public static void process( String path , String library ) throws IOException {
		EvaluateResultsTldData evaluator = new EvaluateResultsTldData();

		System.out.println("------------ "+library+" ------------------");
		PrintStream out = new PrintStream(path+"TldData_"+library+".txt");
		out.println("# F TP TN FP FN");

		for( String dataset : GenerateDetectionsTldData.videos ){
			String inputFile = library+"_"+dataset+".txt";
			evaluator.evaluate(dataset, inputFile, out);
		}
	}

	public static void main(String[] args) throws IOException {

	}
}
