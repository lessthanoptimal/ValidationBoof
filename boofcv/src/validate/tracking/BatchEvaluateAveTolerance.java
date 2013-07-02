package validate.tracking;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.homo.Homography2D_F64;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import static validate.tracking.BatchEvaluateSummaryAndTime.pathToData;

/**
 * Computes the average performance metrics across the whole trajectory for a different inlier tolerances
 *
 * @author Peter Abeles
 */
public class BatchEvaluateAveTolerance {

	private static void computeResults(double[] tolerances, Class imageType,
									   String whichData, int skip,
									   PrintStream out)
			throws FileNotFoundException
	{
		for( double tol : tolerances ) {

			SimpleImageSequence sequence =
					DefaultMediaManager.INSTANCE.openVideo(pathToData + whichData+"_undistorted.mjpeg", ImageDataType.single(imageType));

			List<Homography2D_F64> groundTruth = LogParseHomography.parse(pathToData + whichData + "_homography.txt");

			EvaluateTrackerStability app = new EvaluateTrackerStability(tol,skip);

			FactoryEvaluationTrackers trackers = new FactoryEvaluationTrackers(imageType);
//			EvaluationTracker tracker = trackers.createSurf(false);
			EvaluationTracker tracker = trackers.createFhSurfKlt();
//			EvaluationTracker tracker = trackers.createKlt();

			app.evaluate(tracker,sequence,groundTruth,null);

			out.printf("%6.3f %10.4f %10.4f %10.4f %10.4f %10.4f %8.2f\n",
					tol,app.getMeanF(),app.getMeanFA(),app.getMeanPrecision(),app.getMeanRecallA(),app.getMeanRecall(),app.getMeanTrackCount());
		}
	}

	public static void main( String args[] ) throws FileNotFoundException {
		int skips[] = new int[]{1,4,8};
		double tolerances[] = new double[]{0.1,0.2,0.5,1,1.5,2,3,5,10,15,20,30,40,50};

		String dir = "carpet";

//		String dataSets[] = new String[]{"skew","rotate2","move_out","move_in"};
		String dataSets[] = new String[]{"move_in"};

		Class imageType = ImageFloat32.class;

		for( String whichData : dataSets ) {
			for( int skip : skips )  {
				String path = dir +"/"+whichData;
				PrintStream out = new PrintStream(new FileOutputStream("PR_"+whichData+"_"+skip+".txt"));
				out.println("# Tolerance (mean F) (mean F always inside) (mean precision) (mean recall always inside) (mean recall inside) (mean tracks count)");
				out.println("# File: "+path+"  skip "+skip);
				computeResults(tolerances, imageType, path, skip, out);
				out.close();
			}
		}
	}
}
