package boofcv.metrics.vo;

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO improve synchronization
public class DebugTrackerVideo <T extends ImageGray<T>> implements MouseListener, KeyListener {

	int KEY_FRAME_PERIOD = 10;

	SequenceStereoImages data;
	PointTracker<T> alg;

	ImagePanel imageLeft;

	T inputLeft;

	T rectifiedLeft;

	volatile boolean paused;

	Class<T> imageType;

	BufferedImage rgb;
	BufferedImage key;
	BufferedImage work;

	int selected = -1;
	Point2D_F64 selectedPt = new Point2D_F64();

	int frame = 0;

	boolean step;

	// if true then all but the selected tracks are hidden
	boolean hideTracks = false;


	public DebugTrackerVideo(PointTracker<T> alg,
							 SequenceStereoImages data,
							 Class<T> imageType ) {
		this.alg = alg;
		this.data = data;
		this.imageType = imageType;

		inputLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		rectifiedLeft = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}

	public void processSequence() {

		if( !data.next() )
			throw new RuntimeException("Failed to read first frame");

		int w = data.getLeft().getWidth();
		int h = data.getLeft().getHeight();

		inputLeft.reshape(w,h);
		rectifiedLeft.reshape(w,h);

		if( data.getLeft().getType() == BufferedImage.TYPE_BYTE_GRAY ) {
			rgb = new BufferedImage(w,h,BufferedImage.TYPE_INT_BGR);
		}
		key = new BufferedImage(w,h,BufferedImage.TYPE_INT_BGR);
		work = new BufferedImage(w,2*h,BufferedImage.TYPE_INT_BGR);

		imageLeft = new ImagePanel(work);
		imageLeft.addMouseListener(this);
		imageLeft.addKeyListener(this);
		ShowImages.showWindow(imageLeft, "Tracks");
		imageLeft.grabFocus();

		processFrame();

		while( data.next() ) {
			System.out.println("Frame: "+frame);
			if( data.getLeft().getType() != BufferedImage.TYPE_BYTE_GRAY ) {
				rgb = data.getLeft();
			} else {
				rgb.createGraphics().drawImage(data.getLeft(), 0, 0, null);
			}

			imageLeft.setImage(work);

			processFrame();

			imageLeft.repaint();

			if( step ) {
				paused = true;
				step = false;
			}

			while( paused ) {
				synchronized ( this ) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}

			frame++;
		}
	}

	private void updateSelectedIndex() {
		// find the index of selected again
		if( selected != -1 ) {
			List<PointTrack> all = new ArrayList<PointTrack>();
			alg.getAllTracks(all);
			double dist = Double.MAX_VALUE;
			for( int i = 0; i < all.size(); i++ ) {
				Point2D_F64 orig = all.get(i).getCookie();

				double d = selectedPt.distance2(orig);
				if( d < dist ) {
					dist = d;
					selected = i;
				}
			}
		}
	}

	private synchronized void processFrame() {
		ConvertBufferedImage.convertFrom(data.getLeft(), inputLeft,true);
		if( frame > 350 )
		{
		alg.process(inputLeft);

		if( frame % KEY_FRAME_PERIOD == 0 ) {
			alg.dropAllTracks();
			alg.spawnTracks();

			List<PointTrack> all = new ArrayList<PointTrack>();
			alg.getAllTracks(all);

			for( PointTrack p : all ) {
				p.cookie = p.pixel.copy();
			}
			key.createGraphics().drawImage(rgb,0,0,null);
		}
		}

		updateSelectedIndex();
		renderTracks();
	}

