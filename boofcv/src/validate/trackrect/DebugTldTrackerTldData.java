package validate.trackrect;

import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TldVisualizationPanel;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.RectangleCorner2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Peter Abeles
 */
public class DebugTldTrackerTldData<T extends ImageSingleBand> implements TldVisualizationPanel.Listener {

	T input;
	boolean paused = false;

	public DebugTldTrackerTldData(ImageType<T> type) {
		input = type.createImage(1,1);
	}

	public void evaluate( String dataName , TldTracker<T,?> tracker ) {
		System.out.println("Processing "+dataName);

		String path = "../data/track_rect/TLD/"+dataName;

		RectangleCorner2D_F64 initial = UtilTldData.parseRectangle(path + "/init.txt");
		RectangleCorner2D_F64 found = new RectangleCorner2D_F64();

		TldVisualizationPanel gui = null;

		String imageType = new File(path+"/00001.jpg").exists() ? "jpg" : "png";

		int imageNum = 0;
		while( true ) {
			String imageName = String.format("%s/%05d.%s",path,imageNum+1,imageType);
			BufferedImage image = UtilImageIO.loadImage(imageName);
			if( image == null )
				break;

			input.reshape(image.getWidth(),image.getHeight());
			ConvertBufferedImage.convertFrom(image,input,true);

			boolean detected;

			if( imageNum == 0 ) {
				gui = new TldVisualizationPanel(this);
				gui.setFrame(image);
				gui.setSelectRectangle(false);
				ShowImages.showWindow(gui,dataName);
				tracker.initialize(input,(int)initial.x0,(int)initial.y0,(int)initial.x1,(int)initial.y1);
				detected = true;
			} else {
				detected = tracker.track(input);
				found.set(tracker.getTargetRegion());
			}

			if( !detected ) {
				System.out.println("No Detection");
			} else {
				System.out.printf("Detection: %f,%f,%f,%f\n",found.x0,found.y0,found.x1,found.y1);

				Graphics2D g2 = image.createGraphics();

				int w = (int)found.getWidth();
				int h = (int)found.getHeight();

				g2.drawRect((int)found.x0,(int)found.y0,w,h);
			}

			gui.setFrame(image);
			gui.update(tracker,detected);
			gui.repaint();

			imageNum++;

			while( paused ) {
				Thread.yield();
			}

//			BoofMiscOps.pause(30);
		}
		System.out.println();
	}

	public static void evaluate( String dataset ) {
		Class type = ImageFloat32.class;

		DebugTldTrackerTldData generator = new DebugTldTrackerTldData(ImageType.single(type));

		TldTracker tracker = new TldTracker(new TldConfig(true,type));

		generator.evaluate(dataset,tracker);
	}

	@Override
	public void startTracking(int x0, int y0, int x1, int y1) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void togglePause() {
		paused = !paused;
	}

	public static void main(String[] args) {
		evaluate("01_david");
//		evaluate("02_jumping");
//		evaluate("03_pedestrian1");
//		evaluate("04_pedestrian2");
//		evaluate("05_pedestrian3");
//		evaluate("06_car");
//		evaluate("07_motocross");
//		evaluate("08_volkswagen");
//		evaluate("09_carchase");
//		evaluate("10_panda");

		System.out.println("DONE!");
	}
}
