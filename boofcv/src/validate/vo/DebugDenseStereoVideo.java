package validate.vo;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class DebugDenseStereoVideo<T extends ImageSingleBand> implements MouseListener {

	SequenceStereoImages data;
	StereoDisparity<T,ImageFloat32> alg;

	ImagePanel imageLeft;
	ImagePanel imageRight;

	T inputLeft;
	T inputRight;

	T filteredLeft;
	T filteredRight;

	boolean paused;

	Class<T> imageType;

	ImagePanel disparityView;
	RectifiedPairPanel rectifiedView;

	public DebugDenseStereoVideo(StereoDisparity<T, ImageFloat32> alg,
								 SequenceStereoImages data) {
		this.alg = alg;
		this.data = data;
		imageType = alg.getInputType();

		inputLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		inputRight = GeneralizedImageOps.createSingleBand(imageType,1,1);

		filteredLeft = GeneralizedImageOps.createSingleBand(imageType,1,1);
		filteredRight = GeneralizedImageOps.createSingleBand(imageType,1,1);
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
		filteredLeft.reshape(data.getLeft().getWidth(),data.getLeft().getHeight());
		filteredRight.reshape(data.getRight().getWidth(),data.getRight().getHeight());

		ShowImages.showWindow(imageLeft, "Left");
		ShowImages.showWindow(imageRight,"Right");

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
		ConvertBufferedImage.convertFrom(data.getRight(), inputRight);

		// add invariance to lighting conditions
		GImageDerivativeOps.laplace(inputLeft, filteredLeft);
		GImageDerivativeOps.laplace(inputRight,filteredRight);
		GPixelMath.abs(filteredLeft, filteredLeft);
		GPixelMath.abs(filteredRight,filteredRight);
		GPixelMath.divide(filteredLeft,filteredLeft,4);
		GPixelMath.divide(filteredRight,filteredRight,4);

		StereoParameters param = data.getCalibration();

		// Compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		// original camera calibration matrices
		DenseMatrix64F K1 = PerspectiveOps.calibrationMatrix(param.getLeft(), null);
		DenseMatrix64F K2 = PerspectiveOps.calibrationMatrix(param.getRight(), null);

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();
		// New calibration matrix,
		DenseMatrix64F rectK = rectifyAlg.getCalibrationMatrix();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.fullViewLeft(param.left, rect1, rect2, rectK);

		// undistorted and rectify images
		ImageDistort<T> imageDistortLeft =
				RectifyImageOps.rectifyImage(param.getLeft(), rect1, imageType);
		ImageDistort<T> imageDistortRight =
				RectifyImageOps.rectifyImage(param.getRight(), rect2, imageType);

		// todo hack
		filteredLeft.setTo(inputLeft);
		filteredRight.setTo(inputRight);

		GeneralizedImageOps.fill(inputLeft,0);
		GeneralizedImageOps.fill(inputRight,0);

		imageDistortLeft.apply(filteredLeft, inputLeft);
		imageDistortRight.apply(filteredRight, inputRight);

		alg.process(inputLeft,inputRight);

		ImageFloat32 disparity = alg.getDisparity();

		int min = alg.getMinDisparity();
		int max = alg.getMaxDisparity();

		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, min,max, 0);

		BufferedImage visualizedRectL = ConvertBufferedImage.convertTo(inputLeft,null);
		BufferedImage visualizedRectR = ConvertBufferedImage.convertTo(inputRight,null);


		if( disparityView == null ) {
			disparityView = ShowImages.showWindow(visualized,"Disparity");
			rectifiedView = new RectifiedPairPanel(true,visualizedRectL,visualizedRectR);
			ShowImages.showWindow(rectifiedView,"Rectified");
		} else {
			disparityView.setBufferedImage(visualized);
			disparityView.repaint();
			rectifiedView.setImages(visualizedRectL,visualizedRectR);
			rectifiedView.repaint();
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
		ParseLeuven07 data = new ParseLeuven07("../visual_odometry/leuven07");

		Class imageType = ImageFloat32.class;

		StereoDisparity alg = FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT,
				10, 120, 2, 2, 30, 0, 0.1, imageType);

		DebugDenseStereoVideo app = new DebugDenseStereoVideo(alg,new WrapParseLeuven07(data));
		app.processSequence();
	}
}
