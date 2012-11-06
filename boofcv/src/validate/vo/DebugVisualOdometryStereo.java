package validate.vo;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.StereoVisualOdometry;
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
import java.util.Arrays;
import java.util.List;

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

	Se3_F64 previousWorldToLeftFound = new Se3_F64();
	Se3_F64 previousWorldToLeft = new Se3_F64();
	Se3_F64 initialWorldToLeft = new Se3_F64();

	double totalErrorDistance;
	double totalErrorRotation;
	double totalFoundDistance;
	double totalFoundRotation;
	double totalDistance;
	double totalRotation;
	double absoluteLocation;
	double absoluteRotation;

	int numEstimates;

	int frame;

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

		totalErrorDistance = 0;
		totalErrorRotation = 0;
		totalFoundDistance = 0;
		totalFoundRotation = 0;
		totalDistance = 0;
		totalRotation = 0;

		numEstimates = 0;

		numFaults = 0;
		numSkipUpdate = 0;

		frame = 0;

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

			if( alg instanceof AccessPointTracks3D) {
				drawFeatures((AccessPointTracks3D)alg,data.getLeft());
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

		// todo add absolute location and absolute rotation?
;
		double integralDistance = Math.abs(totalFoundDistance-totalDistance)/totalDistance;
		double integralRotation = Math.abs(totalFoundRotation-totalRotation)/totalDistance;
		double averageDistance = totalErrorDistance/numEstimates;
		double averageRotation = totalErrorRotation/numEstimates;

		System.out.println("Ave per estimate:      location = "+averageDistance+"  angle "+averageRotation);
		System.out.println("Absolute per distance: location "+(absoluteLocation/totalDistance)+" rotation "+
		(absoluteRotation/totalDistance));
		System.out.println("Integral per distance: distance "+integralDistance+" rotation "+integralRotation);

	}

	private void processFrame() {
		ConvertBufferedImage.convertFrom(data.getLeft(), inputLeft);
		ConvertBufferedImage.convertFrom(data.getRight(), inputRight);

		long before = System.nanoTime();
		alg.setCalibration(data.getCalibration());
		boolean updated = alg.process(inputLeft,inputRight);
		long after = System.nanoTime();

		double fps = 1.0/((after-before)*1e-9);

		if( !updated ) {
			numSkipUpdate++;
			if( alg.isFault() ) {
				numFaults++;
			}
			System.out.printf("%d %6.2f NO UPDATE fault = %s\n",frame,fps,alg.isFault());
		} else {
			Se3_F64 found = alg.getLeftToWorld().concat(previousWorldToLeftFound,null);
			Se3_F64 expected = data.getLeftToWorld().concat(previousWorldToLeft,null);

			Se3_F64 diff = expected.concat(found.invert(null), null);

			double distanceError = diff.getT().norm();
			double distanceTruth = expected.getT().norm();

			double errorFrac = distanceError / distanceTruth;
			double errorAngle = rotationMatrixToRadian(diff.getR());

			System.out.printf("%5d %6.2f location error %f error frac %f  angle %6.3f\n", frame,fps,distanceError, errorFrac,errorAngle);
//			System.out.println("  expected "+expected.getT());
//			System.out.println("  found "+found.getT());

			alg.getLeftToWorld().invert(previousWorldToLeftFound);
			data.getLeftToWorld().invert(previousWorldToLeft);

			numEstimates++;
			totalErrorDistance += distanceError;
			totalFoundDistance += found.getT().norm();
			totalFoundRotation += rotationMatrixToRadian(found.getR());
			totalDistance += distanceTruth;
			totalErrorRotation = errorAngle;
			totalRotation += rotationMatrixToRadian(expected.getR());

			// find difference in absolute location
			Se3_F64 leftToWorld = data.getLeftToWorld().concat(initialWorldToLeft,null);

			diff = leftToWorld.concat(alg.getLeftToWorld().invert(null),null);
			absoluteLocation = diff.getT().norm();
			absoluteRotation = rotationMatrixToRadian(diff.getR());
		}

		frame++;
	}

	private double computeAngularError( Se3_F64 found , Se3_F64 expected ) {
		DenseMatrix64F A = new DenseMatrix64F(3,3);

		CommonOps.multTransA(found.getR(), expected.getR(),A);

		return rotationMatrixToRadian(A);
	}

	private double rotationMatrixToRadian(DenseMatrix64F a) {
		double angles[] = RotationMatrixGenerator.matrixToEulerXYZ(a);

		double sum = angles[0]*angles[0] + angles[1]*angles[1] + angles[1]*angles[1];

		return Math.sqrt(sum);
	}

	private static void drawFeatures( AccessPointTracks3D tracker , BufferedImage image )  {

		Graphics2D g2 = image.createGraphics();

		List<Point2D_F64> points = tracker.getAllTracks();

		int numInliers = 0;
		double ranges[] = new double[points.size() ];

		for( int i = 0; i < points.size(); i++ ) {
			ranges[i] = tracker.getTrackLocation(i).z;
		}
		Arrays.sort(ranges);
		double maxRange = ranges[(int)(ranges.length*0.8)];

		for( int i = 0; i < points.size(); i++ ) {
			Point2D_F64 pixel = points.get(i);

			if( tracker.isNew(i) ) {
				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,Color.GREEN);
				continue;
			}

			if( tracker.isInlier(i) ) {
				numInliers++;
//				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,7,Color.BLUE,false);
			}

			Point3D_F64 p3 = tracker.getTrackLocation(i);

			double r = p3.z/maxRange;
			if( r < 0 ) r = 0;
			else if( r > 1 ) r = 1;

			int color = (255 << 16) | ((int)(255*r) << 8);


			VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,new Color(color));
		}


		g2.setColor(Color.BLACK);
		g2.fillRect(25,15,80,45);
		g2.setColor(Color.CYAN);
		g2.drawString("Total: " + tracker.getAllTracks().size(), 30, 30);
		g2.drawString("Inliers: "+numInliers,30,50);
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
				FactoryPointSequentialTracker.dda_ShiTomasi_BRIEF(500,200,1,1,4,imageType,null);

//		ImagePointTracker<ImageFloat32> tracker =
//				FactoryPointSequentialTracker.klt(400, new int[]{1, 2, 4, 8}, 3, 3, 2, imageType, ImageFloat32.class);
		StereoDisparitySparse<ImageFloat32> disparity =
				FactoryStereoDisparity.regionSparseWta(10, 120, 2, 2, 30, 0.1, true, imageType);

		StereoVisualOdometry alg = FactoryVisualOdometry.stereoDepth(80,2,1.5,tracker,disparity,100,imageType);

		DebugVisualOdometryStereo app = new DebugVisualOdometryStereo(new WrapParseLeuven07(data),alg,imageType);

		app.initialize();

		app.processSequence();

		System.out.println("DONE!!!");
		System.exit(0);
	}
}
