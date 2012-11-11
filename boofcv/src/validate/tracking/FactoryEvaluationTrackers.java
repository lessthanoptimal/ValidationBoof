package validate.tracking;

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.detect.interest.WrapFHtoInterestPoint;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.fused.CombinedPointTracker;
import boofcv.alg.tracker.fused.DetectTrackKLT;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageSingleBand;

/**
 * Creates algorithms for evaluation
 *
 * @author Peter Abeles
 */
public class FactoryEvaluationTrackers<T extends ImageSingleBand> {

	Class<T> imageType;

	public FactoryEvaluationTrackers(Class<T> imageType) {
		this.imageType = imageType;
	}

	public EvaluationTracker<T> create( EvaluatedAlgorithm type ){
		switch ( type ) {
			case FH_SURF:
				return createFhSurf(false);

			case FAST_BRIEF:
				return createBrief(false,true);

			case FH_BRIEF:
				return createBrief(false,false);

			case KLT:
				return createKlt();

			case FH_SURF_KLT:
				return createFhSurfKlt();

//			case BRIEF_KLT:
//				return createBriefKlt(true);

			case FH_BRIEF_KLT:
				return createFhBriefKlt(false);

			default:
				throw new RuntimeException("Unknown");
		}
	}

	public EvaluationTracker<T> createFhSurf(boolean copyDescription) {
		InterestPointDetector<T> detector = createDetector(false);
		DescribeRegionPoint<T,SurfFeature> describe = FactoryDescribeRegionPoint.surfm(true, imageType);

		ScoreAssociation<SurfFeature> scorer = FactoryAssociation.scoreEuclidean(describe.getDescriptorType(),true);
		GeneralAssociation<SurfFeature> associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, 0, true);

		return new WrapGenericDetectTracker<T,SurfFeature>(detector,describe,associate,copyDescription);
	}

	public EvaluationTracker<T> createBrief( boolean copyDescription , boolean useFast ) {
		InterestPointDetector<T> detector;
		if( useFast ) {
			Class derivType = GImageDerivativeOps.getDerivativeType(imageType);
			GeneralFeatureDetector det = FactoryDetectPoint.createFast(10, 6, 600, imageType);
			detector = FactoryInterestPoint.wrapPoint(det,imageType,derivType);
		} else {
			detector = createDetector(true);
		}

		DescribeRegionPoint<T,TupleDesc_B> describe = FactoryDescribeRegionPoint.brief(16,512,-1,4,useFast,imageType);
		ScoreAssociation<TupleDesc_B> scorer = FactoryAssociation.scoreHamming(describe.getDescriptorType());
		GeneralAssociation<TupleDesc_B> associate = FactoryAssociation.greedy(scorer,Double.MAX_VALUE,0,true);

		return new WrapGenericDetectTracker<T,TupleDesc_B>(detector,describe,associate,copyDescription);
	}

	public EvaluationTracker<T> createFhSurfKlt() {
		InterestPointDetector<T> detector = createDetector(false);

		DescribeRegionPoint<T,SurfFeature> describe = FactoryDescribeRegionPoint.surfm(true,imageType);
		ScoreAssociation<SurfFeature> scorer = FactoryAssociation.scoreEuclidean(describe.getDescriptorType(),true);
		GeneralAssociation<SurfFeature> associate = FactoryAssociation.greedy(scorer,Double.MAX_VALUE,0,true);

		DetectTrackKLT<T, T> klt = defaultFusedKlt();

		CombinedPointTracker<T, T,SurfFeature> tracker = new CombinedPointTracker<T, T,SurfFeature>(klt,detector,describe,associate,
				2,imageType,imageType);

		return new WrapFusedTracker<T,T,SurfFeature>(tracker,false);
	}
//
	public EvaluationTracker<T> createFhBriefKlt(boolean isFixed) {
		InterestPointDetector<T> detector = createDetector(!isFixed);

		DescribeRegionPoint<T,TupleDesc_B> describe = FactoryDescribeRegionPoint.brief(16,512,-1,4,isFixed,imageType);
		ScoreAssociation<TupleDesc_B> scorer = FactoryAssociation.scoreHamming(describe.getDescriptorType());
		GeneralAssociation<TupleDesc_B> associate = FactoryAssociation.greedy(scorer,Double.MAX_VALUE,0,true);

		DetectTrackKLT<T, T> klt = defaultFusedKlt();

		CombinedPointTracker<T, T,TupleDesc_B> tracker = new CombinedPointTracker<T, T,TupleDesc_B>(klt,detector,describe,associate,
				2,imageType,imageType);

		return new WrapFusedTracker<T,T,TupleDesc_B>(tracker,false);
	}

	public EvaluationTracker<T> createKlt() {
		InterestPointDetector<T> detector = createDetector(false);

		DetectTrackKLT<T, T> klt = defaultFusedKlt();

		CombinedPointTracker<T, T,TupleDesc> tracker = new CombinedPointTracker<T, T,TupleDesc>(klt,detector,null,null,
				2,imageType,imageType);

		return new WrapFusedTracker<T,T,TupleDesc>(tracker,true);
	}

	private InterestPointDetector<T> createDetector( boolean estimateOrientation) {
		float detectThreshold = 1;
		int extractRadius = 2;
		int maxFeaturesPerScale = 200;
		int initialSampleSize = 1;
		int initialSize = 9;
		int numberScalesPerOctave = 4;
		int numberOfOctaves = 4;

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(extractRadius, detectThreshold, 5, true);
		FastHessianFeatureDetector<T> feature = new FastHessianFeatureDetector<T>(extractor,maxFeaturesPerScale,
				initialSampleSize, initialSize,numberScalesPerOctave,numberOfOctaves);
		Class integralType = GIntegralImageOps.getIntegralType(imageType);

		OrientationIntegral angleII = estimateOrientation ?
				FactoryOrientationAlgs.sliding_ii(0.65, Math.PI/3.0,8,-1, 6, integralType) : null;

		return new WrapFHtoInterestPoint(feature,angleII);
	}

	private DetectTrackKLT<T, T> defaultFusedKlt() {
		KltConfig kltConfig = KltConfig.createDefault();
		int scales[] = new int[]{1,2,4,8};
		int featureRadius = 5;
		kltConfig.maxPerPixelError = 10;

		return new DetectTrackKLT<T, T>(kltConfig,
				featureRadius,scales,imageType,imageType);
	}
}
