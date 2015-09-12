package validate.trackrect;

import georegression.struct.shapes.Rectangle2D_F64;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class EvaluateResultsTldData {

	public static boolean formatLatex = false;

	public void evaluate(String dataName, String inputFile , PrintStream out ) throws IOException {
		System.out.println("Processing "+dataName);

		String path = "data/track_rect/TLD/"+dataName;

		Rectangle2D_F64 expected = new Rectangle2D_F64();
		Rectangle2D_F64 found = new Rectangle2D_F64();

		BufferedReader readerTruth = new BufferedReader(new FileReader(path+"/gt.txt"));
		BufferedReader readerRect;
		try {
			readerRect = new BufferedReader(new FileReader(inputFile));
		} catch( IOException e ) {
			System.err.println("Can't find file. Skipping.  "+inputFile);
			return;
		}

		TldResults stats = new TldResults();
		FooResults statsFoo = new FooResults();

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
			UtilTldData.updateStatistics(expected,found,statsFoo);

			System.out.println(imageNum+"  "+found.p0.x);
			imageNum++;
		}

		double averageOverlap = statsFoo.totalOverlap/statsFoo.truePositive;
		if( formatLatex ) {
			if( dataName.contains("_")) {
				String a[] =  dataName.split("_");
				dataName = a[0] +"\\_"+ a[1];
			}
			out.println("\\hline");
			out.printf("%s & %5.2f & %d & %d & %d & %d & %5.2f\\\\\n", dataName, UtilTldData.computeFMeasure(stats), stats.truePositives,
					stats.trueNegatives, stats.falsePositives, stats.falseNegatives, averageOverlap);
		} else {
			out.printf("%s %5.3f %04d %04d %04d %04d %5.3f\n", dataName, UtilTldData.computeFMeasure(stats), stats.truePositives,
					stats.trueNegatives, stats.falsePositives, stats.falseNegatives, averageOverlap);
		}

		System.out.println();
	}

	public static void process( String path , String library ) throws IOException {
		EvaluateResultsTldData evaluator = new EvaluateResultsTldData();

		System.out.println("------------ "+library+" ------------------");
		PrintStream out = new PrintStream(path+"TldData_"+library+".txt");
		if( formatLatex ) {
			out.println("\\begin{tabular}{|l|c|c|c|c|c|c|}");
			out.println("\\hline");
			out.println("\\multicolumn{7}{|c|}{TLD Data} \\\\");
			out.println("\\hline");
			out.println("Scenario & F & TP & TN & FP & FN & Overlap \\\\");
		} else {
			out.println("# F TP TN FP FN Overlap");
		}

		for( String dataset : GenerateDetectionsTldData.videos ){
			String inputFile = "tmp/"+library+"_"+dataset+".txt";
			evaluator.evaluate(dataset, inputFile, out);
		}

		if( formatLatex ) {
			out.println("\\hline");
			out.println("\\end{tabular}");
		}
	}

	public static void main(String[] args) throws IOException {
		formatLatex = true;
//		process("./","BoofCV-Comaniciu");
		process("./","BoofCV-SparseFlow");
	}
}
