package validate.applications;

import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import georegression.struct.point.Point2D_F64;
import validate.misc.PointFileCodec;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Peter Abeles
 */
public class HandSelectPointsApp {


	String outputName;

	JPanel gui = new JPanel();

	SelectPointPanel imagePanel = new SelectPointPanel();
	InfoHandSelectPanel infoPanel = new InfoHandSelectPanel(this);

	public HandSelectPointsApp( BufferedImage image , String outputName ) {
		if( new File(outputName).exists() ) {
			imagePanel.setPoints(PointFileCodec.load(outputName));
		}

		this.outputName = outputName;
		imagePanel.setBufferedImage(image);
		gui.setLayout(new BorderLayout());
		gui.add(imagePanel,BorderLayout.CENTER);
		gui.add(infoPanel, BorderLayout.EAST);

		imagePanel.addMouseWheelListener(infoPanel);

		// scale the image if it's huge
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		double scaleX = screenSize.getWidth()/(double)image.getWidth();
		double scaleY = screenSize.getHeight()/(double)image.getHeight();
		double scale = Math.min(scaleX,scaleY);
		if( scale < 1) {
			infoPanel.setScale(scale);
			setScale(scale);
		}

		ShowImages.showWindow(gui,"Point Selector").setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	public void save() {
		List<Point2D_F64> points = imagePanel.getSelectedPoints();

		PointFileCodec.save(outputName,"list of hand selected 2D points",points);
		System.out.println("Saved to "+outputName);
	}

	public void setScale( double scale ) {
		imagePanel.setScale(scale);
	}

	public void clearPoints() {
		imagePanel.setPoints(new ArrayList<Point2D_F64>());
		imagePanel.repaint();
	}

	public static void main(String[] args) {
		String imagePath = "data/fiducials/square_grid/standard/rotation/image00029.png";

		String outputName = new File(imagePath).getAbsolutePath();
		outputName = outputName.substring(0,outputName.length()-4)+".txt";

		BufferedImage image = UtilImageIO.loadImage(imagePath);

		new HandSelectPointsApp(image,outputName);
	}
}
