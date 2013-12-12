package validate.trackrect;

import georegression.struct.shapes.RectangleCorner2D_F64;

import java.io.*;

import static validate.trackrect.GenerateDetectionsMilTrackData.parseFramesFile;

/**
 * @author Peter Abeles
 */
public class EvaluateResultsMilTrackData {


	public void evaluate(String dataName, String inputName, PrintStream out) throws IOException {
		System.out.println("Processing "+dataName);

		String path = "data/track_rect/MILTrack/"+dataName;

		RectangleCorner2D_F64 expected = new RectangleCorner2D_F64();
		RectangleCorner2D_F64 found = new RectangleCorner2D_F64();

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

			boolean hasTruth = !(expected.x0 == 0 && expected.y0 == 0 && expected.x1 == 0 && expected.y1 == 0);

			if( hasTruth ) {
				UtilTldData.updateStatistics(expected,found,stats);
				UtilTldData.updateStatistics(expected,found,statsFoo);

//				System.out.printf("  F = %6.3f      TP %5d TN %5d      FP %5d FN %5d\n",
//						UtilTldData.computeFMeasure(stats),stats.truePositives,stats.trueNegatives,stats.falsePositives,stats.falseNegatives);
			}
			frameNum++;
		}
		double averageOverlap = statsFoo.totalOverlap/statsFoo.truePositive;
		out.printf("%s %5.3f %04d %04d %04d %04d %5.3f\n", dataName, UtilTldData.computeFMeasure(stats), stats.truePositives,
				stats.trueNegatives, stats.falsePositives, stats.falseNegatives, averageOverlap);
	}

	public static void process( String path , String library ) throws IOException {
		EvaluateResultsMilTrackData evaluator = new EvaluateResultsMilTrackData();

		System.out.println("------------ "+library+" ------------------");
		PrintStream out = new PrintStream(path+"MILTrackData_"+library+".txt");
		out.println("# F TP TN FP FN Overlap");

		for( String dataset : GenerateDetectionsMilTrackData.videos ){
			String inputFile = library+"_"+dataset+".txt";
			evaluator.evaluate(dataset, inputFile, out);
		}
	}

	public static void main(String[] args) throws IOException {

//		String libraries[]=new String[]{"BoofCV-TLD","BoofCV-Circulant","BoofCV-CirculantOrig","BoofCV-SFT","BoofCV-Comaniciu","PCirculant"};

		process("./", "BoofCV-Circulant");


		System.out.println("DONE!");
		System.exit(0);
	}
}
