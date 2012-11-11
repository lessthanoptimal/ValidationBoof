package validate.tracking;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.homo.Homography2D_F64;
import validate.ValidationConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * Outputs performance metrics for each frame in the video and as an average across the whole data set
 *
 * @author Peter Abeles
 */
public class BatchEvaluateSummaryAndTime {

	public static String pathToData = ValidationConstants.PATH_DATA+"abeles2012/";
	public static double tolerance = 5;

	private static void createDirectories( String dir ) {
		if( !new File("results").exists() && !new File("results").mkdir() )
			throw new RuntimeException("Can't create results directory");

		if( !new File("results/"+dir).exists() && !new File("results/"+dir).mkdir() )
			throw new RuntimeException("Can't create results/"+dir+" directory");

		String dirTime = "results/"+dir+"/time";

		if( !new File(dirTime).exists() && !new File(dirTime).mkdir() )
			throw new RuntimeException("Can't create "+dirTime+" directory");
	}

	private static void computeResults(Class imageType,
									   EvaluatedAlgorithm whichAlg,
									   String whichData, int skip,
									   PrintStream outSummary, PrintStream outTime)
			throws FileNotFoundException
	{
		SimpleImageSequence sequence =
				DefaultMediaManager.INSTANCE.openVideo(pathToData+whichData+"_undistorted.mjpeg",imageType);

		List<Homography2D_F64> groundTruth = LogParseHomography.parse(pathToData + whichData + "_homography.txt");

		EvaluateTrackerStability app = new EvaluateTrackerStability(tolerance,skip);

		FactoryEvaluationTrackers trackers = new FactoryEvaluationTrackers(imageType);

		EvaluationTracker tracker = trackers.create(whichAlg);

		app.evaluate(tracker,sequence,groundTruth,outTime);

		outSummary.printf("%s %2d %6.3f %6.3f %6.3f %6.3f %6.3f %6.1f\n", whichData, skip,
				app.getMeanF(), app.getMeanFA(), app.getMeanPrecision(), app.getMeanRecall(), app.getMeanRecallA(),
				app.getMeanTrackCount());
	}

	public static void main( String args[] ) throws FileNotFoundException {
		int skips[] = new int[]{1,4,8};
		String dir = "various";

		String dataSets[] = new String[]{"lighting","compressed","urban"};
//		String dataSets[] = new String[]{"skew","rotate","move_out","move_in"};
//		String dataSets[] = new String[]{"urban"};

		Class imageType = ImageFloat32.class;

		createDirectories(dir);

		for( EvaluatedAlgorithm alg : EvaluatedAlgorithm.values() ) {
//		EvaluatedAlgorithm alg = EvaluatedAlgorithm.FH_SURF_KLT;
			System.out.println("Evaluating "+alg);

			// create the summary file
			PrintStream outSummary = new PrintStream(new FileOutputStream("results/"+dir+"/SummaryResults_"+alg+".txt"));
			outSummary.println("# Inlier Tolerance " + tolerance + "  Algorithm " + alg);
			outSummary.println("# (File) (Skip) (F) (F all inside) (Precision) (Recall) (Recall all inside) (Tracks)");

			for( String whichData : dataSets ) {
				for( int skip : skips )  {
					String path = dir +"/"+whichData;

					// create the results versus time file
					File dirTime = new File("results/"+dir+"/time/"+alg);
					if( !dirTime.exists() && !dirTime.mkdir() )
						throw new RuntimeException("Couldn't created directory: "+dirTime.getPath());

					PrintStream outTime = new PrintStream(new FileOutputStream(new File(dirTime,"ResultsTime_"+whichData+"_"+skip+".txt")));

					outTime.println("# File: " + path + " Algorithm = " + alg + "  skip " + skip + " tolerance = " + tolerance);

					computeResults(imageType, alg , path, skip, outSummary, outTime);
				}
			}

			outSummary.close();
		}
	}
}
