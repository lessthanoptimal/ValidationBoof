package boofcv.metrics.point;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.ImageGray;
import org.ddogleg.optimization.functions.FunctionNtoN;

/**
 * Computes the gradient of {@link FitHomographyFunction}.
 *
 * @author Peter Abeles
 */
public class FitHomographyGradient <I extends ImageGray<I>, D extends ImageGray<D>>
		implements FunctionNtoN {

	GImageGray src;
	GImageGray srcDerivX;
	GImageGray srcDerivY;

	InterpolatePixelS<I> dst;

	public FitHomographyGradient(InterpolatePixelS<I> interpolation ) {
		this.dst = interpolation;
	}

	public void setInputs( I imageSrc , I imageDst ,
						   D srcDerivX , D srcDerivY ) {
		this.src = FactoryGImageGray.wrap(imageSrc);
		dst.setImage(imageDst);
		this.srcDerivX = FactoryGImageGray.wrap(srcDerivX);
		this.srcDerivY = FactoryGImageGray.wrap(srcDerivY);
	}

	@Override
	public int getN() {
		return 9;
	}

	@Override
	public void process(double[] input, double[] output) {
		for( int i = 0; i < 9; i++ )
			output[i] = 0;

		float H0 = (float)input[0];
		float H1 = (float)input[1];
		float H2 = (float)input[2];
		float H3 = (float)input[3];
		float H4 = (float)input[4];
		float H5 = (float)input[5];
		float H6 = (float)input[6];
		float H7 = (float)input[7];
		float H8 = (float)input[8];

		int N = 0;

		for( int y = 0; y < src.getHeight(); y++ ) {
			for( int x = 0; x < src.getWidth(); x++ ) {

				float xx = H0*x + H1*y + H2;
				float yy = H3*x + H4*y + H5;
				float zz = H6*x + H7*y + H8;

				float distX = xx/zz;
				float distY = yy/zz;

				if( dst.isInFastBounds(distX, distY)) {

					float residual = dst.get_fast(distX,distY) - src.get(x,y).floatValue();

					float dx = srcDerivX.get(x,y).floatValue();
					float dy = srcDerivY.get(x,y).floatValue();

					output[0] += residual*dx*(x/zz);
					output[1] += residual*dx*(y/zz);
					output[2] += residual*dx/zz;
					output[3] += residual*dy*(x/zz);
					output[4] += residual*dy*(y/zz);
					output[5] += residual*dy/zz;
					output[6] += -residual*x*(dx*xx + dy*yy)/(zz*zz);
					output[7] += -residual*y*(dx*xx + dy*yy)/(zz*zz);
					output[8] += -residual*(dx*xx + dy*yy)/(zz*zz);

					N++;
				}
			}
		}

		for( int i = 0; i < 9; i++ )
			output[i] *= 2.0/N;
	}
}
