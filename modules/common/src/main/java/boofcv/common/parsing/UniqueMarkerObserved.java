package boofcv.common.parsing;

import boofcv.struct.geo.PointIndex2D_F64;
import org.ddogleg.struct.DogArray;

/**
 * Observed pixel coordinates of landmarks on a marker which has a unique ID and supports multiple IDs
 *
 * @author Peter Abeles
 */
public class UniqueMarkerObserved {
    /** Marker ID */
    public int markerID;

    /** Location on landmarks on the observed marker */
    public final DogArray<PointIndex2D_F64> landmarks = new DogArray<>(PointIndex2D_F64::new);

    public PointIndex2D_F64 findLandmark( int id ) {
        for (int i = 0; i < landmarks.size; i++) {
            if (landmarks.get(i).index == id)
                return landmarks.get(i);
        }
        return null;
    }

    public void setTo( UniqueMarkerObserved src ) {
        this.markerID = src.markerID;
        this.landmarks.resetResize(src.landmarks.size);
        for (int i = 0; i < landmarks.size; i++) {
            landmarks.get(i).setTo(src.landmarks.get(i));
        }
    }
}
