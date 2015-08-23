package validate.tracking;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static validate.tracking.BatchEvaluateSummaryAndTime.pathToData;

/**
 * Runs through homography ground truth log and visualizes the data for debugging purposes.
 * The current image 'i' in the sequence is distorted so that
 *
 * @author Peter Abeles
 */
// TODO compute pixel error weighted by edges
public class VisualDebugHomographyTruth<T extends ImageSingleBand> implements MouseListener {

	T workImage;

	Point2D_F64 c0 = new Point2D_F64(0,0);
	Point2D_F64 c1 = new Point2D_F64(0,0);
	Point2D_F64 c2 = new Point2D_F64(0,0);
	Point2D_F64 c3 = new Point2D_F64(0,0);

	boolean paused = false;
	boolean showLines = true;

	public VisualDebugHomographyTruth(Class<T> imageType) {
		workImage = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
	}

	public void process( SimpleImageSequence<T> sequence , List<Homography2D_F64> transforms ,
						 EvaluationTracker<T> tracker ) {

		T keyFrame = (T)sequence.next().clone();
		workImage.reshape(keyFrame.width,keyFrame.height);

		BufferedImage out = new BufferedImage(keyFrame.width,keyFrame.height,BufferedImage.TYPE_INT_BGR);

		ImagePanel gui = new ImagePanel();
		gui.setPreferredSize(new Dimension(keyFrame.width,keyFrame.height));
		gui.addMouseListener(this);

		ShowImages.showWindow(gui,"Transformed image sequence");

		List<Point2D_F64> allFeatures = new ArrayList<Point2D_F64>();
		if( tracker != null ) {
			tracker.track(keyFrame);
			for( Point2D_F64 p : tracker.getInitial() ) {
				allFeatures.add( p.copy() );
			}
		}

		int index = 1;
		while( sequence.hasNext() ) {
			// transform in log is from key to image, need the opposite
			Homography2D_F64 H = transforms.get(index);
			Homography2D_F64 H_inv = H.invert(null);
			T frame = sequence.next();

			// draw the keyframe as the background
			workImage.setTo(keyFrame);
//			GeneralizedImageOps.fill(workImage,0);

			// render the transformed image
			DistortImageOps.distortSingle(frame, workImage, new PixelTransformHomography_F32(H),
					TypeInterpolate.BILINEAR, BorderType.EXTENDED);

			ConvertBufferedImage.convertTo(workImage,out,true);

			Graphics2D g2 = out.createGraphics();

			HomographyPointOps_F64.transform(H_inv,0,0,c0);
			HomographyPointOps_F64.transform(H_inv,frame.width,0,c1);
			HomographyPointOps_F64.transform(H_inv,frame.width,frame.height,c2);
			HomographyPointOps_F64.transform(H_inv,0,frame.height,c3);

			if( showLines ) {
				g2.setColor(Color.ORANGE);
				g2.setStroke(new BasicStroke(3));

				g2.drawLine((int)c0.x,(int)c0.y,(int)c1.x,(int)c1.y);
				g2.drawLine((int)c1.x,(int)c1.y,(int)c2.x,(int)c2.y);
				g2.drawLine((int)c2.x,(int)c2.y,(int)c3.x,(int)c3.y);
				g2.drawLine((int)c3.x,(int)c3.y,(int)c0.x,(int)c0.y);
			}

			if( tracker != null ) {
				// draw the original set of features in gray to make it easier to see when features are dropped
				for( Point2D_F64 p : allFeatures )
					VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,Color.gray);

				tracker.track(frame);
				List<Point2D_F64> initial = tracker.getInitial();
				List<Point2D_F64> current = tracker.getCurrent();
				for( int i = 0; i < initial.size(); i++ ) {
					Point2D_F64 pi = initial.get(i);
					Point2D_F64 pc = current.get(i);
					HomographyPointOps_F64.transform(H_inv,pc.x,pc.y,c0);

					VisualizeFeatures.drawPoint(g2,(int)pi.x,(int)pi.y,Color.blue);
					VisualizeFeatures.drawPoint(g2,(int)c0.x,(int)c0.y,Color.RED);

					g2.setColor(Color.BLUE);
					g2.drawLine((int)pi.x,(int)pi.y,(int)c0.x,(int)c0.y);
				}
			}

			gui.setBufferedImage(out);
			gui.repaint();

			BoofMiscOps.pause(50);

			System.out.printf("Frame %4d  :  %4f %4f %4f %4f %4f %4f %4f %4f %4f\n",
					index,H.a11,H.a12,H.a13,H.a21,H.a22,H.a23,H.a31,H.a32,H.a33);
			index++;

			while( paused )
				BoofMiscOps.pause(50);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		paused = !paused;
//		showLines = !showLines;
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public static void main( String args[] ) throws FileNotFoundException {
		Class imageType = ImageFloat32.class;

		String whichData = "various/urban";
//		String whichData = "carpet/skew";
//		String whichData = "carpet/rotate";
//		String whichData = "carpet/move_out";
//		String whichData = "carpet/move_in";

		SimpleImageSequence sequence =
				DefaultMediaManager.INSTANCE.openVideo(pathToData+whichData+"_undistorted.mjpeg", ImageType.single(imageType));

		List<Homography2D_F64> groundTruth = LogParseHomography.parse(pathToData+whichData+"_homography.txt");

		FactoryEvaluationTrackers trackers = new FactoryEvaluationTrackers(imageType);
//		EvaluationTracker tracker = trackers.createFhSurf(false);
//		EvaluationTracker tracker = trackers.createFhSurfKlt();
		EvaluationTracker tracker = trackers.createFhBriefKlt(false);
//		EvaluationTracker tracker = trackers.createKlt();
//		EvaluationTracker tracker = trackers.createBrief(false,false);


		VisualDebugHomographyTruth app = new VisualDebugHomographyTruth(imageType);

		app.process(sequence,groundTruth,tracker);

	}
}
