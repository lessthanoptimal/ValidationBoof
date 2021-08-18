package boofcv.metrics.ecocheck;

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.app.PaperSize;
import boofcv.generate.Unit;
import georegression.struct.point.Point2D_F64;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

/**
 * Creates a file which specifies where the corners are
 *
 * @author Peter Abeles
 */
public class CreateEcoCheckCornerFile {
    public PaperSize paper = PaperSize.LETTER;
    public Unit unit = Unit.CENTIMETER;

    public void generate(ConfigECoCheckMarkers config) {
        ECoCheckUtils utils = new ECoCheckUtils();
        config.convertToGridList(utils.markers);
        utils.fixate();

        ConfigECoCheckMarkers.MarkerShape shape = config.markerShapes.get(0);

        double paperWidth = paper.convertWidth(unit);
        double paperHeight = paper.convertHeight(unit);

        List<Point2D_F64> corners = utils.createCornerList(0, shape.squareSize);

        try (PrintStream out = new PrintStream("ecocheck_corners_" + config.compactName() + ".txt")) {
            out.printf("# Location of ECoCheck Corners: rows=%d cols=%d error=%d markers=%d\n",
                    shape.numRows, shape.numCols, config.errorCorrectionLevel, config.firstTargetDuplicated);
            out.println("paper=" + paper.name);
            out.println("units=" + unit.name());
            out.println("count=" + corners.size());
            for (int i = 0; i < corners.size(); i++) {
                Point2D_F64 p = corners.get(i);
                // These corners are going to be in a coordinate system that has the origin in the document center
                // that needs to be changed to the document's top left corner
                double x = paperWidth / 2 + p.x;
                double y = paperHeight / 2 + p.y;

                out.printf("%.8f %.8f\n", x, y);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        CreateEcoCheckCornerFile app = new CreateEcoCheckCornerFile();
        ConfigECoCheckMarkers config = ConfigECoCheckMarkers.singleShape(9, 7, 3.0, 1);
        app.generate(config);
        config.errorCorrectionLevel = 0;
        app.generate(config);
    }
}
