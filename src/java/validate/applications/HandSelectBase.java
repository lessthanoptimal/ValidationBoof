package validate.applications;

import boofcv.gui.image.ShowImages;

import javax.swing.*;
import java.awt.*;

/**
 * Base class for hand selecting image features
 *
 * @author Peter Abeles
 */
public abstract class HandSelectBase {
	String outputName;

	JPanel gui = new JPanel();

	InfoHandSelectPanel infoPanel = new InfoHandSelectPanel(this);

	public HandSelectBase( String outputName ) {
		this.outputName = outputName;
	}

	protected void initGui( int width , int height , JComponent imagePanel ) {
		gui.setLayout(new BorderLayout());
		gui.add(imagePanel,BorderLayout.CENTER);
		gui.add(infoPanel, BorderLayout.EAST);

		imagePanel.addMouseWheelListener(infoPanel);

		// scale the image if it's huge
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		double scaleX = screenSize.getWidth()/(double)width;
		double scaleY = screenSize.getHeight()/(double)height;
		double scale = Math.min(scaleX,scaleY);
		if( scale < 1) {
			infoPanel.setScale(scale);
			setScale(scale);
		}

		ShowImages.showWindow(gui,getApplicationName()).setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	public abstract String getApplicationName();

	public abstract void setScale( double scale );

	public abstract void clearPoints();

	public abstract void save();
}
