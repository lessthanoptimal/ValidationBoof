package boofcv.metrics.point;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.combined.CombinedTrack;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class WrapFusedTracker
		<I extends ImageGray<I>, D extends ImageGray<D>, TD extends TupleDesc>
	implements EvaluationTracker<I>
{

	CombinedTrackerScalePoint<I,D,TD> tracker;

	PyramidDiscrete<I> pyramid;
	D[] derivX;
	D[] derivY;
	Class<D> derivType;

	ImageGradient<I,D> gradient;

	boolean isFirstImage = true;

	boolean modeKlt;

	int previousSpawn;

	public WrapFusedTracker(CombinedTrackerScalePoint<I, D,TD> tracker, boolean modeKlt,
							Class<I> imageType ) {
		this.tracker = tracker;
		this.modeKlt = modeKlt;

		derivType = GImageDerivativeOps.getDerivativeType(imageType);

		gradient = FactoryDerivative.sobel(imageType, derivType);

		int pyramidScaling[] = tracker.getTrackerKlt().pyramidScaling;

		pyramid = FactoryPyramid.discreteGaussian(pyramidScaling,-1,2,true,
				ImageType.single(imageType));
	}

	@Override
	public void track(I image) {

		// update the image pyramid
		pyramid.process(image);
		if( derivX == null ) {
			derivX = PyramidOps.declareOutput(pyramid,derivType);
			derivY = PyramidOps.declareOutput(pyramid,derivType);
		}
		PyramidOps.gradient(pyramid, gradient, derivX, derivY);

		// pass in filtered inputs
		tracker.updateTracks(image, pyramid, derivX, derivY);

		if( isFirstImage ) {
			tracker.associateAllToDetected();
			tracker.spawnTracksFromDetected();
			List<CombinedTrack<TD>> tracks = tracker.getPureKlt();

			// save locations of the track in the first image
			for( CombinedTrack<TD> t : tracks ) {
				Point2D_F64 firstLoc = new Point2D_F64(t.x,t.y);
				t.setCookie(firstLoc);
			}

			previousSpawn = tracker.getSpawned().size();
			isFirstImage = false;
		} else if( !modeKlt ) {
			int numActive = tracker.getPureKlt().size() + tracker.getReactivated().size();
//			System.out.printf("  prev %d   curr %d\n",previousSpawn,numActive);
			if( previousSpawn-numActive > 0.10*numActive ) {
				tracker.associateAllToDetected();
				previousSpawn = tracker.getPureKlt().size() + tracker.getReactivated().size();
			}
		}
	}

	@Override
	public void reset() {
		isFirstImage = true;
		tracker.reset();
	}

	@Override
	public List<Point2D_F64> getInitial() {
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		List<CombinedTrack<TD>> tracks = new ArrayList<CombinedTrack<TD>>();

		tracks.addAll(tracker.getPureKlt());
		tracks.addAll(tracker.getReactivated());

		for( CombinedTrack<TD> t : tracks ) {
			ret.add((Point2D_F64)t.getCookie());
		}

		return ret;
	}

	@Override
	public List<Point2D_F64> getCurrent() {
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		ret.addAll(tracker.getPureKlt());
		ret.addAll(tracker.getReactivated());

		return ret;
	}
}
