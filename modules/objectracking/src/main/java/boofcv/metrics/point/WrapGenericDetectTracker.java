package boofcv.metrics.point;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class WrapGenericDetectTracker<I extends ImageGray<I>, TD extends TupleDesc>
		implements EvaluationTracker<I> {


	// todo comment more
	DetectDescribePoint<I,TD> detector;

	AssociateDescription<TD> associate;

	List<Info> tracks = new ArrayList<Info>();
	// tracks that where associated in the most recent image
	List<Info> tracksMatched = new ArrayList<Info>();

	FastArray<Point2D_F64> pointCurr = new FastArray<>(Point2D_F64.class,100);
	FastArray<TD> descKey;
	FastArray<TD> descCurr;

	boolean hasKeyFrame = false;
	boolean copyDescription;

	public WrapGenericDetectTracker(DetectDescribePoint<I,TD> detector,
									AssociateDescription<TD> associate,
									boolean copyDescription ) {
		this.detector = detector;
		this.associate = associate;
		this.copyDescription =copyDescription;

		descKey = new FastArray<>(detector.getDescriptionType());
		descCurr = new FastArray<>(detector.getDescriptionType());
	}

	@Override
	public void track(I image) {
		detector.detect(image);

		pointCurr.reset();
		descKey.reset();
		descCurr.reset();
		tracksMatched.clear();

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			descCurr.add(detector.getDescription(i));
			pointCurr.add(detector.getLocation(i));
		}

		if( hasKeyFrame ) {

			// match up features
			for( int i = 0; i < tracks.size(); i++ ) {
				descKey.add(tracks.get(i).desc);
			}

			associate.setSource(descKey);
			associate.setDestination(descCurr);
			associate.associate();

			// update the active track list
			FastAccess<AssociatedIndex> matches = associate.getMatches();

			for( AssociatedIndex i : matches.toList() ) {
				Info track = tracks.get(i.src);
				track.curr.set(pointCurr.get(i.dst));
				if( copyDescription )
					track.desc.setTo(descCurr.get(i.dst));
				tracksMatched.add(track);
			}

		} else {
			hasKeyFrame = true;

			// set up the features
			for( int i = 0; i < pointCurr.size(); i++ ) {
				Info info = new Info();
				info.key.set(pointCurr.get(i));
				info.curr.set(pointCurr.get(i));
				info.desc = (TD)descCurr.get(i).copy();

				tracks.add(info);
				tracksMatched.add(info);
			}
		}
	}

	@Override
	public void reset() {
		throw new RuntimeException("Implement");
	}

	@Override
	public List<Point2D_F64> getInitial() {
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		for( Info i : tracksMatched ) {
			ret.add( i.key );
		}

		return ret;
	}

	@Override
	public List<Point2D_F64> getCurrent() {
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		for( Info i : tracksMatched ) {
			ret.add( i.curr );
		}

		return ret;
	}

	private class Info
	{
		Point2D_F64 key = new Point2D_F64();
		Point2D_F64 curr = new Point2D_F64();
		TD desc;
	}
}
