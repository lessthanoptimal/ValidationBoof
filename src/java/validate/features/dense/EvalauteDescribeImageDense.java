package validate.features.dense;

import boofcv.abst.distort.FDistort;
import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.GrowQueue_F64;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// TODO image location shift
// TODO image location scale

/**
 * @author Peter Abeles
 */
public class EvalauteDescribeImageDense<T extends ImageBase>
{
	Random rand = new Random(234);

	DescribeImageDense<T,TupleDesc_F64> alg;
	ImageType<T> imageType;

	List<String> images;

	T workImage;

	PrintStream out = System.out;

	List<TupleDesc_F64> original;
	GrowQueue_F64 magnitude = new GrowQueue_F64();

	public EvalauteDescribeImageDense( List<String> images , ImageType<T> imageType ) {
		this.images = images;
		this.imageType = imageType;

		workImage = imageType.createImage(1,1);
	}

	public void evaluate( DescribeImageDense<T, TupleDesc_F64> alg ) {
		this.alg = alg;

		for (int i = 0; i < images.size(); i++) {
			T input = UtilImageIO.loadImage(new File(images.get(i)),true,imageType);
			workImage.reshape(input.width,input.height);

			alg.process(input);
			original = copy(alg.getDescriptions());

			// precompute the norm for original descriptors.  Used later to normalize the scale of a descriptor
			magnitude.reset();
			for (int j = 0; j < original.size(); j++) {
				TupleDesc_F64 t = original.get(j);
				// Compute and add to the list the Euclidean norm for the descriptor vector
				double total = 0;
				for (int k = 0; k < t.size(); k++) {
					total += t.value[k]*t.value[k];
				}
				magnitude.add(Math.sqrt(total));
			}

			out.println("======================================================================");
			out.println(images.get(i));
			out.println();
			out.println("Total Descriptions = "+original.size());
			out.println();
			printMetric(invarianceLightScaling(input),"Light Scaling");
			out.println();
			printMetric(invarianceLightOffset(input),"Light Offset");
			out.println();
			printMetric(additiveGaussianNoise(input),"Additive Gaussian");
			out.println();
			printMetric(imageTranslation(input),"Image Translation");
			out.println();
			printMetric(imageScale(input),"Image Scale");
			out.println();
		}

	}

	public void setOutputStream(PrintStream out) {
		this.out = out;
	}

	private void printMetric(Metric m , String metricName ) {
		out.println(metricName);
		out.printf("%8s ","metric");
		for (int i = 0; i < m.parameter.size; i++) {
			out.printf("%8.3f ",m.parameter.get(i));
		}
		out.println();
		out.printf("%8s ","error");
		for (int i = 0; i < m.error.size; i++) {
			out.printf("%8.5f ",m.error.get(i));
		}
	}

	private Metric invarianceLightScaling( T input ) {

		Metric metric = new Metric();

		for (int i = -3; i <= 4; i++) {
			double scale = Math.pow(5.0,i/10.0);

			GPixelMath.multiply(input,scale,0,255,workImage);
			alg.process(workImage);

			List<TupleDesc_F64> descriptions = alg.getDescriptions();

			double error = computeError( descriptions);

			metric.error.add(error);
			metric.parameter.add(scale);
		}

		return metric;
	}

	private Metric invarianceLightOffset( T input ) {

		Metric metric = new Metric();

		for (int i = 0; i <= 100; i += 10 ) {
			GPixelMath.plus(input,i,0,255,workImage);
			alg.process(workImage);

			List<TupleDesc_F64> descriptions = alg.getDescriptions();

			double error = computeError( descriptions);

			metric.error.add(error);
			metric.parameter.add(i);
		}

		return metric;
	}

	private Metric additiveGaussianNoise(T input ) {

		Metric metric = new Metric();

		for (int i = -3; i <= 6; i++) {
			double sigma = Math.pow(10.0,i/5.0);

			workImage.setTo(input);
			GImageMiscOps.addGaussian(workImage,rand,sigma,0,255);

			alg.process(workImage);
			List<TupleDesc_F64> descriptions = alg.getDescriptions();

			double error = computeError( descriptions);

			metric.error.add(error);
			metric.parameter.add(sigma);
		}

		return metric;
	}

	private Metric imageTranslation( T input ) {

		Metric metric = new Metric();

		for (int i = -4; i <= 6; i++) {
			double d = Math.pow(10.0, i / 5.0);

			double tx,ty;
			double total = 0;
			for (int trial = 0; trial < 8; trial++) {
				switch( trial ) {
					case 0: tx =  d;ty =  0; break;
					case 1: tx =  d;ty =  d; break;
					case 2: tx =  0;ty =  d; break;
					case 3: tx = -d;ty =  d; break;
					case 4: tx = -d;ty =  0; break;
					case 5: tx = -d;ty = -d; break;
					case 6: tx =  0;ty = -d; break;
					case 7: tx =  d;ty = -d; break;
					default:
					throw new RuntimeException("Bad");
				}

				new FDistort(input,workImage).affine(1,0,0,1,tx,ty).border(BorderType.EXTENDED).apply();

				alg.process(workImage);
				List<TupleDesc_F64> descriptions = alg.getDescriptions();

				double error = computeError( descriptions);

				total += error;
			}
			total /= 8;

			metric.error.add(total);
			metric.parameter.add(d);
		}
		return metric;
	}

	private Metric imageScale( T input ) {

		Metric metric = new Metric();

		for (int i = -4; i <= 8; i++) {
			double scale = Math.pow(3.0, i / 40.0);

			// keep it centered
			double offX = input.width*(1.0-scale)/2.0;
			double offY = input.height*(1.0-scale)/2.0;

			new FDistort(input,workImage).affine(scale,0,0,scale,offX,offY).border(BorderType.EXTENDED).apply();

			alg.process(workImage);
			List<TupleDesc_F64> descriptions = alg.getDescriptions();

			double error = computeError( descriptions);

			metric.error.add(error);
			metric.parameter.add(scale);
		}
		return metric;
	}

	/**
	 * Computes the average fractional error across all descriptions in the image.
	 */
	private double computeError( List<TupleDesc_F64> descriptions) {
		double totalError = 0;
		int count = 0;
		for (int j = 0; j < descriptions.size(); j++) {
			double e = DescriptorDistance.euclidean(original.get(j),descriptions.get(j));
			double m = magnitude.get(j);
			if( m != 0 ) {
				totalError += e / m;
				count++;
			}
		}
		totalError /= count;

		return totalError;
	}

	private List<TupleDesc_F64> copy( List<TupleDesc_F64> input ) {
		List<TupleDesc_F64> output = new ArrayList<TupleDesc_F64>();

		for( TupleDesc_F64 d : input ) {
			output.add( d.copy() );
		}

		return output;
	}


	private static class Metric {
		GrowQueue_F64 parameter = new GrowQueue_F64();
		GrowQueue_F64 error = new GrowQueue_F64();
	}

	public static void main(String[] args) {
		List<String> images = new ArrayList<String>();
		images.add("data/fiducials/image/standard/distance_angle/image00001.png");
		images.add("data/calib/mono/chessboard/distant/image00000.jpg");

		ImageType<GrayU8> imageType = ImageType.single(GrayU8.class);

		EvalauteDescribeImageDense<GrayU8> evaluator =
				new EvalauteDescribeImageDense<GrayU8>(images,imageType );

		DescribeImageDense<GrayU8,TupleDesc_F64> alg = FactoryDescribeImageDense.hog(new ConfigDenseHoG(),imageType);
		evaluator.evaluate(alg);
	}

}
