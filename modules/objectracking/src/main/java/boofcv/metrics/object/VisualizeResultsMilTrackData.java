package boofcv.metrics.object;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import georegression.struct.shapes.Rectangle2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static boofcv.metrics.object.GenerateDetectionsMilTrackData.parseFramesFile;

/**
 * @author Peter Abeles
 */
public class VisualizeResultsMilTrackData {


	public void visualize(String dataName, String inputName) throws IOException {
		System.out.println("Processing "+dataName);

		String path = "data/track_rect/MILTrack/"+dataName;

		Rectangle2D_F64 expected = new Rectangle2D_F64();
		Rectangle2D_F64 found = new Rectangle2D_F64();

		BufferedReader readerTruth = new BufferedReader(new FileReader(path + "/" + dataName + "_gt.txt"));
		BufferedReader readerRect = new BufferedReader(new FileReader(inputName));

		ImagePanel gui = null;

		BufferedImage output = null;

		TldResults stats = new TldResults();
		FooResults statsFoo = new FooResults();

		int frames[] = parseFramesFile(path + "/" + dataName + "_frames.txt");
		int frameNum = frames[0];
		boolean firstImage = true;

		while( frameNum <= frames[1] ) {

			String imageName = String.format("%s/imgs/img%05d.png",path,frameNum);

			BufferedImage image = UtilImageIO.loadImage(imageName);

			if( image == null ) {
				if( frameNum == 0 ) {
					System.out.println("Skipping index 0");
					frameNum++;
					continue;
				} else {
					break;
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

			String lineTruth = readerTruth.readLine();
			if( lineTruth != null )
				UtilTldData.parseRectWH(lineTruth,expected);
			else
				expected.setTo(0,0,0,0);
			UtilTldData.parseFRect(readerRect.readLine(),found);

			Graphics2D g2 = output.createGraphics();

			boolean hasTruth = !(expected.p0.x == 0 && expected.p0.y == 0 && expected.p1.x == 0 && expected.p1.y == 0);

			boolean isVisibleFound = !Double.isNaN(found.p0.x);
			if( isVisibleFound ) {
				g2.setStroke(new BasicStroke(3));
				g2.setColor(Color.BLUE);
				UtilTldData.drawRectangle(found,g2);
			}

			if( hasTruth ) {
				boolean isVisibleTruth = !Double.isNaN(expected.p0.x);

				if( isVisibleTruth ) {
					g2.setColor(Color.RED);
					UtilTldData.drawRectangle(expected,g2);
				}

				UtilTldData.updateStatistics(expected,found,stats);
				UtilTldData.updateStatistics(expected,found,statsFoo);

				System.out.printf("  F = %6.3f      TP %5d TN %5d      FP %5d FN %5d\n",
						UtilTldData.computeFMeasure(stats),stats.truePositives,stats.trueNegatives,stats.falsePositives,stats.falseNegatives);
			}

			gui.repaint();

			frameNum++;
			BoofMiscOps.pause(30);
		}
		System.out.println("F-measure: "+UtilTldData.computeFMeasure(stats));
		System.out.println("Average overlap "+(statsFoo.totalOverlap/statsFoo.truePositive));
	}

	public static void main(String[] args) throws IOException {

		VisualizeResultsMilTrackData visualizer = new VisualizeResultsMilTrackData();

//		String dataset = "cliffbar";
//		String dataset = "coke11";
//		String dataset = "david";
//		String dataset = "dollar";
//		String dataset = "faceocc";
//		String dataset = "faceocc2";
//		String dataset = "girl";
//		String dataset = "surfer";
//		String dataset = "sylv";
//		String dataset = "tiger1";
		String dataset = "tiger2";
//		String dataset = "twinings";

//		String path = "../thirdparty/opentld_c";
//		String library = "copentld";
		String path = "./";
		String library = "BoofCV-TLD";
//		String library = "BoofCV-Circulant";
//		String library = "PCirculant";
//		String library = "BoofCV-CirculantOrig";
//		String library = "BoofCV-SFT";
//		String library = "BoofCV-MeanShift";
		String inputFile = path+library+"_"+dataset+".txt";

		visualizer.visualize(dataset,inputFile);

		System.out.println("DONE!");
		System.exit(0);
	}
}
