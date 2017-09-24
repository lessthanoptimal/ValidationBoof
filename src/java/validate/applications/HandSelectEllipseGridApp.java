package validate.applications;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.KeyPointsCircleHexagonalGrid;
import validate.misc.EllipseFileCodec;
import validate.misc.PointFileCodec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

/**
 *
 *
 * @author Peter Abeles
 */
public class HandSelectEllipseGridApp extends HandSelectBase {

	//		KeyPointsCircleRegularGrid keyAlg = new KeyPointsCircleRegularGrid();
	KeyPointsCircleHexagonalGrid keyAlg = new KeyPointsCircleHexagonalGrid();
	int numRows = 5;
	int numCols = 6;
	boolean regular = false;


	public HandSelectEllipseGridApp( File file ) {
		super(new SelectEllipsePanel(true),file);
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
