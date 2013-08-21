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
public class VisualizeResultsTldData{


	public void visualize(String dataName, String inputName) throws IOException {
		System.out.println("Processing "+dataName);

		String path = "../data/track_rect/TLD/"+dataName;

		RectangleCorner2D_F64 expected = new RectangleCorner2D_F64();
		RectangleCorner2D_F64 found = new RectangleCorner2D_F64();

		BufferedReader readerTruth = new BufferedReader(new FileReader(path+"/gt.txt"));
		BufferedReader readerRect = new BufferedReader(new FileReader(inputName));

		ImagePanel gui = null;

		BufferedImage output = null;

		TldResults stats = new TldResults();

		int imageNum = 0;


		while( true ) {
			String imageName1 = String.format("%s/%05d.jpg",path,imageNum+1);
			String imageName2 = String.format("%s/%05d.png",path,imageNum+1);

			BufferedImage image = UtilImageIO.loadImage(imageName1);
			if( image == null )
				image = UtilImageIO.loadImage(imageName2);
			if( image == null )
				break;

			if( imageNum == 0 ) {
				output = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_BGR);
				output.createGraphics().drawImage(image,0,0,null);
				gui = new ImagePanel(output);
				ShowImages.showWindow(gui, dataName);
			} else {
				output.createGraphics().drawImage(image,0,0,null);
			}

			UtilTldData.parseRect(readerTruth.readLine(),expected);
			UtilTldData.parseRect(readerRect.readLine(),found);

			Graphics2D g2 = output.createGraphics();

			boolean isVisibleTruth = !Double.isNaN(expected.x0);
			boolean isVisibleFound = !Double.isNaN(found.x0);

			if( isVisibleTruth ) {
				g2.setColor(Color.RED);
				UtilTldData.drawRectangle(expected,g2);
			}

			if( isVisibleFound ) {
				g2.setColor(Color.BLUE);
				UtilTldData.drawRectangle(found,g2);
			}

			UtilTldData.updateStatistics(expected,found,stats);

			System.out.printf("  F = %6.3f      TP %5d TN %5d      FP %5d FN %5d\n",
					UtilTldData.computeFMeasure(stats),stats.truePositives,stats.trueNegatives,stats.falsePositives,stats.falseNegatives);

			gui.repaint();

			imageNum++;
			BoofMiscOps.pause(10);
		}

		System.out.println("F-measure: "+UtilTldData.computeFMeasure(stats));

		System.out.println();
	}




	public static void main(String[] args) throws IOException {

		VisualizeResultsTldData visualizer = new VisualizeResultsTldData();

//		String dataset = "01_david";
//		String dataset = "02_jumping";
//		String dataset = "03_pedestrian1";
		String dataset = "04_pedestrian2";
//		String dataset = "05_pedestrian3";
//		String dataset = "06_car";
//		String dataset = "07_motocross";
//		String dataset = "08_volkswagen";
//		String dataset = "09_carchase";
//		String dataset = "10_panda";

//		String path = "../thirdparty/opentld_c";
//		String library = "copentld";
		String path = "./";
		String library = "BoofCV";
		String inputFile = path+"/"+library+"_"+dataset+".txt";

		visualizer.visualize(dataset,inputFile);

		System.out.println("DONE!");
		System.exit(0);
	}
}
