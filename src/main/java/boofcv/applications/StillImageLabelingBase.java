package boofcv.applications;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
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
public abstract class StillImageLabelingBase {

	protected JComponent gui = new JPanel();
	private ImageZoomPanel imagePanel;
	JFrame frame;

	File inputFile;

	BufferedImage image;

	java.util.List<File> files;
	int selectedFile;

	// used to detect spamming of reload
	private volatile boolean openingImage = false;

	protected JMenuBar menuBar;
	protected JMenu menuFile;

	{
		try {
			// In Mac OS X Display the menubar in the correct location
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			// smoother font
			System.setProperty("apple.awt.textantialiasing", "true");
		} catch( Exception ignore ) {

		}
	}

	protected void initialize( File openFile ) {
		gui.setLayout(new BorderLayout());
		initializeGui(gui);
		if (imagePanel ==null)
			throw new RuntimeException("Must assign 'imagePanel' inside of initializeGui()");

		if( openFile == null ) {
			BoofSwingUtil.invokeNowOrLater(() -> {
				openImageDialog();
				if( inputFile == null ) {
					System.err.println("Can't open file");
					System.exit(0);
				}
			});
		} else if( !openImage(openFile,false) ) {
			System.err.println("Failed to open file passed into constructor");
		}
	}

	protected void setImagePanel(ImageZoomPanel panel) {
		this.imagePanel = panel;
	}

	protected abstract void initializeGui(JComponent panel);

	protected void createMenuBar() {
		menuBar = new JMenuBar();

		menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menuFile);

		JMenuItem menuItemFile = new JMenuItem("Open Image");
		BoofSwingUtil.setMenuItemKeys(menuItemFile, KeyEvent.VK_O, KeyEvent.VK_O);
		menuItemFile.addActionListener(e-> openImageDialog());
		menuFile.add(menuItemFile);

		JMenuItem menuItemNext = new JMenuItem("Next Image");
		BoofSwingUtil.setMenuItemKeys(menuItemNext, KeyEvent.VK_I, KeyEvent.VK_I);
		menuItemNext.addActionListener(e->openNextImage());
		menuFile.add(menuItemNext);

		JMenuItem menuItemClear = new JMenuItem("Clear");
		menuItemClear.addActionListener(e-> clearLabels());
		menuFile.add(menuItemClear);

		JMenuItem menuItemSave = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuItemSave, KeyEvent.VK_S, KeyEvent.VK_S);
		menuItemSave.addActionListener(e-> save());
		menuFile.add(menuItemSave);

		frame.setJMenuBar(menuBar);
	}

	protected void handleFirstImage() {
		frame = ShowImages.showWindow(gui,getApplicationName());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent windowEvent) {
				adjustImageScale();
			}
		});
		createMenuBar();

		Rectangle screenSize = frame.getGraphicsConfiguration().getBounds();
		Insets inset = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());

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

		clearLabels();
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
		imagePanel.setScale(scale);
		setScale(scale);
	}

	public abstract void process(File file, BufferedImage image);

	public void openImageDialog() {
		File file = BoofSwingUtil.openFileChooser(gui, BoofSwingUtil.FileTypes.IMAGES);

		if (file != null) {
			openImage(file,false);
		}
	}

	public abstract void openNextImage();

	public abstract String getApplicationName();

	public abstract void setScale( double scale );

	/** Called when the user wishes to save the current results to disk */
	public abstract void save();

	/** Called when the user wishes to clear all labels inside the image */
	public abstract void clearLabels();

	public void repaint() {
		gui.repaint();
	}

	public void reloadImage() {
		if( openingImage ) {
			System.err.println("Still opening a file");
			return;
		}
		new Thread(() -> openImage(inputFile,true)).start();
	}
}
