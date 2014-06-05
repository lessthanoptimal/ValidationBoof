package validate.trackrect;

import boofcv.abst.tracker.*;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FactoryEvaluationTrackerObjectQuad {

	/**
	 * List of trackers to test for regression tests
	 */
	public static List<Info> createRegression(ImageDataType dataType) {
		List<Info> all = new ArrayList<Info>();

		try { all.add(tld(dataType)); } catch(  RuntimeException ignore ){}
		try { all.add(circulant(dataType)); } catch(  RuntimeException ignore ){}
		try { all.add(meanShiftComaniciuNoScale(dataType)); } catch(  RuntimeException ignore ){}
		try { all.add(meanShiftLikelihoodHist(dataType)); } catch(  RuntimeException ignore ){}
		try { all.add(sparseFlow(dataType)); } catch(  RuntimeException ignore ){}

		return all;
	}

	public static Info tld( ImageDataType dataType ) {
		Info info = new Info();
		info.name = "BoofCV-TLD";
		info.imageType = new ImageType(ImageType.Family.SINGLE_BAND,dataType,1);
		info.tracker = FactoryTrackerObjectQuad.tld(new ConfigTld(true),ImageDataType.typeToSingleClass(dataType));
		return info;
	}

	public static Info circulant( ImageDataType dataType ) {
		Info info = new Info();
		info.name = "BoofCV-Circulant";
		info.imageType = new ImageType(ImageType.Family.SINGLE_BAND,dataType,1);
		info.tracker = FactoryTrackerObjectQuad.circulant(new ConfigCirculantTracker(), ImageDataType.typeToSingleClass(dataType));
		return info;
	}

	public static Info meanShiftComaniciuNoScale( ImageDataType dataType ) {
		Info info = new Info();
		info.name = "BoofCV-Comaniciu";
		info.imageType = new ImageType(ImageType.Family.MULTI_SPECTRAL,dataType,3);
		info.tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), info.imageType);
		return info;
	}

	public static Info meanShiftComaniciuScale( ImageDataType dataType ) {
		Info info = new Info();
		info.name = "BoofCV-ComaniciuScale";
		info.imageType = new ImageType(ImageType.Family.MULTI_SPECTRAL,dataType,3);
		info.tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true), info.imageType);
		return info;
	}

	public static Info meanShiftLikelihoodHist( ImageDataType dataType ) {
		Info info = new Info();
		info.name = "BoofCV-MeanShiftHist";
		info.imageType = new ImageType(ImageType.Family.MULTI_SPECTRAL,dataType,3);

		int maxIterations = 30;
		int numBins = 5;
		double maxPixelValue = 256;
		MeanShiftLikelihoodType modelType = MeanShiftLikelihoodType.HISTOGRAM;

		info.tracker = FactoryTrackerObjectQuad.meanShiftLikelihood(maxIterations,
				numBins,maxPixelValue, modelType, info.imageType);
		return info;
	}

	public static Info sparseFlow( ImageDataType dataType ) {
		Info info = new Info();
		info.name = "BoofCV-SparseFlow";
		info.imageType = new ImageType(ImageType.Family.SINGLE_BAND,dataType,1);

		info.tracker = FactoryTrackerObjectQuad.sparseFlow(null, ImageDataType.typeToSingleClass(dataType),null);
		return info;
	}


	public static class Info {
		public String name;
		public ImageType imageType;
		public TrackerObjectQuad tracker;
	}
}
