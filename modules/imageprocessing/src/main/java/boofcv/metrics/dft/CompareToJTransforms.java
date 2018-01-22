package boofcv.metrics.dft;

import boofcv.alg.transform.fft.DiscreteFourierTransformOps;
import boofcv.alg.transform.fft.GeneralPurposeFFT_F32_2D;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.InterleavedF32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class CompareToJTransforms {

	public static void main( String args[] ) {
		Random rand = new Random(234);

		GrayF32 image = new GrayF32(768,323);
		for( int i = 0; i < image.data.length; i++ ) {
			image.data[i] = rand.nextInt(255);
		}

		InterleavedF32 imageR = new InterleavedF32(image.width,image.height,2);
		DiscreteFourierTransformOps.realToComplex(image,imageR);

		FloatFFT_2D fft = new FloatFFT_2D(image.height,image.width);

		fft.realForwardFull(imageR.data);

		System.out.println("Done JTransforms");

		InterleavedF32 imageY = new InterleavedF32(image.width,image.height,2);
		GeneralPurposeFFT_F32_2D mine = new GeneralPurposeFFT_F32_2D(image.height,image.width);
		DiscreteFourierTransformOps.realToComplex(image,imageY);
		mine.realForwardFull(imageY.data);

		for( int i = 0; i < imageR.data.length; i++ ) {
			double error = Math.abs(imageR.data[i]-imageY.data[i]);
			if( error != 0 )
				throw new RuntimeException("Crap");
		}

		fft.complexInverse(imageR.data, true);
		mine.complexInverse(imageY.data, true);

		for( int i = 0; i < imageR.data.length; i++ ) {
			double error = Math.abs(imageR.data[i]-imageY.data[i]);
			if( error != 0 )
				throw new RuntimeException("Crap");
		}

		System.out.println("DONE!");
		System.exit(0);
	}
}
