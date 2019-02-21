package boofcv.applications;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.KeyPointsCircleHexagonalGrid;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.common.misc.EllipseFileCodec;
import boofcv.common.misc.PointFileCodec;
import boofcv.factory.shape.ConfigEllipseDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import georegression.struct.curve.EllipseRotated_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Peter Abeles
 */
public class HandSelectEllipseGridApp extends HandSelectBase {

	//		KeyPointsCircleRegularGrid keyAlg = new KeyPointsCircleRegularGrid();
	KeyPointsCircleHexagonalGrid keyAlg = new KeyPointsCircleHexagonalGrid();
	int numRows = 24;
	int numCols = 28;
	boolean regular = false;


	public HandSelectEllipseGridApp( File file ) {
		super(new SelectEllipsePanel(true),file);

		infoPanel.handleSelectShape = new Runnable() {
			@Override
			public void run() {
				if( image == null ) {
					return;
				}

				ConfigEllipseDetector config = new ConfigEllipseDetector();
				config.minimumMinorAxis = 5;
				BinaryEllipseDetector<GrayU8> detector = FactoryShapeDetector.ellipse(config,GrayU8.class);
				GrayU8 gray = new GrayU8(image.getWidth(),image.getHeight());
				GrayU8 binary = gray.createSameShape();

				ConvertBufferedImage.convertFrom(image,gray);

				ThresholdImageOps.localMean(gray,binary, ConfigLength.fixed(21),1.0f,true,null,null,null);

				detector.process(gray,binary);

				List<EllipseRotated_F64> list = new ArrayList<>();
				detector.getFoundEllipses(list);


				System.out.println("Detected total "+list.size());
				((SelectEllipsePanel)imagePanel).addUnselected(list);
				imagePanel.repaint();
			}
		};
	}

	@Override
	public void process(File file, BufferedImage image) {
		this.infoPanel.setImageShape(image.getWidth(),image.getHeight());
		SelectEllipsePanel gui = (SelectEllipsePanel)this.imagePanel;
		File outputFile = selectOutputFile(file);
		String ellipsePath = outputFile.getPath();
		ellipsePath = ellipsePath.substring(0,ellipsePath.length()-4)+"_ellipse.txt";

		if( new File(ellipsePath).exists() ) {
			gui.list.addAll(EllipseFileCodec.load(ellipsePath));
		}

		gui.setBufferedImage(image);
	}

	@Override
	public void save() {
		SelectEllipsePanel gui = (SelectEllipsePanel)this.imagePanel;

		Grid g = new Grid();
		g.rows = numRows;
		g.columns = numCols;

		// for regular grid it can be just set
		if( regular ) {
			g.ellipses = gui.list;
		} else {
			int index = 0;
			g.ellipses = new ArrayList<>();
			for (int row = 0; row < numRows; row++) {
				for (int col = 0; col < numCols; col++) {
					if( row%2==0 && col%2==1) {
						g.ellipses.add(null);
					} else if( row%2==1 &&col%2 == 0 ) {
						g.ellipses.add(null);
					} else {
						g.ellipses.add( gui.list.get(index++));
					}
				}
			}
		}

		keyAlg.process(g);
		keyAlg.getKeyPoints();

		File outputFile = selectOutputFile(inputFile);

		String ellipsePath = outputFile.getPath();
		ellipsePath = ellipsePath.substring(0,ellipsePath.length()-4)+"_ellipse.txt";

		EllipseFileCodec.save(ellipsePath," rotated ellipses. x y a b phi",gui.list);
		PointFileCodec.save(outputFile.getPath(), " list of hand selected 2D points from ellipse tangents",
				keyAlg.getKeyPoints().toList());
	}

	@Override
	public String getApplicationName() {
		return "Select Ellipses";
	}

	@Override
	public void setScale( double scale ) {
		SelectEllipsePanel gui = (SelectEllipsePanel)this.imagePanel;
		gui.setScale(scale);
	}

	@Override
	public void clearPoints() {
		SelectEllipsePanel gui = (SelectEllipsePanel)this.imagePanel;
		gui.clearPoints();
		gui.repaint();
	}

	public static void main(String[] args) {
		new HandSelectEllipseGridApp(null);
	}
}
