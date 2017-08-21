package validate.applications;

import georegression.struct.point.Point2D_F64;
import validate.misc.PointFileCodec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 *
 *
 * @author Peter Abeles
 */
public class HandSelectPointsApp extends HandSelectBase {



	public HandSelectPointsApp( File file ) {
		super(new SelectPointPanel(),file);
	}

	@Override
	public void process(File file, BufferedImage image) {
		SelectPointPanel gui = (SelectPointPanel)this.imagePanel;

		File outputFile = selectOutputFile(file);

		if( outputFile.exists() ) {
			List<List<Point2D_F64>> sets = PointFileCodec.loadSets(outputFile.getPath());
			if( sets == null ) {
				gui.addPointSet(PointFileCodec.load(outputFile.getPath()));
			} else {
				gui.setSets(sets);
			}
		}
		infoPanel.setImageShape(image.getWidth(),image.getHeight());
		gui.setBufferedImage(image);
	}

	@Override
	public void save() {
		SelectPointPanel gui = (SelectPointPanel)this.imagePanel;
		File outputFile = selectOutputFile(inputFile);
		List<List<Point2D_F64>> points = gui.getSelectedPoints();

		if( points.size() == 1 ) {
			PointFileCodec.save(outputFile.getPath(), "list of hand selected 2D points", points.get(0));
			System.out.println("Saved to " + outputFile.getPath());
		} else if( points.size() > 1 ){
			PointFileCodec.saveSets(outputFile.getPath(), "list of hand selected 2D points", points);
			System.out.println("Saved to " + outputFile.getPath());
		}
	}

	@Override
	public String getApplicationName() {
		return "Select Point Features";
	}

	@Override
	public void setScale( double scale ) {
		SelectPointPanel gui = (SelectPointPanel)this.imagePanel;
		gui.setScale(scale);
	}

	@Override
	public void clearPoints() {
		SelectPointPanel gui = (SelectPointPanel)this.imagePanel;
		gui.clearPoints();
		gui.repaint();
	}

	public static void main(String[] args) {
		new HandSelectPointsApp(null);
	}
}
