package validate.applications;

import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.prefs.Preferences;

/**
 * Base class for hand selecting image features
 *
 * @author Peter Abeles
 */
public abstract class HandSelectBase {

	protected JComponent gui = new JPanel();

	JFrame frame;
	InfoHandSelectPanel infoPanel = new InfoHandSelectPanel(this);
	JComponent imagePanel;

	File inputFile;

	BufferedImage image;

	java.util.List<File> files;
	int selectedFile;

	public HandSelectBase( JComponent imagePanel, File openFile ) {
		this.imagePanel = imagePanel;
		gui.setLayout(new BorderLayout());
		gui.add(imagePanel,BorderLayout.CENTER);
		gui.add(infoPanel, BorderLayout.EAST);

		imagePanel.addMouseWheelListener(infoPanel);

		if( openFile == null ) {
			openImageDialog();
			if( inputFile == null ) {
				System.err.println("Can't open file");
				System.exit(0);
			}
		} else if( !openImage(openFile,false) ) {
			System.err.println("Failed to open file passed into constructor");
		}
	}

	public HandSelectBase(){}

	protected void handleFirstImage() {
		frame = ShowImages.showWindow(gui,getApplicationName());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	public boolean openImage( File f , boolean next ) {

		if( !f.exists() ) {
			System.err.println("File does not exist");
			return false;
		}

		if( !next ) {
			f = f.getAbsoluteFile();
			if( f.getParentFile() != null ) {
				File []array = f.getParentFile().listFiles();
				if( array == null )
					throw new RuntimeException("WTF no files?");
				files = Arrays.asList(array);
				Collections.sort(files);

				for (int i = 0; i < files.size(); i++) {
					if( files.get(i).getName().equals(f.getName())) {
						selectedFile = i;
						break;
					}
				}
			}
		}

		BufferedImage image = UtilImageIO.loadImage(f.getPath());
		if( image == null )
			return false;
		boolean firstImage = this.image == null;

		this.image = image;
		this.inputFile = f;

		clearPoints();
		process(f, image);

		// if huge scale image down
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		double scaleX = screenSize.getWidth()/(double)image.getWidth();
		double scaleY = screenSize.getHeight()/(double)image.getHeight();
		double scale = Math.min(scaleX,scaleY);
		if( scale < 1) {
			infoPanel.setScale(scale);
			setScale(scale);
		}

		if( firstImage ) {
			handleFirstImage();
		}
		frame.setTitle(f.getName());

		System.out.println("Opening image "+f.getName());

		return true;
	}

	public abstract void process(File file, BufferedImage image);

	public void openImageDialog() {
		String LAST_USED_FOLDER = "LAST_USED_FOLDER";

		Preferences prefs = Preferences.userRoot().node(getClass().getName());
		JFileChooser fc = new JFileChooser(prefs.get(LAST_USED_FOLDER,
				new File(".").getAbsolutePath()));

		int returnVal = fc.showOpenDialog(gui);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			prefs.put(LAST_USED_FOLDER, file.getParent());
			openImage(file,false);
		}
	}

	public void openNextImage() {
		if( inputFile == null )
			return;

		// save current results
		save();

		// select the next file
		for (selectedFile += 1; selectedFile < files.size(); selectedFile++) {
			File f = files.get(selectedFile);

			if( f.isDirectory() )
				continue;
			if( f.getName().endsWith("txt"))
				continue;

			File n = selectOutputFile(f);
			if( n != null && !(infoPanel.skipLabeled && n.exists()) ) {
				if( openImage(f,true) ) {
					return;
				}
			}
		}

		System.err.println("Couldn't find next");
	}

	public File selectOutputFile( File input ) {
		String path = input.getPath();
		//strip the suffic
		int s = path.lastIndexOf('.');
		if( s < 0 )
			return null;
		return new File(path.substring(0,s)+".txt");
	}

	public abstract String getApplicationName();

	public abstract void setScale( double scale );

	public abstract void clearPoints();

	public abstract void save();


}
