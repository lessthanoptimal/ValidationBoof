package validate.calib;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector2D_F64;

/**
 * Point which lies on the edge of a feature in the calibration target.  Vector says where the dark part should be.
 *
 * @author Peter Abeles
 */
public class CalibValidPoint {
	// location of the edge
	Point2D_F64 p = new Point2D_F64();
	// unit vector pointing towards the darkness
	Vector2D_F64 darkVector = new Vector2D_F64();

	public CalibValidPoint( double x , double y , double vx , double vy ) {
		p.set(x,y);
		darkVector.set(vx,vy);
	}

	public CalibValidPoint() {
	}

	@Override
	public String toString() {
		return "CalibValidPoint{" +
				"p=" + p +
				", darkVector=" + darkVector +
				'}';
	}
}
