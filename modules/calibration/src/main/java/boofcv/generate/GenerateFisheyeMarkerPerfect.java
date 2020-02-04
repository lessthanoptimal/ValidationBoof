package boofcv.generate;

import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.gui.image.ShowImages;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraUniversalOmni;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class GenerateFisheyeMarkerPerfect {
	public static void main(String[] args) throws FileNotFoundException {
		String baseName = "fisheye";

		int padding = 25;
		RenderCalibrationTargetsGraphics2D render = new RenderCalibrationTargetsGraphics2D(padding,1.0);
		render.chessboard(7, 5, 30);

		CameraUniversalOmni model = CalibrationIO.load("data/calibration_mono/chessboard/ricoh_theta_5/camera_calibration.yaml");
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		double markerWidth = 2.5;

		Se3_F64 fidToWorld = SpecialEuclideanOps_F64.eulerXyz(0.6,0.0,0.8,0,Math.PI+0.1,-0.4,null);
		simulator.addSurface(fidToWorld,markerWidth,render.getGrayF32());
		simulator.render();
		BufferedImage output = ConvertBufferedImage.convertTo(simulator.getOutput(),null);

		double pixel_to_unit = markerWidth/(render.getGrayF32().width);
		double width_to_height = render.getGrayF32().height/(double)render.getGrayF32().width;
		Point2D_F64 pixel = new Point2D_F64();
		PrintStream out = new PrintStream(baseName + ".txt");
		out.println("# Generated chessboard image");
		List<Point2D_F64> pixels = new ArrayList<>();
		for (Point2D_F64 p : render.getLandmarks() ) {
			double xx = p.x*pixel_to_unit-markerWidth/2;
			double yy = p.y*pixel_to_unit-markerWidth*width_to_height/2;
			simulator.computePixel(0,xx,yy,pixel);
			pixels.add(pixel.copy());
			out.printf("%f %f\n", pixel.x, pixel.y);
		}
		out.close();

		UtilImageIO.saveImage(output,baseName+".png");

		// Visualize for debugging purposes
		Graphics2D g2 = output.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Color.RED);
		Ellipse2D ellipse = new Ellipse2D.Double();
		for (Point2D_F64 p : pixels ) {
			ellipse.setFrame(p.x-3,p.y-3,6,6);
			g2.draw(ellipse);
		}

		ShowImages.showWindow(render.getBufferred(),"Moo", true);
		ShowImages.showWindow(output,baseName, true);
	}
}
