package boofcv.metrics.point;

import boofcv.alg.misc.ImageCoverage;
import boofcv.gui.image.ImagePanel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.struct.GrowQueue_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static boofcv.regression.PointTrackerRegression.pathToData;

/**
 * @author Peter Abeles
 */
public class EvaluateTrackerStability<T extends ImageGray<T>> {

	// list of tracks that have never gone outside the image's FOV
	List<Point2D_F64> alwaysInside;
	// number of features that were originally detected
	int originalSize;

	int dropRate;

	// tolerance for how close a feature needs to be
	double tol;

	// used to compute the area covered by image features
	// Image area is used as a sanity check. There was a bug where a tracker returned the same point 500 times and it
	// scored very well.
	ImageCoverage coverage = new ImageCoverage();

	Point2D_F64 p = new Point2D_F64();

	double meanPrecision;
	double meanRecallA;
	double meanRecall;
	double meanTrackCount;
	double meanF;
	double meanFA;
	double meanImageArea;

	// keeps track of per frame processing time
	public GrowQueue_F64 elapsedTimeMS = new GrowQueue_F64();

	BufferedImage imageDebug = null;// = new BufferedImage(640,480,BufferedImage.TYPE_INT_BGR);
	ImagePanel gui;

	public EvaluateTrackerStability(double tol, int skip ) {
		this.tol = tol;
		this.dropRate = skip;

//		gui = ShowImages.showWindow(imageDebug,"Debug");
	}

	public void evaluate( EvaluationTracker<T> tracker ,
						  SimpleImageSequence<T> sequence ,
						  List<Homography2D_F64> transforms ,
						  PrintStream out ) {
		elapsedTimeMS.reset();
		if( out != null )
			out.println("# (time tick) ( F ) ( F all inside) (precision) (recall) (recall all inside) (tracks)");

		alwaysInside = new ArrayList<>();
		int totalFrames = 0;
		Point2D_F64 p = new Point2D_F64();

		double sumPrecision = 0;
		double sumRecallA = 0;
		double sumRecall = 0;
		double sumF = 0;
		double sumFA = 0;
		double sumArea = 0;
		int sumTotalTracks = 0;

		int totalEvaluations = 0;

		tracker.reset();

		while( sequence.hasNext() ) {

			if( totalFrames >= transforms.size() ) {
				System.out.println("Egads");
				continue;
			}

			Homography2D_F64 H = transforms.get(totalFrames);

			T image = sequence.next();

			coverage.reset(500,image.width, image.height);

			if( totalFrames % dropRate == 0 ) {
				long time0 = System.nanoTime();
				tracker.track(image);
				long time1 = System.nanoTime();
				elapsedTimeMS.add((time1-time0)*1e-6); // record how long it to process a single frame

				if( totalFrames == 0 ) {
					List<Point2D_F64> tracks = tracker.getInitial();
					for( Point2D_F64 t : tracks ) {
						alwaysInside.add(t.copy());
					}
					originalSize = tracks.size();
				} else {

					// remove points which are outside the image
					for( int i = 0; i < alwaysInside.size();) {
						Point2D_F64 t = alwaysInside.get(i);
						HomographyPointOps_F64.transform(H,t,p);

						if( p.x < 0 || p.y < 0 || p.x >= image.width || p.y >= image.height ) {
							alwaysInside.remove(i);
						} else {
							i++;
						}
					}

					if( alwaysInside.size() == 0 )
						throw new RuntimeException("All original features have been dropped!");

					if( gui != null )
						visualizeTracks(H);

					List<Point2D_F64> initial = tracker.getInitial();
					List<Point2D_F64> current = tracker.getCurrent();

					for( Point2D_F64 pixel : current ) {
						coverage.markPixel((int)pixel.x,(int)pixel.y);
					}

					double truePositive = computeTruePositives(initial,current,H,false);
					double truePositiveA = computeTruePositives(initial,current,H,true);

					double precision = truePositive/(double)initial.size();
					double recallA = truePositiveA/(double)alwaysInside.size();
					double recall = truePositive/(double)originalSize;

					coverage.process();
					double imageArea = coverage.getFraction();

					double FA = 2.0*(precision*recallA)/(precision+recallA);
					double F = 2.0*(precision*recall)/(precision+recall);

					if( Double.isNaN(FA)) FA = 0;
					if( Double.isNaN(F)) F = 0;

					if( out != null )
						out.printf("%04d %6.3f %6.3f %6.3f %6.3f %6.3f %d\n",totalFrames,F,FA,
								precision,recall,recallA,initial.size());

					System.out.printf("%4d  F = %6.3f FA = %6.3f NT = %3d NA = %3d AREA=%2d\n",
							totalFrames,F,FA,initial.size(),alwaysInside.size(),(int)(100.0*imageArea));

					sumPrecision += precision;
					sumRecallA += recallA;
					sumRecall += recall;
					sumF += F;
					sumFA += FA;
					totalEvaluations++;
					sumTotalTracks += initial.size();
					sumArea += imageArea;
				}
			}

			totalFrames++;
//			System.out.println("Frame: " + totalFrames + "  tracks " + tracker.getCurrent().size());
		}

		// the first frame is not scored
		meanPrecision = sumPrecision / totalEvaluations;
		meanRecallA = sumRecallA / totalEvaluations;
		meanRecall = sumRecall / totalEvaluations;
		meanTrackCount = sumTotalTracks / totalEvaluations;
		meanF = sumF / totalEvaluations;
		meanFA = sumFA / totalEvaluations;
		meanImageArea = sumArea / totalEvaluations;

		System.out.println();
		System.out.printf("Mean Summary:  F %7.3f  FA %7.3f \n",meanF,meanFA);
	}

