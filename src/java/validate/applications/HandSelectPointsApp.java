package validate.applications;

import boofcv.io.image.UtilImageIO;
import georegression.struct.point.Point2D_F64;
import validate.misc.PointFileCodec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 *
 *
 * @author Peter Abeles
 */
public class HandSelectPointsApp extends HandSelectBase {


	SelectPointPanel imagePanel = new SelectPointPanel();

	public HandSelectPointsApp( BufferedImage image , String outputName ) {
		super(outputName);

		if( new File(outputName).exists() ) {
			List<List<Point2D_F64>> sets = PointFileCodec.loadSets(outputName);
			if( sets == null ) {
				imagePanel.addPointSet(PointFileCodec.load(outputName));
			} else {
				imagePanel.setSets(sets);
			}
		}
		imagePanel.setBufferedImage(image);
		initGui(image.getWidth(),image.getHeight(),imagePanel);
	}

	@Override
	public void save() {
		List<List<Point2D_F64>> points = imagePanel.getSelectedPoints();

		if( points.size() == 1 ) {
			PointFileCodec.save(outputName, "list of hand selected 2D points", points.get(0));
			System.out.println("Saved to " + outputName);
		} else if( points.size() > 1 ){
			PointFileCodec.saveSets(outputName, "list of hand selected 2D points", points);
			System.out.println("Saved to " + outputName);
		}
	}

	@Override
	public String getApplicationName() {
		return "Select Point Features";
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
		String imagePath = "data/shape/border01/image00000.jpg";

		String outputName = new File(imagePath).getAbsolutePath();
		outputName = outputName.substring(0,outputName.length()-4)+".txt";

		BufferedImage image = UtilImageIO.loadImage(imagePath);

		new HandSelectPointsApp(image,outputName);
	}
}
