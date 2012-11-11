package validate.tracking;

import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedMinimization;
import boofcv.numerics.optimization.impl.UtilOptimize;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;
import georegression.struct.homo.Homography2D_F64;

/**
 *
 * TODO comment
 *
 * @author Peter Abeles
 */
public class RefineHomographTransform<I extends ImageSingleBand, D extends ImageSingleBand> {

	PyramidUpdaterDiscrete<I> updater;
	PyramidDiscrete<I> src;
	PyramidDiscrete<D> srcDericX;
	PyramidDiscrete<D> srcDericY;

	PyramidDiscrete<I> dst;


	FitHomographyFunction<I> function;
	FitHomographyGradient<I,D> gradient;

	UnconstrainedMinimization minimizer = FactoryOptimization.unconstrained();

	Homography2D_F64 result = new Homography2D_F64();

	public RefineHomographTransform( int scales[], Class<I> imageType , Class<D> derivType ) {
		InterpolatePixel<I> interp = FactoryInterpolation.bilinearPixel(imageType);

		function = new FitHomographyFunction<I>(interp);
		gradient = new FitHomographyGradient<I, D>(interp);

		src = new PyramidDiscrete<I>(imageType,true,scales);
		dst = new PyramidDiscrete<I>(imageType,true,scales);

		srcDericX = new PyramidDiscrete<D>(derivType,false,scales);
		srcDericY = new PyramidDiscrete<D>(derivType,false,scales);

		updater = FactoryPyramid.discreteGaussian(imageType, -1, 2);
	}

	public void setSource( I src ) {

		updater.update(src,this.src);
		srcDericX.initialize(src.width, src.height);
		srcDericY.initialize(src.width,src.height);

		for( int i = 0; i < this.src.getNumLayers(); i++ ) {
			D dx = srcDericX.getLayer(i);
			D dy = srcDericY.getLayer(i);

			GImageDerivativeOps.sobel(this.src.getLayer(i),dx,dy, BorderType.EXTENDED);
		}
	}

	public boolean process( I dst, Homography2D_F64 H_init ) {

		updater.update(dst,this.dst);

		Homography2D_F64 H = H_init.copy();

		for( int layer = src.getNumLayers()-1; layer >= 0 ; layer-- ) {
			// adjust the transform for the layer
			double scale = src.getScale(layer);
			Homography2D_F64 H_scale = new Homography2D_F64(scale,0,0,0,scale,0,0,0,1);
			Homography2D_F64 H_scale_inv = H_scale.invert(null);

			Homography2D_F64 H_layer = H_scale.concat(H,null).concat(H_scale_inv,null);

			H_layer = processLayer( src.getLayer(layer),this.dst.getLayer(layer),
					srcDericX.getLayer(layer),srcDericY.getLayer(layer),H_layer);

			// convert the scale back up into the original image scale
			H_scale_inv.concat(H_layer,null).concat(H_scale,H);
		}

		result.set(H);

		return true;
	}

	private Homography2D_F64 processLayer( I src, I dst , D srcDerivX , D srcDerivY,  Homography2D_F64 H_init ) {
		function.setInputs(src,dst);
		gradient.setInputs(src,dst,srcDerivX,srcDerivY);

		minimizer.setFunction(function,gradient,0);
//		minimizer.setFunction(function,new NumericalGradientForward(function,0.1));

		double param[] = new double[]{
				H_init.a11,H_init.a12,H_init.a13,
				H_init.a21,H_init.a22,H_init.a23,
				H_init.a31,H_init.a32,H_init.a33};
		minimizer.initialize(param,1e-20,1e-20);

//		System.out.println("initial error "+minimizer.getFunctionValue());
		UtilOptimize.process(minimizer,1000);
//		System.out.println("after error "+minimizer.getFunctionValue());

		Homography2D_F64 result = new Homography2D_F64();

		double found[] = minimizer.getParameters();
		result.a11 = found[0];
		result.a12 = found[1];
		result.a13 = found[2];
		result.a21 = found[3];
		result.a22 = found[4];
		result.a23 = found[5];
		result.a31 = found[6];
		result.a32 = found[7];
		result.a33 = found[8];

		return result;
	}


	public Homography2D_F64 getRefinement() {
		return result;
	}
}
