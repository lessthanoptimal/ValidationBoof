package validate.trackrect;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import georegression.struct.shapes.RectangleCorner2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class VisualizeResultsMilTrackData {


	public void visualize(String dataName, String inputName) throws IOException {
		System.out.println("Processing "+dataName);

		String path = "../data/track_rect/MILTrack/"+dataName;

//		RectangleCorner2D_F64 expected = new RectangleCorner2D_F64();
		RectangleCorner2D_F64 found = new RectangleCorner2D_F64();

//		BufferedReader readerTruth = new BufferedReader(new FileReader(path+"/"+dataName+"_gt.txt"));
		BufferedReader readerRect = new BufferedReader(new FileReader(inputName));

		ImagePanel gui = null;

		BufferedImage output = null;

		TldResults stats = new TldResults();

		int imageNum = 0;
		boolean firstImage = true;

		while( true ) {
			String imageName = String.format("%s/imgs/img%05d.png",path,imageNum);
			System.out.println(imageNum);

			BufferedImage image = UtilImageIO.loadImage(imageName);

			if( image == null ) {
				if( imageNum == 0 ) {
					System.out.println("Skipping index 0");
					imageNum++;
					continue;
				} else {
					throw new RuntimeException("Can't load "+imageName);
				}
			}

			if( firstImage ) {
				firstImage = false;
				output = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_BGR);
				output.createGraphics().drawImage(image,0,0,null);
				gui = new ImagePanel(output);
				ShowImages.showWindow(gui, dataName);
			} else {
				output.createGraphics().drawImage(image,0,0,null);
			}

//			UtilTldData.parseRect(readerTruth.readLine(),expected);
			UtilTldData.parseRect(readerRect.readLine(),found);

			Graphics2D g2 = output.createGraphics();

//			boolean isVisibleTruth = !Double.isNaN(expected.x0);
			boolean isVisibleFound = !Double.isNaN(found.x0);

//			if( isVisibleTruth ) {
//				g2.setColor(Color.RED);
//				UtilTldData.drawRectangle(expected,g2);
//			}

			if( isVisibleFound ) {
				g2.setStroke(new BasicStroke(3));
				g2.setColor(Color.BLUE);
				UtilTldData.drawRectangle(found,g2);
			}

//			UtilTldData.updateStatistics(expected,found,stats);

//			System.out.printf("  F = %6.3f      TP %5d TN %5d      FP %5d FN %5d\n",
//					UtilTldData.computeFMeasure(stats),stats.truePositives,stats.trueNegatives,stats.falsePositives,stats.falseNegatives);

			gui.repaint();

			imageNum++;
			BoofMiscOps.pause(20);
		}
	}




	public static void main(String[] args) throws IOException {

		VisualizeResultsMilTrackData visualizer = new VisualizeResultsMilTrackData();

//		String dataset = "cliffbar";
//		String dataset = "coke11";
		String dataset = "david";
//		String dataset = "04_pedestrian2";
//		String dataset = "05_pedestrian3";
//		String dataset = "06_car";
//		String dataset = "07_motocross";
//		String dataset = "08_volkswagen";
//		String dataset = "09_carchase";
//		String dataset = "10_panda";

//		String path = "../thirdparty/opentld_c";
//		String library = "copentld";
		String path = "./";
		String library = "BoofCV-Circulant";
		String inputFile = path+library+"_"+dataset+".txt";

		visualizer.visualize(dataset,inputFile);

		System.out.println("DONE!");
		System.exit(0);
	}
}
