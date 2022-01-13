package boofcv.applications;

import boofcv.common.misc.PointFileCodec;
import boofcv.io.UtilIO;
import georegression.struct.point.Point2D_F64;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

/**
 * Converts a polygon file into a list of marker landmarks
 *
 * @author Peter Abeles
 */
public class LabeledPolygonsToMarkerLandmark {
    public static void main(String[] args) throws FileNotFoundException {
        for (String directory : new String[]{"angled", "lots", "small", "standard"}) {
            List<String> foundImages = UtilIO.listSmartImages(String.format("glob:data/fiducials/microqr/%s/*.jpg", directory), true);
            for (String imagePath : foundImages) {
                File imageFile = new File(imagePath);
                String base = FilenameUtils.getBaseName(imageFile.getName());
                String truthName = base + ".txt";
                File truthFile = new File(imageFile.getParent(), truthName);
                if (!truthFile.exists())
                    continue;

                System.out.println("converting " + truthFile.getPath());

                List<List<Point2D_F64>> truth = PointFileCodec.loadSets(truthFile);
                var out = new PrintStream(new File(imageFile.getParentFile(), "landmarks_" + base + ".txt"));

                out.println("# True marker landmark locations\n" +
                        "image.shape=1x1\n" +
                        "markers.size=" + truth.size());
                for (List<Point2D_F64> bounds : truth) {
                    out.println("marker=0\n" +
                            "landmarks.size=" + bounds.size());
                    for (int landmarkIdx = 0; landmarkIdx < bounds.size(); landmarkIdx++) {
                        Point2D_F64 p = bounds.get(landmarkIdx);
                        out.printf("%d %f %f\n", landmarkIdx, p.x, p.y);
                    }
                }
            }
        }

    }
}
