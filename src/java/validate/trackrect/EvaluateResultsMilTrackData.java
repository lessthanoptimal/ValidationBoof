package validate.trackrect;

import georegression.struct.shapes.Rectangle2D_F64;

import java.io.*;

import static validate.trackrect.GenerateDetectionsMilTrackData.parseFramesFile;

/**
 * @author Peter Abeles
 */
public class EvaluateResultsMilTrackData {

	public static boolean formatLatex = false;

	public void evaluate(String dataName, String inputName, PrintStream out) throws IOException {
		System.out.println("Processing "+dataName);

		String path = "data/track_rect/MILTrack/"+dataName;

		Rectangle2D_F64 expected = new Rectangle2D_F64();
		Rectangle2D_F64 found = new Rectangle2D_F64();

		File resultsFile = new File(inputName);

		if( resultsFile.length() == 0 ) {
			System.out.println("Empty file, skipping");
			return;
		}

		BufferedReader readerTruth = new BufferedReader(new FileReader(path + "/" + dataName + "_gt.txt"));
		BufferedReader readerRect = new BufferedReader(new FileReader(resultsFile));

		TldResults stats = new TldResults();
		FooResults statsFoo = new FooResults();

		int frames[] = parseFramesFile(path + "/" + dataName + "_frames.txt");
		int frameNum = frames[0];

		while( frameNum <= frames[1] ) {
			UtilTldData.parseRectWH(readerTruth.readLine(),expected);
			UtilTldData.parseFRect(readerRect.readLine(), found);

			boolean hasTruth = !(expected.p0.x == 0 && expected.p0.y == 0 && expected.p1.x == 0 && expected.p1.y == 0);

			if( hasTruth ) {
				UtilTldData.updateStatistics(expected,found,stats);
				UtilTldData.updateStatistics(expected,found,statsFoo);

//				System.out.printf("  F = %6.3f      TP %5d TN %5d      FP %5d FN %5d\n",
//						UtilTldData.computeFMeasure(stats),stats.truePositives,stats.trueNegatives,stats.falsePositives,stats.falseNegatives);
			}
			frameNum++;
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
	}

	public static void process( String path , String library ) throws IOException {
		EvaluateResultsMilTrackData evaluator = new EvaluateResultsMilTrackData();

		System.out.println("------------ "+library+" ------------------");
		PrintStream out = new PrintStream(new File(path,"MILTrackData_"+library+".txt"));
		if( formatLatex ) {
			out.println("\\begin{tabular}{|l|c|c|c|c|c|c|}");
			out.println("\\hline");
			out.println("\\multicolumn{7}{|c|}{MILTrack} \\\\");
			out.println("\\hline");
			out.println("Scenario & F & TP & TN & FP & FN & Overlap \\\\");
		} else {
			out.println("# F TP TN FP FN Overlap");
		}

		for( String dataset : GenerateDetectionsMilTrackData.videos ){
			String inputFile = library+"_"+dataset+".txt";
			evaluator.evaluate(dataset, inputFile, out);
		}

		if( formatLatex ) {
			out.println("\\hline");
			out.println("\\end{tabular}");
		}
	}

	public static void main(String[] args) throws IOException {

//		String libraries[]=new String[]{"BoofCV-TLD","BoofCV-Circulant","BoofCV-CirculantOrig","BoofCV-SFT","BoofCV-Comaniciu","PCirculant"};

		formatLatex = true;

//		process("./tmp", "BoofCV-Comaniciu");
		process("./tmp", "BoofCV-SparseFlow");


		System.out.println("DONE!");
		System.exit(0);
	}
}
