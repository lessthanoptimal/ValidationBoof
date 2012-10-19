package validate.vo;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.se.Se3_F64;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author Peter Abeles
 */
public class DebugVisualOdometryStereo<T extends ImageSingleBand> implements MouseListener {

	SequenceStereoImages data;
	StereoVisualOdometry<T> alg;

	ImagePanel imageLeft;
	ImagePanel imageRight;

	T inputLeft;
	T inputRight;

	boolean paused;
	int numFaults = 0;
	int numSkipUpdate = 0;

	// initial location of the camera
	Se3_F64 worldToInitial = new Se3_F64();

	public DebugVisualOdometryStereo(SequenceStereoImages data,
									 StereoVisualOdometry<T> alg ,
									 Class<T> imageType )
	{
		this.data = data;
		this.alg = alg;

		inputLeft = GeneralizedImageOps.createSingleBand(imageType,1,1);
		inputRight = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}

	public void initialize() {
		paused = false;
		alg.reset();

		numFaults = 0;
		numSkipUpdate = 0;
	}

	public void processSequence() {

		if( !data.next() )
			throw new RuntimeException("Failed to read first frame");

		imageLeft = new ImagePanel(data.getLeft());
		imageRight = new ImagePanel(data.getRight());

		imageLeft.addMouseListener(this);
		imageRight.addMouseListener(this);

		inputLeft.reshape(data.getLeft().getWidth(),data.getLeft().getHeight());
		inputRight.reshape(data.getRight().getWidth(),data.getRight().getHeight());

		ShowImages.showWindow(imageLeft,"Left");
		ShowImages.showWindow(imageRight,"Right");

		data.getLeftToWorld().invert(worldToInitial);

		processFrame();

		while( data.next() ) {
			imageLeft.setBufferedImage(data.getLeft());
			imageRight.setBufferedImage(data.getRight());

			processFrame();

			imageLeft.repaint();
			imageRight.repaint();

			while( paused ) {
				synchronized ( this ) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	private void processFrame() {
		ConvertBufferedImage.convertFrom(data.getLeft(), inputLeft);
		ConvertBufferedImage.convertFrom(data.getRight(),inputRight);

		alg.setCalibration(data.getCalibration());
		if( !alg.process(inputLeft,inputRight) ) {
			numSkipUpdate++;
			if( alg.isFault() ) {
				numFaults++;
			}
			System.out.println(" NO UPDATE fault = "+alg.isFault());
		} else {
			Se3_F64 found = alg.getCameraToWorld();
			Se3_F64 expected = data.getLeftToWorld().concat(worldToInitial,null);

			double error = found.getT().distance(expected.getT());
			double errorFrac = error / expected.getT().norm();

			System.out.printf(" location error %f error frac %f\n", error, errorFrac);
		}
	}


	@Override
	public void mouseClicked(MouseEvent e) {
		paused = !paused;
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public static void main( String args[] ) {
		// TODO light invariant sparse stereo
		// TODO or preprocess images with edge detector?

		ParseLeuven07 data = new ParseLeuven07("/home/pja/projects/ValidationBoof/visual_odometry/leuven07");

		Class imageType = ImageFloat32.class;

		ImagePointTracker<ImageFloat32> tracker =
				FactoryPointSequentialTracker.klt(400, new int[]{1, 2, 4, 8}, 3, 3, 2, imageType, ImageFloat32.class);
		StereoDisparitySparse<ImageFloat32> disparity =
				FactoryStereoDisparity.regionSparseWta(0, 150, 3, 3, 30, -1, true, imageType);


		StereoVisualOdometry alg = FactoryVisualOdometry.stereoDepth(75,1,tracker,disparity,imageType);

		DebugVisualOdometryStereo app = new DebugVisualOdometryStereo(new WrapParseLeuven07(data),alg,imageType);

		app.initialize();

		app.processSequence();
	}
}
