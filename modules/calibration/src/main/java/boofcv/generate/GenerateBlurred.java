package boofcv.generate;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import org.apache.commons.io.FilenameUtils;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class GenerateBlurred {
	public static void main(String[] args) {
		Random rand = new Random(123);
		String[] names = new String[]{"perfect.png","fisheye.png"};
		double[] sigmas = new double[]{0.5,1.0,2.0,3.0,4.0,5.0,6.0,8.0,10.0,12.0,14.0,16.0};

		for( String name : names ) {
			GrayF32 input = UtilImageIO.loadImage(name,GrayF32.class);
			GrayF32 noise = input.createSameShape();
			GrayF32 blurred = input.createSameShape();
			GrayF32 workspace = input.createSameShape();

			String basename = FilenameUtils.getBaseName(name);

			int count = 0;
			for( double sigma : sigmas ) {
				noise.setTo(input);
				GImageMiscOps.addGaussian(noise,rand,5,0,255);
				GBlurImageOps.gaussian(noise,blurred,sigma,-1,workspace);
				UtilImageIO.saveImage(blurred,String.format("%s%02d.png",basename,count++));
			}
		}
	}
}
