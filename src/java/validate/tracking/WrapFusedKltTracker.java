package validate.tracking;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.combined.PyramidKltForCombined;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapFusedKltTracker <I extends ImageGray, D extends ImageGray>
		implements EvaluationTracker<I>
{
	InterestPointDetector<I> detector;
	PyramidKltForCombined<I,D> tracker;

	ImageGradient<I,D> gradient;

	PyramidDiscrete<I> pyramid;
	D[] derivX;
	D[] derivY;

	Class<D> derivType;

	List<PyramidKltFeature> tracks = new ArrayList<PyramidKltFeature>();

	boolean first = true;

	public WrapFusedKltTracker(InterestPointDetector<I> detector,
							   PyramidKltForCombined<I, D> tracker,
							   Class<I> imageType ) {
		this.detector = detector;
		this.tracker = tracker;

		derivType = GImageDerivativeOps.getDerivativeType(imageType);

		gradient = FactoryDerivative.sobel(imageType, derivType);

		int pyramidScaling[] = tracker.pyramidScaling;

		pyramid = FactoryPyramid.discreteGaussian(pyramidScaling,-1,2,true,imageType);
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

		tracker.setInputs(pyramid,derivX,derivY);

		if( first ) {
			first = false;

			detector.detect(image);

			for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
				Point2D_F64 p = detector.getLocation(i);

				PyramidKltFeature t = tracker.createNewTrack();

				t.cookie = p.copy();
				tracker.setDescription( (float)p.x,(float)p.y,t);

				tracks.add(t);
			}
		} else {
			for( int i = 0; i < tracks.size(); ) {
				PyramidKltFeature t = tracks.get(i);

				if( !tracker.performTracking(t) ) {
					tracks.remove(i);
				} else {
					i++;
				}
			}
		}
	}

	@Override
	public void reset() {
		tracks.clear();
		first = true;
	}

	@Override
	public List<Point2D_F64> getInitial() {
		List<Point2D_F64> initial = new ArrayList<Point2D_F64>();

		for( PyramidKltFeature t : tracks ) {
			initial.add((Point2D_F64) t.getCookie());
		}

		return initial;
	}

	@Override
	public List<Point2D_F64> getCurrent() {
		List<Point2D_F64> current = new ArrayList<Point2D_F64>();

		for( PyramidKltFeature t : tracks ) {
			current.add( new Point2D_F64(t.x,t.y));
		}

		return current;
	}
}
