package boofcv.metrics.vo;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.gui.d3.Polygon3DSequenceViewer;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.regression.StereoVisualOdometryRegression;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO Remove rotational component from translational error vector by applying inverse to total rotation estimate
public class DebugVisualOdometryStereo<T extends ImageBase<T>>
		extends EvaluateVisualOdometryStereo<T>
		implements MouseListener
{

	ImageGridPanel imageDisplay;
	Polygon3DSequenceViewer viewer = new Polygon3DSequenceViewer();

	boolean paused;
	boolean step;

	BufferedImage rgb;


	public DebugVisualOdometryStereo(SequenceStereoImages data,
									 StereoVisualOdometry<T> alg ,
									 ImageType<T> imageType )
	{
		super(data,alg,imageType);
		setOutputStream(System.out);
	}

	public void initialize() {
		super.initialize();
		paused = false;
		step = false;
	}

	public void processSequence() {

		if( !data.next() )
			throw new RuntimeException("Failed to read first frame");

		int numRows=1,numCols=2;
		if( data.getLeft().getWidth() > data.getLeft().getHeight()*2 ) {
			numCols = 1;
			numRows=2;
		}
		imageDisplay = new ImageGridPanel(numRows,numCols,data.getLeft(),data.getRight());
		imageDisplay.addMouseListener(this);

		boolean convertToRgb = data.getLeft().getType() == BufferedImage.TYPE_BYTE_GRAY;

		if( convertToRgb )
			rgb = new BufferedImage(inputLeft.width,inputLeft.height,BufferedImage.TYPE_INT_BGR);

		if( imageDisplay != null )
			ShowImages.showWindow(imageDisplay,"Input");

		if( viewer != null ) {
//			DMatrixRMaj K = PerspectiveOps.calibrationMatrix(data.getCalibration().left,(DMatrixRMaj)null);
//			viewer.setFocalLength(K);
			viewer.setStepSize(data.getCalibration().getBaseline());
			viewer.setPreferredSize(new Dimension(inputLeft.width, inputLeft.height));
			viewer.setMaximumSize(viewer.getPreferredSize());

			ShowImages.showWindow(viewer,"3D");
		}

		processFrame();

		while( data.next() ) {
			if( imageDisplay != null ) {
				if( convertToRgb )
					rgb.getGraphics().drawImage(data.getLeft(),0,0,null);
				else
					rgb = data.getLeft();
				imageDisplay.setImages(rgb, data.getRight());
			}

			processFrame();

			if( imageDisplay != null ) {

				if( alg instanceof AccessPointTracks3D) {
					drawFeatures((AccessPointTracks3D)alg,rgb);
				}

				imageDisplay.repaint();
			}

			if( step ) {
				step = false;
				paused = true;
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
		}
	}

	private void updateView3D( Se3_F64 leftToWorld , Color color ) {
		StereoParameters config = data.getCalibration();

		double r = config.getBaseline();

		Point3D_F64 p1 = new Point3D_F64(-r,-r,0);
		Point3D_F64 p2 = new Point3D_F64(r,-r,0);
		Point3D_F64 p3 = new Point3D_F64(r,r,0);
		Point3D_F64 p4 = new Point3D_F64(-r,r,0);

		SePointOps_F64.transform(leftToWorld, p1, p1);
		SePointOps_F64.transform(leftToWorld,p2,p2);
		SePointOps_F64.transform(leftToWorld,p3,p3);
		SePointOps_F64.transform(leftToWorld,p4,p4);

		viewer.add(color,p1,p2,p3,p4);
		viewer.repaint();
	}

	private void processFrame() {
		if( nextFrame() ) {
			if( viewer != null ) {
				Se3_F64 leftToWorld = data.getLeftToWorld().concat(initialWorldToLeft,null);
				updateView3D(alg.getCameraToWorld(),Color.BLACK);
				updateView3D(leftToWorld,Color.ORANGE);
			}
		}
	}

	private double computeAngularError( Se3_F64 found , Se3_F64 expected ) {
		DMatrixRMaj A = new DMatrixRMaj(3,3);

		CommonOps_DDRM.multTransA(found.getR(), expected.getR(),A);

		return rotationMatrixToRadian(A);
	}

	private double rotationMatrixToRadian(DMatrixRMaj a) {
		double angles[] = ConvertRotation3D_F64.matrixToEuler(a, EulerType.XYZ,null);

		double sum = angles[0]*angles[0] + angles[1]*angles[1] + angles[1]*angles[1];

		return Math.sqrt(sum);
	}

	private static void drawFeatures( AccessPointTracks3D tracker , BufferedImage image )  {

		Graphics2D g2 = image.createGraphics();

		List<Point2D_F64> points = tracker.getAllTracks();
		if( points.isEmpty() )
			return;


		int numInliers = 0;
		double ranges[] = new double[points.size() ];

		for( int i = 0; i < points.size(); i++ ) {
			ranges[i] = tracker.getTrackLocation(i).z;
		}
		Arrays.sort(ranges);
		double maxRange = ranges[(int)(ranges.length*0.8)];

		for( int i = 0; i < points.size(); i++ ) {
			Point2D_F64 pixel = points.get(i);

			Point3D_F64 p3 = tracker.getTrackLocation(i);

			double r = p3.z/maxRange;
			if( r < 0 ) r = 0;
			else if( r > 1 ) r = 1;

			int color = (255 << 16) | ((int)(255*r) << 8);


			VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,new Color(color));
		}

		for( int i = 0; i < points.size(); i++ ) {
			Point2D_F64 pixel = points.get(i);

			if( tracker.isNew(i) ) {
//				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,Color.GREEN);
				continue;
			}

			if( tracker.isInlier(i) ) {
				numInliers++;
				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,7,Color.WHITE,false);
				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,5,Color.BLACK,false);
			}
		}


		g2.setColor(Color.BLACK);
		g2.fillRect(25,15,80,45);
		g2.setColor(Color.CYAN);
		g2.drawString("Total: " + tracker.getAllTracks().size(), 30, 30);
		g2.drawString("Inliers: "+numInliers,30,50);
	}


	@Override
	public void mouseClicked(MouseEvent e) {
		if( e.getButton() == MouseEvent.BUTTON3 ) {
			step = true;
			paused = false;
		} else {
			paused = !paused;
		}
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
//		SequenceStereoImages data = new WrapParseLeuven07(new ParseLeuven07("data/leuven07"));
		SequenceStereoImages data = new WrapParseKITTI("data/KITTI","02");

		Class bandType = GrayF32.class;
		ImageType imageType = ImageType.single(bandType);
		Class derivType = GImageDerivativeOps.getDerivativeType(bandType);

		StereoDisparitySparse<GrayF32> disparity =
				FactoryStereoDisparity.regionSparseWta(10, 120, 2, 2, 30, 0.1, true, bandType);

		PointTrackerTwoPass tracker = null;

		int selection = 0;

		if( selection == 0 ) {
			PkltConfig configKlt = new PkltConfig();
			configKlt.pyramidScaling = new int[]{1, 2, 4, 8};
			configKlt.templateRadius = 3;

			tracker = FactoryPointTrackerTwoPass.klt(configKlt, new ConfigGeneralDetector(600, 3, 1),
					bandType, derivType);
		}

		StereoVisualOdometry alg = StereoVisualOdometryRegression.createDualTrackerPnP(bandType).vo;

//		StereoVisualOdometry alg = FactoryVisualOdometry.stereoDepth(1.5,120, 2,200,50,false,disparity, tracker,bandType);

		DebugVisualOdometryStereo app = new DebugVisualOdometryStereo(data,alg,imageType);
		app.initialize();

		app.processSequence();

		System.out.println("DONE!!!");
		System.exit(0);
	}
}
