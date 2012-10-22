package validate.vo;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.alg.sfm.AccessSfmPointTracks;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * @author Peter Abeles
 */
public class DebugVisualOdometryStereo<T extends ImageSingleBand> implements MouseListener {

	SequenceStereoImages data;
	StereoVisualOdometry<T> alg;

	ImageGridPanel imageDisplay;

	T inputLeft;
	T inputRight;

	boolean paused;
	int numFaults = 0;
	int numSkipUpdate = 0;

	double distanceTruth = 0;
	double distanceFound = 0;

	Se3_F64 previousWorldToLeftFound = new Se3_F64();
	Se3_F64 previousWorldToLeft = new Se3_F64();
	Se3_F64 initialWorldToLeft = new Se3_F64();


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

		imageDisplay = new ImageGridPanel(1,2,data.getLeft(),data.getRight());
		imageDisplay.addMouseListener(this);

		inputLeft.reshape(data.getLeft().getWidth(),data.getLeft().getHeight());
		inputRight.reshape(data.getRight().getWidth(),data.getRight().getHeight());

		ShowImages.showWindow(imageDisplay,"Input");

		previousWorldToLeftFound.reset();
		data.getLeftToWorld().invert(previousWorldToLeft);
		initialWorldToLeft.set(previousWorldToLeft);

		processFrame();

		while( data.next() ) {
			imageDisplay.setImages(data.getLeft(), data.getRight());

			processFrame();

			if( alg instanceof AccessSfmPointTracks ) {
				drawFeatures((AccessSfmPointTracks)alg,data.getLeft());
			}

			imageDisplay.repaint();

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

		alg.setCalibration(data.getCalibration());

		if( !alg.process(inputLeft,inputRight) ) {
			numSkipUpdate++;
			if( alg.isFault() ) {
				numFaults++;
			}
			System.out.println(" NO UPDATE fault = "+alg.isFault());
		} else {
			Se3_F64 found = alg.getLeftToWorld().concat(previousWorldToLeftFound,null);
			Se3_F64 expected = data.getLeftToWorld().concat(previousWorldToLeft,null);

			distanceFound += found.getT().norm();
			distanceTruth += expected.getT().norm();

			double error = Math.abs(distanceFound-distanceTruth);
			double errorFrac = error / distanceTruth;

			Se3_F64 foundAbsolute = alg.getLeftToWorld();
			Se3_F64 expectedAbsolute = data.getLeftToWorld().concat(initialWorldToLeft,null);
			double errorAngle = computeAngularError(foundAbsolute,expectedAbsolute);
//			errorAngle = UtilAngle.radianToDegree(errorAngle);

			expectedAbsolute.getT().print();
			foundAbsolute.getT().print();


			System.out.printf(" location error %f error frac %f  angle %6.3f\n", error, errorFrac,errorAngle);

			data.getLeftToWorld().invert(previousWorldToLeft);
			alg.getLeftToWorld().invert(previousWorldToLeftFound);
		}
	}

	private double computeAngularError( Se3_F64 found , Se3_F64 expected ) {
		DenseMatrix64F A = new DenseMatrix64F(3,3);

		CommonOps.multTransA(found.getR(), expected.getR(),A);

		double angles[] = RotationMatrixGenerator.matrixToEulerXYZ(A);

		double sum = angles[0]*angles[0] + angles[1]*angles[1] + angles[1]*angles[1];

		return Math.sqrt(sum);
	}

	private static void drawFeatures( AccessSfmPointTracks tracker , BufferedImage image )  {

		Graphics2D g2 = image.createGraphics();

		for( Point2D_F64 p : tracker.getNewTracks() ) {
			VisualizeFeatures.drawPoint(g2, (int) p.x, (int) p.y, 3, Color.GREEN);
		}

		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(3));
		for( Point2D_F64 p : tracker.getAllTracks() ) {
			VisualizeFeatures.drawCross(g2,(int)p.x,(int)p.y,3);
		}

		java.util.List<Point2D_F64> inliers = tracker.getInlierTracks();
		if( inliers.size() > 0 ) {
			double ranges[] = new double[tracker.getInlierTracks().size() ];
			for( int i = 0; i < tracker.getInlierTracks().size(); i++ ) {

				int indexAll = tracker.fromInlierToAllIndex(i);
				Point3D_F64 p3 = tracker.getTrackLocation(indexAll);

				ranges[i] = p3.z;
			}
			Arrays.sort(ranges);

			double maxRange = ranges[(int)(ranges.length*0.8)];

			for( int i = 0; i < tracker.getInlierTracks().size(); i++ ) {

				int indexAll = tracker.fromInlierToAllIndex(i);

				Point2D_F64 pixel = tracker.getInlierTracks().get(i);
				Point3D_F64 p3 = tracker.getTrackLocation(indexAll);

				double r = p3.z/maxRange;
				if( r < 0 ) r = 0;
				else if( r > 1 ) r = 1;

				int color = (255 << 16) | ((int)(255*r) << 8);

				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,new Color(color));
			}

		}

		g2.setColor(Color.BLACK);
		g2.fillRect(25,15,80,45);
		g2.setColor(Color.CYAN);
		g2.drawString("Total: " + tracker.getAllTracks().size(), 30, 30);
		g2.drawString("Inliers: "+inliers.size(),30,50);
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

		ParseLeuven07 data = new ParseLeuven07("../visual_odometry/leuven07");

		Class imageType = ImageFloat32.class;

		ImagePointTracker<ImageFloat32> tracker =
				FactoryPointSequentialTracker.klt(400, new int[]{1, 2, 4, 8}, 3, 3, 2, imageType, ImageFloat32.class);
		StereoDisparitySparse<ImageFloat32> disparity =
				FactoryStereoDisparity.regionSparseWta(10, 120, 2, 2, 30, 0.1, true, imageType);


		StereoVisualOdometry alg = FactoryVisualOdometry.stereoDepth(75,1,tracker,disparity,imageType);

		DebugVisualOdometryStereo app = new DebugVisualOdometryStereo(new WrapParseLeuven07(data),alg,imageType);

		app.initialize();

		app.processSequence();
	}
}
