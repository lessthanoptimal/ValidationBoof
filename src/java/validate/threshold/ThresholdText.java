package validate.threshold;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public interface ThresholdText {
	public void process( ImageFloat32 input , ImageUInt8 output );
}
