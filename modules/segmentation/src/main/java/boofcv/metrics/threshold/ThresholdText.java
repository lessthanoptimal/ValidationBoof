package boofcv.metrics.threshold;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public interface ThresholdText {
	void process(GrayF32 input, GrayU8 output);
}
