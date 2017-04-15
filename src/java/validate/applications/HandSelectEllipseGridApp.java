package validate.applications;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.KeyPointsCircleRegularGrid;
import boofcv.io.image.UtilImageIO;
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

	SelectEllipsePanel imagePanel = new SelectEllipsePanel();

	String outputEllipseName;

	public HandSelectEllipseGridApp(BufferedImage image , String outputName ) {
		super(outputName);

		outputEllipseName = outputName.substring(0,outputName.length()-4)+"_ellipse.txt";

		if( new File(outputEllipseName).exists() ) {
			imagePanel.list.addAll(EllipseFileCodec.load(outputEllipseName));
		}

		imagePanel.setBufferedImage(image);
		initGui(image.getWidth(),image.getHeight(),imagePanel);
	}

	@Override
	public void save() {

		KeyPointsCircleRegularGrid keyAlg = new KeyPointsCircleRegularGrid();

		Grid g = new Grid();
		g.rows = numRows;
		g.columns = numCols;
		g.ellipses = imagePanel.list;


		keyAlg.process(g);

		keyAlg.getKeyPoints();

		EllipseFileCodec.save(outputEllipseName," rotated ellipses. x y a b phi",imagePanel.list);
		PointFileCodec.save(outputName, " list of hand selected 2D points from ellipse tangents",
				keyAlg.getKeyPoints().toList());
	}

	@Override
	public String getApplicationName() {
		return "Select Ellipses";
	}

	@Override
	public void setScale( double scale ) {
		imagePanel.setScale(scale);
	}

	@Override
	public void clearPoints() {
		imagePanel.clearPoints();
		imagePanel.repaint();
	}

	public static void main(String[] args) {
		String imagePath = "data/calib/mono/circle_regular/fisheye/image04.jpg";

		String outputName = new File(imagePath).getAbsolutePath();
		outputName = outputName.substring(0,outputName.length()-4)+".txt";

		BufferedImage image = UtilImageIO.loadImage(imagePath);

		new HandSelectEllipseGridApp(image,outputName);
	}
}
