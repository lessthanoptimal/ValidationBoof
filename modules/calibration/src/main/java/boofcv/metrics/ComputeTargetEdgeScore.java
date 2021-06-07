package boofcv.metrics;

import java.io.File;
import java.util.List;

/**
 * Since reprojection error on detected calibration points in not entirely trust worthy, see example, this computes
 * the difference along the projected edge of the calibration pattern. The edge score is the average difference between
 * a pixel on the target's edge and one just outside.
 *
 * @author Peter Abeles
 */
public class ComputeTargetEdgeScore {

	public static List<CalibValidPoint> loadChessboard( File file ) {
		return null;

	}
}
