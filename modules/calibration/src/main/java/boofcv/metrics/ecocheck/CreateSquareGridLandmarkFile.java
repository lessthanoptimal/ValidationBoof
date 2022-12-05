package boofcv.metrics.ecocheck;

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.alg.fiducial.calib.hammingchess.HammingChessboardGenerator;
import boofcv.alg.fiducial.calib.hamminggrids.HammingGridGenerator;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.generate.PaperSize;
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
public class CreateSquareGridLandmarkFile {
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
            for (int cornerID = 0; cornerID < corners.size(); cornerID++) {
                Point2D_F64 p = corners.get(cornerID);
                // These corners are going to be in a coordinate system that has the origin in the document center
                // that needs to be changed to the document's top left corner
                double x = paperWidth / 2.0 + p.x;
                double y = paperHeight / 2.0 + p.y;

                out.printf("%d %.8f %.8f\n", cornerID, x, y);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void generate(ConfigHammingChessboard config) {
        double paperWidth = paper.convertWidth(unit);
        double paperHeight = paper.convertHeight(unit);

        var generator = new HammingChessboardGenerator(config);
        generator.saveCornerLocations();

        List<Point2D_F64> corners = generator.corners;

        double offsetX = paperWidth/2.0 - config.getMarkerWidth()/2.0;
        double offsetY = paperHeight/2.0 - config.getMarkerHeight()/2.0;

        try (PrintStream out = new PrintStream("chessboard_corners_" + config.numRows+ "x" + config.numCols + ".txt")) {
            out.printf("# Location of Hamming Chessboard Corners: rows=%d cols=%d square=%f\n",
                    config.numRows, config.numCols, config.squareSize);
            out.println("paper=" + paper.name);
            out.println("units=" + unit.name());
            out.println("count=" + corners.size());
            for (int cornerID = 0; cornerID < corners.size(); cornerID++) {
                Point2D_F64 p = corners.get(cornerID);
                out.printf("%d %.8f %.8f\n", cornerID, offsetX + p.x, offsetY + p.y);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void generate(ConfigHammingGrid config) {
        double paperWidth = paper.convertWidth(unit);
        double paperHeight = paper.convertHeight(unit);

        var generator = new HammingGridGenerator(config);
        generator.saveCornerLocations();

        double offsetX = paperWidth/2.0 - config.getMarkerWidth()/2.0;
        double offsetY = paperHeight/2.0 - config.getMarkerHeight()/2.0;

        List<Point2D_F64> corners = generator.corners;

        try (PrintStream out = new PrintStream("squaregrid_corners_" + config.numRows+ "x" + config.numCols+ ".txt")) {
            out.printf("# Location of Hamming Grid Corners: rows=%d cols=%d square=%f space=%f\n",
                    config.numRows, config.numCols, config.squareSize, config.spaceToSquare);
            out.println("paper=" + paper.name);
            out.println("units=" + unit.name());
            out.println("count=" + corners.size());
            for (int cornerID = 0; cornerID < corners.size(); cornerID++) {
                Point2D_F64 p = corners.get(cornerID);
                    out.printf("%d %.8f %.8f\n", cornerID, offsetX + p.x, offsetY + p.y);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generic generator for a square grid
     */
    public void generate(String name, int rows, int cols, double squareSize) {
        double offsetX = (paper.convertWidth(unit) - (cols - 1) * squareSize) / 2;
        double offsetY = (paper.convertHeight(unit) - (rows - 1) * squareSize) / 2;

        generateOffset(name, rows, cols, squareSize, offsetX, offsetY);
    }

    /**
     * Generic generator for a square grid
     */
    public void generateOffset(String name, int rows, int cols, double squareSize,
                               double offsetX, double offsetY) {
        try (PrintStream out = new PrintStream(name + ".txt")) {
            out.printf("# Grid marker: rows=%d cols=%d square=%f\n", rows, cols, squareSize);
            out.println("paper=" + paper.name);
            out.println("units=" + unit.name());
            out.println("count=" + rows * cols);

            for (int row = 0; row < rows; row++) {
                double y = row * squareSize + offsetY;
                for (int col = 0; col < cols; col++) {
                    double x = col * squareSize + offsetX;
                    int landmarkID = row * cols + col;
                    out.printf("%d %.8f %.8f\n", landmarkID, x, y);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void generate(String name, int rows, int cols, double squareSize, double space) {
        try (PrintStream out = new PrintStream(name + ".txt")) {
            out.printf("# Grid marker: rows=%d cols=%d square=%f\n", rows, cols, squareSize);
            out.println("paper=" + paper.name);
            out.println("units=" + unit.name());
            out.println("count=" + rows * cols * 4);

            double markerWidth = cols * (squareSize + space) - space;
            double markerHeight = rows * (squareSize + space) - space;

            double offsetX = (paper.convertWidth(unit) - markerWidth) / 2;
            double offsetY = (paper.convertHeight(unit) - markerHeight) / 2;

            int cornerCols = cols * 2;

            for (int row = 0; row < rows; row++) {
                double y = row * (squareSize + space) + offsetY;
                for (int col = 0; col < cols; col++) {
                    double x = col * (squareSize + space) + offsetX;
                    int landmarkID = 2 * (rows - row - 1) * cornerCols + col * 2;
                    out.printf("%d %.8f %.8f\n", landmarkID + cornerCols, x, y);
                    out.printf("%d %.8f %.8f\n", landmarkID + 1 + cornerCols, x + squareSize, y);
                    out.printf("%d %.8f %.8f\n", landmarkID + 1, x + squareSize, y + squareSize);
                    out.printf("%d %.8f %.8f\n", landmarkID, x, y + squareSize);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        CreateSquareGridLandmarkFile app = new CreateSquareGridLandmarkFile();
        ConfigECoCheckMarkers config = ConfigECoCheckMarkers.singleShape(9, 7, 1, 3.0);
        app.generate(config);
        config.errorCorrectionLevel = 0;
//        app.generate(config);
        app.generate(ConfigHammingChessboard.create(HammingDictionary.ARUCO_ORIGINAL, 8,6, 3.0));
        app.generate(ConfigHammingGrid.create(HammingDictionary.ARUCO_ORIGINAL, 7,5, 3.0, 0.2));
//        app.generate("charuco_6X8", 7, 5, 3.0);
//        app.generate("charuco_6X8", 7, 5, 3.0);
//        app.generate("aruco_grids/corners_7x5", 7, 5, 3.0, 0.8);
//        double offset = 1.0/80.0;
//        app.generateOffset("caltag", 8, 6, 3.0, 0.75+1.5-offset, 0.75+1.5-offset);
    }
}
