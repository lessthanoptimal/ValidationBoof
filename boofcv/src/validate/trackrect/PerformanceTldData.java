package validate.trackrect;

import georegression.struct.shapes.RectangleCorner2D_F64;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class PerformanceTldData {

	String detectionPath;
	String name;


	public void evaluateAll( String detectionPath , String name ) throws IOException {
		this.detectionPath = detectionPath;
		this.name = name;

		PrintStream output = new PrintStream(name+"_performance.txt");
		output.println("# F-measure TruePositive TrueNegative FalsePositive FalseNegative");

		evaluate("01_david", output);
		evaluate("02_jumping", output);
		evaluate("03_pedestrian1", output);
		evaluate("04_pedestrian2", output);
		evaluate("05_pedestrian3", output);
		evaluate("06_car", output);
		evaluate("07_motocross", output);
		evaluate("08_volkswagen", output);
		evaluate("09_carchase",  output);
		evaluate("10_panda", output);

		output.close();
	}

	private void evaluate(String dataName, PrintStream output ) throws IOException {
		System.out.println("Processing "+dataName);

		String inputFile = detectionPath +"/"+name+"_"+dataName+".txt";

		String path = "../data/track_rect/TLD/"+dataName;

		RectangleCorner2D_F64 expected = new RectangleCorner2D_F64();
		RectangleCorner2D_F64 found = new RectangleCorner2D_F64();

		BufferedReader readerTruth = new BufferedReader(new FileReader(path+"/gt.txt"));
		BufferedReader readerRect = new BufferedReader(new FileReader(inputFile));

		TldResults stats = new TldResults();

		while( true ) {
			String lineTruth = readerTruth.readLine();
			if( lineTruth == null )
				break;

			String lineFound = readerRect.readLine();
			if( lineFound == null )
				throw new RuntimeException("Found is missing data");

			UtilTldData.parseRect(lineTruth, expected);
			UtilTldData.parseRect(lineFound,found);

			UtilTldData.updateStatistics(expected,found,stats);

			System.out.printf("  F = %6.3f      TP %5d TN %5d      FP %5d FN %5d\n",
					UtilTldData.computeFMeasure(stats),stats.truePositives,stats.trueNegatives,stats.falsePositives,stats.falseNegatives);

		}

		System.out.println("F-measure: "+UtilTldData.computeFMeasure(stats));
		System.out.println();

		output.printf("%-20s %6.4f %5d %5d %5d %5d\n", dataName,
				UtilTldData.computeFMeasure(stats),
				stats.truePositives,stats.trueNegatives,stats.falsePositives,stats.falseNegatives);

	}




	public static void main(String[] args) throws IOException {

		PerformanceTldData performance = new PerformanceTldData();

//		performance.evaluateAll("../thirdparty/opentld_c","copentld");
		performance.evaluateAll("./","BoofCV");

		System.out.println("DONE!");
	}
}
