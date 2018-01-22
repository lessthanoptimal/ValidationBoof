package boofcv.metrics;

import boofcv.abst.distort.PointDeformKeyPoints;
import georegression.geometry.UtilPoint2D_F32;
import georegression.struct.point.Point2D_F32;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Regression test designed to test change in output. A hash is created using the difference between input point
 * and output point location.
 *
 * @author Peter Abeles
 */
public class ChangeOutputPointDeformKeyPoints {

    public PrintStream out = System.out;
    public PrintStream err = System.err;

    public void process( String algName , PointDeformKeyPoints alg ) {

        List<Point2D_F32> src = new ArrayList<>();
        List<Point2D_F32> dst = new ArrayList<>();

        src.add( new Point2D_F32(5,10));
        src.add( new Point2D_F32(100,100));
        src.add( new Point2D_F32(100,105));
        src.add( new Point2D_F32(195,89));

        dst.add( new Point2D_F32(90,16));
        dst.add( new Point2D_F32(110,120));
        dst.add( new Point2D_F32(95,70));
        dst.add( new Point2D_F32(130,80));

        int height = 150;
        int width = 200;

        alg.setImageShape(width, height);
        alg.setSource(src);
        alg.setDestination(dst);

        Point2D_F32 p = new Point2D_F32();
        float difference = 0;
        for (int y = 0; y < height; y += 4) {
            for (int x = 0; x < width; x += 4) {
                alg.compute(x, y, p);
                difference += UtilPoint2D_F32.distance(x, y, p.x, p.y);
            }
        }
        difference /= (width / 4) * (height / 4);
        out.printf("%20s change-hash=%18.12f\n",algName,difference);
    }
}
