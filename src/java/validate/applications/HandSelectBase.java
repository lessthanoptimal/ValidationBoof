package validate.applications;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/**
 * Base class for hand selecting image features
 *
 * @author Peter Abeles
 */
public abstract class HandSelectBase {

	protected JComponent gui = new JPanel();

	JFrame frame;
	InfoHandSelectPanel infoPanel = new InfoHandSelectPanel(this);
	ImageZoomPanel imagePanel;

	File inputFile;

	BufferedImage image;

	java.util.List<File> files;
	int selectedFile;

	// used to detect spamming of reload
	private volatile boolean openingImage = false;

	public HandSelectBase( ImageZoomPanel imagePanel, File openFile ) {
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
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent windowEvent) {
				adjustImageScale();
			}
		});

		Rectangle screenSize = frame.getGraphicsConfiguration().getBounds();
		Insets inset = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());

		System.out.println("insert "+inset);

		screenSize.width -= 200;
		screenSize.height -= inset.bottom;

		int w = image.getWidth();
		int h = image.getHeight();

		// Make sure it doesn't open an image larger than the monitor
		w = Math.min(screenSize.width,w);
		h = Math.min(screenSize.height,h);

		double scale = Math.min(w/(double)image.getWidth(),h/(double)image.getHeight());
		scale = Math.min(1.0,scale);
		int width = Math.min((int)(image.getWidth()*scale),w);
		int height = Math.min((int)(image.getHeight()*scale),h);

		imagePanel.setPreferredSize(new Dimension(width,height));
		frame.pack();
//		frame.setSize(new Dimension(w,h));
	}

	public boolean openImage( final File f , boolean next ) {

		if( !f.exists() ) {
			System.err.println("File does not exist");
			return false;
		}
		openingImage = true;

		if( !next ) {
			File ff = f.getAbsoluteFile();
			if( ff.getParentFile() != null ) {
				File []array = ff.getParentFile().listFiles();
				if( array == null ) {
					openingImage = false;
					throw new RuntimeException("WTF no files?");
				}
				files = Arrays.asList(array);
				Collections.sort(files);

				for (int i = 0; i < files.size(); i++) {
					if( files.get(i).getName().equals(ff.getName())) {
						selectedFile = i;
						break;
					}
				}
			}
		}

		BufferedImage image = UtilImageIO.loadImage(f.getPath());
		if( image == null ) {
			openingImage = false;
			return false;
		}
		boolean firstImage = this.image == null;

		this.image = image;
		this.inputFile = f;

		clearPoints();
		process(f, image);

		if( firstImage ) {
			handleFirstImage();
		}

		BoofSwingUtil.invokeNowOrLater(() -> {
			adjustImageScale();
			frame.setTitle(getApplicationName()+" "+f.getName());
		});

		System.out.println("Opening image "+f.getName());

		openingImage = false;
		return true;
	}

	private void adjustImageScale() {
		double scale = BoofSwingUtil.selectZoomToShowAll(imagePanel,image.getWidth(),image.getHeight());
		scale = Math.min(1,scale);
		infoPanel.setScale(scale);
		setScale(scale);
	}

	public abstract void process(File file, BufferedImage image);

	public void openImageDialog() {
		File file = BoofSwingUtil.openFileChooser(gui, BoofSwingUtil.FileTypes.IMAGES);

		if (file != null) {
			openImage(file,false);
		}
	}

	public void openNextImage() {
		if( inputFile == null )
			return;

		// save current results
		if( infoPanel.prefix.length() == 0 ) { // Only save automatically if the user is not viewing generated results
			boolean save = true;
			if( selectOutputFile(inputFile).exists() ) {
				save = false;
//				int dialogResult = JOptionPane.showConfirmDialog(gui, "Output already exists. Save? ",
//						"Warning",JOptionPane.OK_CANCEL_OPTION);
//				if(dialogResult == JOptionPane.CANCEL_OPTION){
//					save = false;
//				}
			}
			if( save )
				save();
		}

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

		String path = input.getParent();
		String name = input.getName();
		//strip the suffic
		int s = name.lastIndexOf('.');
		if( s < 0 )
			return null;

		return new File(path,infoPanel.prefix+name.substring(0,s)+".txt");
	}

	public abstract String getApplicationName();

	public abstract void setScale( double scale );

	public abstract void clearPoints();

	public abstract void save();

	public void reloadImage() {
		if( openingImage ) {
			System.err.println("Still opening a file");
			return;
		}
		new Thread(){
			public void run() {
				openImage(inputFile,true);
			}
		}.start();
	}

//	public abstract void reloadLabeled();


}
