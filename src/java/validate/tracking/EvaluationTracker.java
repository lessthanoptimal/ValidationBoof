package validate.tracking;

import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Interface for tracking, used to evaluate the tracker's performance
 *
 * @author Peter Abeles
 */
public interface EvaluationTracker<T extends ImageGray> {

	/**
	 * Updates tracks locations
	 */
	public void track( T image );

	/**
	 * Sets the tracker into its initial state
	 */
	public void reset();

	/**
	 * Returns the initial location of all active tracks
	 */
	public List<Point2D_F64> getInitial();

	/**
	 * Returns current location of all active tracks
	 */
	public List<Point2D_F64> getCurrent();
}