	private void visualizeTracks( Homography2D_F64 H ) {
		Graphics2D g2 = imageDebug.createGraphics();

		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,640,480);


		g2.setColor(Color.RED);
		int r = 2;

		// remove points which are outside the image
		for( int i = 0; i < alwaysInside.size(); i++) {
			Point2D_F64 t = alwaysInside.get(i);
			HomographyPointOps_F64.transform(H,t,p);

			int x = (int)p.x;
			int y = (int)p.y;

			g2.drawOval(x-r,y-r,2*r+1,2*r+1);
		}

		gui.repaint();
	}

	private double computePrecision( List<Point2D_F64> initial ,
									 List<Point2D_F64> current ,
									 Homography2D_F64 H ) {
		int accurate = 0;
		for( int i = 0; i < initial.size(); i++ ) {
			Point2D_F64 orig = initial.get(i);
			Point2D_F64 found = current.get(i);

			// compute the expected location given "ground truth"
			HomographyPointOps_F64.transform(H, orig, p);

			double error = p.distance2(found);
			if( error <= tol*tol ) {
				accurate++;
			}
		}

		return (double)accurate/(double)initial.size();
	}

	private int computeTruePositives( List<Point2D_F64> initial ,
									  List<Point2D_F64> current ,
									  Homography2D_F64 H ,
									  boolean onlyInside ) {
		int accurate = 0;
		for( int i = 0; i < initial.size(); i++ ) {
			Point2D_F64 orig = initial.get(i);

			if( onlyInside && !isContained( orig , alwaysInside) )
				continue;

			Point2D_F64 found = current.get(i);

			// compute the expected location given "ground truth"
			HomographyPointOps_F64.transform(H, orig, p);

			double error = p.distance2(found);
			if( error <= tol*tol ) {
				accurate++;
			}
		}

		return accurate;
	}

	private boolean isContained( Point2D_F64 point , List<Point2D_F64> list ) {
		for( Point2D_F64 p : list ) {
			if( p.x == point.x && p.y == point.y )
				return true;
		}
		return false;
	}

	public double getMeanPrecision() {
		return meanPrecision;
	}

	public double getMeanRecall() {
		return meanRecall;
	}

	public double getMeanRecallA() {
		return meanRecallA;
	}

	public double getMeanTrackCount() {
		return meanTrackCount;
	}

	public double getMeanF() {
		return meanF;
	}

	public double getMeanFA() {
		return meanFA;
	}

	public double getMeanImageArea() {
		return 100*meanImageArea;
	}

	public static void main(String args[] ) throws FileNotFoundException {

		Class imageType = GrayF32.class;

		String whichData = "bricks/skew";
//		String whichData = "bricks/rotate2";
//		String whichData = "bricks/move_out";
//		String whichData = "bricks/move_in";

		SimpleImageSequence sequence =
				DefaultMediaManager.INSTANCE.openVideo(pathToData+whichData+"_undistorted.mjpeg", ImageType.single(imageType));

		List<Homography2D_F64> groundTruth = LogParseHomography.parse(pathToData+whichData+"_homography.txt");

		EvaluateTrackerStability app = new EvaluateTrackerStability(5,1);

		FactoryEvaluationTrackers trackers = new FactoryEvaluationTrackers(imageType);
//		EvaluationTracker tracker = trackers.createSurf(false);
//		EvaluationTracker tracker = trackers.createSurfWithScale(false);
//		EvaluationTracker tracker = trackers.createFused();
		EvaluationTracker tracker = trackers.create(EvaluatedAlgorithm.KLT);

		app.evaluate(tracker,sequence,groundTruth,null);
	}
}
