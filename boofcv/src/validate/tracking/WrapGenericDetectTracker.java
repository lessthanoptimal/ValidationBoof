package validate.tracking;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class WrapGenericDetectTracker<I extends ImageSingleBand, TD extends TupleDesc>
		implements EvaluationTracker<I> {


	// todo comment more
	DetectDescribePoint<I,TD> detector;

	AssociateDescription<TD> associate;

	List<Info> tracks = new ArrayList<Info>();
	// tracks that where associated in the most recent image
	List<Info> tracksMatched = new ArrayList<Info>();

	FastQueue<Point2D_F64> pointCurr = new FastQueue<Point2D_F64>(100,Point2D_F64.class,false);
	FastQueue<TD> descKey;
	FastQueue<TD> descCurr;

	boolean hasKeyFrame = false;
	boolean copyDescription;

	public WrapGenericDetectTracker(DetectDescribePoint<I,TD> detector,
									AssociateDescription<TD> associate,
									boolean copyDescription ) {
		this.detector = detector;
		this.associate = associate;
		this.copyDescription =copyDescription;

		descKey = new FastQueue<TD>(10,detector.getDescriptionType(),false);
		descCurr = new FastQueue<TD>(10,detector.getDescriptionType(),false);
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
			FastQueue<AssociatedIndex> matches = associate.getMatches();

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
