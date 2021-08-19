package boofcv.parsing;

import org.ddogleg.struct.DogArray;

/**
 * All observed markers in an image with processing time.
 *
 * @author Peter Abeles
 */
public class ObservedLandmarkMarkers {
    /** Processing time in milliseconds */
    public double milliseconds;

    /** Which markers it observed */
    public final DogArray<UniqueMarkerObserved> markers = new DogArray<>(UniqueMarkerObserved::new);
}
