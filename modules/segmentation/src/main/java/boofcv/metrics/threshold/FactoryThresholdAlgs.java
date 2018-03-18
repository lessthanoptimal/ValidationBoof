package boofcv.metrics.threshold;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class FactoryThresholdAlgs {

	public static ThresholdText globalMean() {
		return (input, output) -> {
            float mean = (float)ImageStatistics.mean(input);
            ThresholdImageOps.threshold(input,output,mean,true);
        };
	}

	public static ThresholdText globalOtsu() {
		return (input, output) -> {
            double threshold = GThresholdImageOps.computeOtsu(input,0,255);
            ThresholdImageOps.threshold(input,output,(int)threshold,true);
        };
	}

	public static ThresholdText globalEntropy() {
		return (input, output) -> {
            double threshold = GThresholdImageOps.computeEntropy(input, 0, 255);
            ThresholdImageOps.threshold(input,output,(int)threshold,true);
        };
	}

	public static ThresholdText localMean() {
		return new ThresholdText() {
			@Override
			public void process(GrayF32 input, GrayU8 output) {
				GThresholdImageOps.localMean(input, output, ConfigLength.fixed(61), 1.0, true, null, null);
			}
		};
	}

	public static ThresholdText localGaussian() {
		return new ThresholdText() {
			@Override
			public void process(GrayF32 input, GrayU8 output) {
				GThresholdImageOps.localGaussian(input, output, ConfigLength.fixed(71), 1.0, true, null, null);
			}
		};
	}

	public static ThresholdText localSauvola() {
		return new ThresholdText() {
			@Override
			public void process(GrayF32 input, GrayU8 output) {
				GThresholdImageOps.localSauvola(input, output, ConfigLength.fixed(31), 0.30f, true);//15 0.30 0.8895
			}
		};
	}

	public static ThresholdText localOtsu() {
		return new ThresholdText() {
			@Override
			public void process(GrayF32 input, GrayU8 output) {
				GThresholdImageOps.localOtsu(input, output, true,ConfigLength.fixed(61),15,1.0,true);
			}
		};
	}

	public static ThresholdText localBlockMinMax() {
		return (input, output) -> GThresholdImageOps.blockMinMax(input, output, ConfigLength.fixed(31), 1.0, true,15);
	}

	public static ThresholdText localBlockOtsu() {
		return (input, output) -> GThresholdImageOps.blockOtsu(input, output, true,ConfigLength.fixed(31),15,1.0,true);
	}

	public static ThresholdText localBlockMean() {
		return (input, output) -> GThresholdImageOps.blockMean(input, output, ConfigLength.fixed(31), 0.95, true);
	}

}
