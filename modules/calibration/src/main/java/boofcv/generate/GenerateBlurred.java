package boofcv.generate;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public class GenerateBlurred {
	public static void main(String[] args) {
		String[] names = new String[]{"perfect.png","fisheye.jpg"};
		double[] sigmas = new double[]{0.5,1.0,2.0,4.0,12.0,16.0};

		int count = 0;
		for( String name : names ) {
			GrayF32 input = UtilImageIO.loadImage(name,GrayF32.class);
			GrayF32 blurred = input.createSameShape();
			GrayF32 workspace = input.createSameShape();

			for( double sigma : sigmas ) {
				GBlurImageOps.gaussian(input,blurred,sigma,-1,workspace);
				UtilImageIO.saveImage(blurred,String.format("image%02d.png",count++));
			}
		}

	}
}
