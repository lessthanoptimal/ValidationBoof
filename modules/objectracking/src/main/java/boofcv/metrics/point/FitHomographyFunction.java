package boofcv.metrics.point;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.ImageGray;
import org.ddogleg.optimization.functions.FunctionNtoS;

/**
 * Computes the average difference in intensity values inside of overlapping regions between two
 * images after a homography transform has been applied.
 *
 * @author Peter Abeles
 */
public class FitHomographyFunction<T extends ImageGray<T>> implements FunctionNtoS {


	GImageGray src;
	InterpolatePixelS<T> dst;

	public FitHomographyFunction(InterpolatePixelS<T> dst) {
		this.dst = dst;
	}

	public void setInputs( T imageSrc , T imageDst ) {
		this.src = FactoryGImageGray.wrap(imageSrc);
		dst.setImage(imageDst);
	}

	@Override
	public int getNumOfInputsN() {
		return 9;
	}

	@Override
	public double process(double[] input) {
		float H0 = (float)input[0];
		float H1 = (float)input[1];
		float H2 = (float)input[2];
		float H3 = (float)input[3];
		float H4 = (float)input[4];
		float H5 = (float)input[5];
		float H6 = (float)input[6];
		float H7 = (float)input[7];
		float H8 = (float)input[8];

		double total = 0;
		int N = 0;

		for( int y = 0; y < src.getHeight(); y++ ) {
			for( int x = 0; x < src.getWidth(); x++ ) {
				float xx = H0*x + H1*y + H2;
				float yy = H3*x + H4*y + H5;
				float zz = H6*x + H7*y + H8;

				float distX = xx/zz;
				float distY = yy/zz;

				if( dst.isInFastBounds(distX, distY)  ) {
					float residual = dst.get_fast(distX,distY) - src.get(x,y).floatValue();
//					System.out.println(distX+" "+distY+"  error "+residual);
					total += residual*residual;
					N++;
				}
			}
		}

		return total/N;
	}
}
