package validate.features.dense;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.misc.GPixelMath;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvalauteDescribeImageDense<T extends ImageBase>
{
	DescribeImageDense<T,TupleDesc_F64> alg;
	ImageType<T> imageType;

	List<String> images;

	T workImage;

	public EvalauteDescribeImageDense( List<String> images , ImageType<T> imageType ) {
		this.images = images;
		this.imageType = imageType;

		workImage = imageType.createImage(1,1);
	}

	public void evaluate( DescribeImageDense<T, TupleDesc_F64> alg ) {

		for (int i = 0; i < images.size(); i++) {
			T input = UtilImageIO.loadImage(new File(images.get(i)),true,imageType);
			workImage.reshape(input.width,input.height);

		}

	}

	private void invarianceLightScaling( T input ) {

		alg.process(input);
		List<TupleDesc_F64> original = copy(alg.getDescriptions());

		for (int i = -3; i <= 4; i++) {
			double scale = Math.pow(5.0,i/10.0);

			GPixelMath.multiply(input,scale,0,255,workImage);

			List<TupleDesc_F64> descriptions = alg.getDescriptions();

			double error = 0;
			for (int j = 0; j < descriptions.size(); j++) {
				error += DescriptorDistance.euclidean(original.get(j),descriptions.get(j));
			}
			error /= descriptions.size();

			// TODO save
		}





	}


	private List<TupleDesc_F64> copy( List<TupleDesc_F64> input ) {
		List<TupleDesc_F64> output = new ArrayList<TupleDesc_F64>();

		for( TupleDesc_F64 d : input ) {
			output.add( d.copy() );
		}

		return output;
	}


}
