package boofcv.metrics.point;

import boofcv.struct.image.ImageGray;

/**
 * Creates algorithms for evaluation
 *
 * @author Peter Abeles
 */
public class FactoryEvaluationTrackers<T extends ImageGray<T>> {

	Class<T> imageType;

	public FactoryEvaluationTrackers(Class<T> imageType) {
		this.imageType = imageType;
	}

	public EvaluationTracker<T> create( EvaluatedAlgorithm type ){
		throw new RuntimeException("Update using new Config description of each tracker");
//		switch ( type ) {
//			case FH_SURF:
//				return createFhSurf(false);
//
//			case FAST_BRIEF:
//				return createBrief(false,true);
//
//			case FH_BRIEF:
//				return createBrief(false,false);
//
//			case KLT:
//				return createKlt();
//
//			case FH_SURF_KLT:
//				return createFhSurfKlt();
//
////			case BRIEF_KLT:
////				return createBriefKlt(true);
//
//			case FH_BRIEF_KLT:
//				return createFhBriefKlt(false);
//
//			default:
//				throw new RuntimeException("Unknown");
//		}
	}
}
