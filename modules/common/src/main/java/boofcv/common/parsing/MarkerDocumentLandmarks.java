package boofcv.common.parsing;

import boofcv.generate.PaperSize;
import boofcv.generate.Unit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

/**
 * Location of landmarks on a marker printed on a document.
 *
 * @author Peter Abeles
 */
public class MarkerDocumentLandmarks {
    public final PaperSize paper;
    public final Unit units;
    public final DogArray<Point2D_F64> landmarks = new DogArray<>(Point2D_F64::new);

    public MarkerDocumentLandmarks(PaperSize paper, Unit units) {
        this.paper = paper;
        this.units = units;
    }
}
