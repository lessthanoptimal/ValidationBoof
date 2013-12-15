package validate.tracking;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribeFusion;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.interest.ConfigFast;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.detect.interest.WrapFHtoInterestPoint;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.tracker.combined.PyramidKltForCombined;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.SurfFeature;
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
		InterestPointDetector<T> detector = createDetector();
		DescribeRegionPoint<T,SurfFeature> describe =
				FactoryDescribeRegionPoint.surfStable(null, imageType);

		ScoreAssociation<SurfFeature> scorer = FactoryAssociation.scoreEuclidean(describe.getDescriptionType(),true);
		AssociateDescription<SurfFeature> associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);
		OrientationImage<T> orientation = createOrientation();

		DetectDescribeFusion<T,SurfFeature> fused =
				new DetectDescribeFusion<T, SurfFeature>(detector,orientation,describe);

		return new WrapGenericDetectTracker<T,SurfFeature>(fused,associate,copyDescription);
	}

	public EvaluationTracker<T> createBrief( boolean copyDescription , boolean useFast ) {
		InterestPointDetector<T> detector;
		OrientationImage<T> orientation;
		if( useFast ) {
			Class derivType = GImageDerivativeOps.getDerivativeType(imageType);
			GeneralFeatureDetector det = FactoryDetectPoint.createFast(
					new ConfigFast(6,9), new ConfigGeneralDetector(600,10,6), imageType);
			detector = FactoryInterestPoint.wrapPoint(det,1,imageType,derivType);
			orientation = null;
		} else {
			detector = createDetector();
			orientation = createOrientation();
		}

		DescribeRegionPoint<T,TupleDesc_B> describe = FactoryDescribeRegionPoint.
				brief(new ConfigBrief(16, 512, -1, 4, useFast), imageType);
		ScoreAssociation<TupleDesc_B> scorer = FactoryAssociation.scoreHamming(describe.getDescriptionType());
		AssociateDescription<TupleDesc_B> associate = FactoryAssociation.greedy(scorer,Double.MAX_VALUE,true);

		DetectDescribeFusion<T,TupleDesc_B> fused =
				new DetectDescribeFusion<T, TupleDesc_B>(detector,orientation,describe);

		return new WrapGenericDetectTracker<T,TupleDesc_B>(fused,associate,copyDescription);
	}

	public EvaluationTracker<T> createFhSurfKlt() {
		InterestPointDetector<T> detector = createDetector();

		DescribeRegionPoint<T,SurfFeature> describe =
				FactoryDescribeRegionPoint.surfStable(null, imageType);
		ScoreAssociation<SurfFeature> scorer = FactoryAssociation.scoreEuclidean(describe.getDescriptionType(),true);
		AssociateDescription<SurfFeature> associate = FactoryAssociation.greedy(scorer,Double.MAX_VALUE,true);

		OrientationImage<T> orientation = createOrientation();

		DetectDescribeFusion<T,SurfFeature> fused =
				new DetectDescribeFusion<T, SurfFeature>(detector,orientation,describe);

		PyramidKltForCombined<T, T> klt = defaultFusedKlt();

		CombinedTrackerScalePoint<T, T,SurfFeature> tracker =
				new CombinedTrackerScalePoint<T, T,SurfFeature>(klt, fused,associate);

		return new WrapFusedTracker<T,T,SurfFeature>(tracker,false,imageType);
	}

	private OrientationImage<T> createOrientation() {
		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
		return FactoryOrientation.convertImage(orientationII,imageType);
	}

	//
	public EvaluationTracker<T> createFhBriefKlt( boolean isFixed ) {
		InterestPointDetector<T> detector = createDetector();

		DescribeRegionPoint<T,TupleDesc_B> describe = FactoryDescribeRegionPoint.
				brief(new ConfigBrief(16, 512, -1, 4, isFixed), imageType);
		ScoreAssociation<TupleDesc_B> scorer = FactoryAssociation.scoreHamming(describe.getDescriptionType());
		AssociateDescription<TupleDesc_B> associate = FactoryAssociation.greedy(scorer,Double.MAX_VALUE,true);

		PyramidKltForCombined<T, T> klt = defaultFusedKlt();

		OrientationImage<T> orientation = null;
		if( !isFixed ) {
			orientation = createOrientation();
		}

		DetectDescribeFusion<T,TupleDesc_B> fused =
				new DetectDescribeFusion<T, TupleDesc_B>(detector,orientation,describe);

		CombinedTrackerScalePoint<T, T,TupleDesc_B> tracker = new CombinedTrackerScalePoint<T, T,TupleDesc_B>(klt,fused,associate);

		return new WrapFusedTracker<T,T,TupleDesc_B>(tracker,false,imageType);
	}

	public EvaluationTracker<T> createKlt() {
		InterestPointDetector<T> detector = createDetector();

		PyramidKltForCombined<T, T> klt = defaultFusedKlt();

		return new WrapFusedKltTracker<T,T>(detector,klt,imageType);
	}

	private InterestPointDetector<T> createDetector() {
		float detectThreshold = 1;
		int extractRadius = 2;
		int maxFeaturesPerScale = 200;
		int initialSampleSize = 1;
		int initialSize = 9;
		int numberScalesPerOctave = 4;
		int numberOfOctaves = 4;

		NonMaxSuppression extractor = FactoryFeatureExtractor.
				nonmax(new ConfigExtract(extractRadius, detectThreshold, 5, true));
		FastHessianFeatureDetector<T> feature = new FastHessianFeatureDetector<T>(extractor,maxFeaturesPerScale,
				initialSampleSize, initialSize,numberScalesPerOctave,numberOfOctaves);

		return new WrapFHtoInterestPoint(feature);
	}

	private PyramidKltForCombined<T, T> defaultFusedKlt() {
		KltConfig kltConfig = KltConfig.createDefault();
		int scales[] = new int[]{1,2,4,8};
		int featureRadius = 5;
		kltConfig.maxPerPixelError = 10;

		return new PyramidKltForCombined<T, T>(kltConfig,
				featureRadius,scales,imageType,imageType);
	}
}
