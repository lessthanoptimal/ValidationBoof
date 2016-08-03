package validate.applications;

import boofcv.io.image.UtilImageIO;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 *
 *
 * @author Peter Abeles
 */
public class HandSelectEllipsesApp extends HandSelectBase {


	SelectEllipsePanel imagePanel = new SelectEllipsePanel();

	public HandSelectEllipsesApp(BufferedImage image , String outputName ) {
		super(outputName);

		imagePanel.setBufferedImage(image);
		initGui(image.getWidth(),image.getHeight(),imagePanel);
	}

	@Override
	public void save() {

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
		String imagePath = "data/shape/border01/image00000.jpg";

		String outputName = new File(imagePath).getAbsolutePath();
		outputName = outputName.substring(0,outputName.length()-4)+".txt";

		BufferedImage image = UtilImageIO.loadImage(imagePath);

		new HandSelectEllipsesApp(image,outputName);
	}
}
