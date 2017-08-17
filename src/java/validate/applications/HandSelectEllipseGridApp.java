package validate.applications;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.KeyPointsCircleRegularGrid;
import validate.misc.EllipseFileCodec;
import validate.misc.PointFileCodec;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 *
 *
 * @author Peter Abeles
 */
public class HandSelectEllipseGridApp extends HandSelectBase {

	int numRows = 4;
	int numCols = 3;


	public HandSelectEllipseGridApp( File file ) {
		super(new SelectEllipsePanel(),file);
	}

	@Override
	public void process(File file, BufferedImage image) {
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
		KeyPointsCircleRegularGrid keyAlg = new KeyPointsCircleRegularGrid();

		Grid g = new Grid();
		g.rows = numRows;
		g.columns = numCols;
		g.ellipses = gui.list;

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
		String imagePath = "data/fiducials/circle_asymmetric/standard/distance_angle/image00000.jpg";

		new HandSelectEllipseGridApp(new File(imagePath));
	}
}
