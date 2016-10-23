package validate.applications;

import boofcv.io.image.UtilImageIO;
import validate.misc.EllipseFileCodec;

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

		if( new File(outputName).exists() ) {
			imagePanel.list.addAll(EllipseFileCodec.load(outputName));
		}

		imagePanel.setBufferedImage(image);
		initGui(image.getWidth(),image.getHeight(),imagePanel);
	}

	@Override
	public void save() {
		EllipseFileCodec.save(outputName," rotated ellipses. x y a b phi",imagePanel.list);
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
		String imagePath = "data/shape/ellipse/outdoors/image00009.png";

		String outputName = new File(imagePath).getAbsolutePath();
		outputName = outputName.substring(0,outputName.length()-4)+".txt";

		BufferedImage image = UtilImageIO.loadImage(imagePath);

		new HandSelectEllipsesApp(image,outputName);
	}
}
