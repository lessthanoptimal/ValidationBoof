package validate.tracking;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapPointTracker<I extends ImageSingleBand>
	implements EvaluationTracker<I>
{
	PointTracker<I> tracker;

	boolean first = true;

	@Override
	public void track(I image) {
		tracker.process(image);

		if( first ) {
			tracker.spawnTracks();

			List<PointTrack> tracks = tracker.getNewTracks(null);

			for( PointTrack t : tracks ) {
				Point2D_F64 initial = new Point2D_F64(t.x,t.y);
				t.setCookie(initial);
			}
		}
	}

	@Override
	public void reset() {
		tracker.reset();
	}

	@Override
	public List<Point2D_F64> getInitial() {
		List<Point2D_F64> initial = new ArrayList<Point2D_F64>();

		List<PointTrack> tracks = tracker.getActiveTracks(null);

		for( PointTrack t : tracks ) {
			initial.add((Point2D_F64) t.getCookie());
		}

		return initial;
	}

	@Override
	public List<Point2D_F64> getCurrent() {
		List<Point2D_F64> current = new ArrayList<Point2D_F64>();

		List<PointTrack> tracks = tracker.getActiveTracks(null);

		for( PointTrack t : tracks ) {
			current.add(t);
		}

		return current;
	}
}
