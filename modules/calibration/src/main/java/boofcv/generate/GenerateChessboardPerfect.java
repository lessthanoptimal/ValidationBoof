package boofcv.generate;

import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.io.image.UtilImageIO;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates chessboard images with noise free data. This can be a pathological case
 *
 * @author Peter Abeles
 */
public class GenerateChessboardPerfect {
	public static void main(String[] args) throws FileNotFoundException {
		int padding = 25;
		RenderCalibrationTargetsGraphics2D render = new RenderCalibrationTargetsGraphics2D(padding,1.0);

		int count = 0;

		// Each directory only supports one shape for now
		for( int[]shape : new int[][]{{12,8,50},{12,8,5}}) {
			int rows = shape[0];
			int cols = shape[1];
			int side = shape[2];

			render.chessboard(rows, cols, side);

			BufferedImage template = render.getBufferred();
			BufferedImage output = new BufferedImage(template.getWidth(), template.getHeight(), template.getType());

			// generate images with offsets as a way to flush out more pathological conditions
			for (int offY = 0; offY < 2; offY++) {
				for (int offX = 0; offX < 2; offX++) {
					Graphics2D g2 = output.createGraphics();
					g2.setColor(Color.WHITE);
					g2.fillRect(0, 0, output.getWidth(), output.getHeight());
					g2.drawImage(template, offX, offY, null);

					String name = String.format("image%02d", count++);
					UtilImageIO.saveImage(output, name + ".png");
					List<Point2D_F64> corners = createCorners(rows, cols, offX, offY, padding, side);

					PrintStream out = new PrintStream(name + ".txt");
					out.println("# Generated chessboard image");
					for (Point2D_F64 p : corners) {
						out.printf("%f %f\n", p.x, p.y);
					}
					out.close();
				}
			}
		}
	}

	public static List<Point2D_F64> createCorners(int rows, int cols , int offX , int offY, int padding , int width ) {
		List<Point2D_F64> list = new ArrayList<>();
		for (int y = 1; y < rows; y++) {
			int yy = padding+offY+y*width;
			for (int x = 1; x < cols; x++) {
				int xx = padding+offX+x*width;
				list.add( new Point2D_F64(xx,yy));
			}
		}
		return list;
	}
}