	private void renderTracks() {
		int h = rgb.getHeight();

		Graphics2D g2 = work.createGraphics();
		g2.drawImage(key,0,0,null);
		g2.drawImage(rgb,0,h,null);

		List<PointTrack> active = new ArrayList<PointTrack>();
		alg.getActiveTracks(active);

		if( !hideTracks ) {
			for( PointTrack p : active ) {
				Point2D_F64 orig = p.getCookie();

				VisualizeFeatures.drawPoint(g2, (int)orig.x, h+(int) orig.y, 5, Color.WHITE, false);
				VisualizeFeatures.drawPoint(g2, (int)orig.x, h+(int) orig.y, 3, Color.gray, false);
			}

			for( PointTrack p : active ) {
				Point2D_F64 orig = p.getCookie();

				g2.setColor( Color.blue);
				g2.setStroke(new BasicStroke(2));
				g2.drawLine((int)orig.x, h+(int) orig.y, (int)p.pixel.x, h+(int) p.pixel.y);

				VisualizeFeatures.drawPoint(g2, (int)p.pixel.x, h+(int) p.pixel.y, 5, Color.WHITE, false);
				VisualizeFeatures.drawPoint(g2, (int)p.pixel.x, h+(int) p.pixel.y, 3, Color.RED, false);
			}
		}

		if( selected != -1 && selected < active.size() ) {
			PointTrack p = active.get(selected);
			Point2D_F64 orig = p.getCookie();

			VisualizeFeatures.drawPoint(g2, (int) orig.x, (int) orig.y, 5, Color.WHITE, false);
			VisualizeFeatures.drawPoint(g2, (int)orig.x, (int) orig.y, 3, Color.gray, false);

			g2.setColor( Color.blue);
			g2.setStroke(new BasicStroke(2));
			g2.drawLine((int)orig.x, h+(int) orig.y, (int)p.pixel.x, h+(int) p.pixel.y);

			g2.setColor( Color.CYAN);
			g2.setStroke(new BasicStroke(5));
			g2.drawLine((int)orig.x, (int) orig.y, (int)p.pixel.x, h+(int) p.pixel.y);

			VisualizeFeatures.drawPoint(g2, (int)p.pixel.x, h+(int) p.pixel.y, 5, Color.WHITE, false);
			VisualizeFeatures.drawPoint(g2, (int)p.pixel.x, h+(int) p.pixel.y, 3, Color.RED, false);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		imageLeft.grabFocus();
		paused = selected != -1 ? true : !paused;
		hideTracks = false;
		int prevSelected = selected;
		selected = -1;

		List<PointTrack> active = new ArrayList<PointTrack>();
		alg.getActiveTracks(active);

		Point2D_F64 w = new Point2D_F64(e.getX(),e.getY());
		Point2D_F64 t = new Point2D_F64();

		int h = data.getLeft().getHeight();

		for( int i = 0; i < active.size(); i++ ) {
			PointTrack p = active.get(i);
			t.setTo((Point2D_F64)p.getCookie());
			t.y += h;

			if( w.distance(t) < 5 ) {
				selected = i;
				break;
			}

			t.setTo(p.pixel.x,h+p.pixel.y);

			if( w.distance(t) < 5 ) {
				selected = i;
				break;
			}
		}

		if( selected != -1 )  {
			if( prevSelected == selected )
				hideTracks = true;
			selectedPt.setTo(active.get(selected).pixel);
			paused = true;
		}
		renderTracks();
		imageLeft.repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if( paused )
			paused = false;
		step = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	public static void main( String args[] ) {
//		SequenceStereoImages data = new WrapParseLeuven07(new ParseLeuven07("data/leuven07"));
		SequenceStereoImages data = new WrapParseKITTI("data/KITTI","01");

		Class imageType = GrayF32.class;

		ConfigPKlt configPKlt = new ConfigPKlt();
		configPKlt.templateRadius = 3;
		ConfigPointDetector configDet = new ConfigPointDetector();
		configDet.general.maxFeatures = -1;
		configDet.general.radius = 5;
		configDet.general.threshold = 300;

//		ImagePointTracker<GrayF32> tracker = FactoryPointSequentialTracker.dda_ST_BRIEF(-1,200,5,500,imageType,null);
//		ImagePointTracker<GrayF32> tracker =
//				FactoryPointSequentialTracker.dda_FH_SURF(500,2,200,1,true,imageType);
		PointTracker<GrayF32> tracker =
				FactoryPointTracker.klt(configPKlt, configDet, imageType, GrayF32.class);

		DebugTrackerVideo app = new DebugTrackerVideo(tracker,data,imageType);
		app.processSequence();
	}
}
