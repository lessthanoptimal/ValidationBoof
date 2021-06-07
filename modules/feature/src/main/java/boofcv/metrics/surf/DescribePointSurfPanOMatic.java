/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of the SURF Performance Benchmark
 * (https://github.com/lessthanoptimal/SURFPerformance).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.metrics.surf;

import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;

public class DescribePointSurfPanOMatic<II extends ImageGray<II>> extends DescribePointSurf<II> {

	double _cmp[][][];
	int iVecLen = 4;

	public DescribePointSurfPanOMatic(int widthLargeGrid, int widthSubRegion, int widthSample,
									  double weightSigma, boolean useHaar, Class<II> imageType) {
		super(widthLargeGrid, widthSubRegion, widthSample, weightSigma, useHaar,imageType);

		// allocate and initialize the components of the vector
		// the idea is to allocate 2 more in each direction and
		// shift access by 1 to discard out of bounds.

		int aExtSub = widthLargeGrid + 2;
		_cmp = new double [aExtSub][][];
		for (int aYIt = 0; aYIt < aExtSub; ++aYIt)
		{
			_cmp[aYIt] = new double [aExtSub][];
			for (int aXIt = 0; aXIt < aExtSub; ++aXIt)
			{
				_cmp[aYIt][aXIt] = new double[iVecLen];
				for (int aVIt = 0; aVIt < iVecLen; ++aVIt)
					_cmp[aYIt][aXIt][aVIt] = 0;
			}
		}
	}

	/**
	 * Create a SURF-64 descriptor. See [1] for details.
	 */
	public DescribePointSurfPanOMatic(Class<II> imageType) {
		this(4,5,2, 4.5 , true,imageType);
	}

	/**
	 * <p>
	 * Computes the SURF descriptor for the specified interest point. If the feature
	 * goes outside of the image border (including convolution kernels) then null is returned.
	 * </p>
	 *
	 * @param c_x Location of interest point.
	 * @param c_y Location of interest point.
	 * @param angle The angle the feature is pointing at in radians.
	 * @param ret storage for the feature. Must have 64 values. If null a new feature will be declared internally.
	 */
	@Override
	public void describe(double c_x, double c_y,
						 double angle, double radius,
						 boolean normalize,
						 TupleDesc_F64 ret)
	{

		double c = Math.cos(angle);
		double s = -Math.sin(angle);

		// declare the feature if needed
		if( ret == null )
			ret = new TupleDesc_F64(featureDOF);
		else if( ret.data.length != featureDOF )
			throw new IllegalArgumentException("Provided feature must have "+featureDOF+" values");

		// extract descriptor
		gradient.setImage(ii);
//TODO		gradient.setWidth(scale*1.65);

		zeroCmp();

//TODO		foo(c_x,c_y,scale*1.65,c,s);
		
		// fill the vector with the values of the square...
		// remember the shift of 1 to drop outborders.
		int _subRegions = widthLargeGrid;
		int aV = 0;
		for(int aYIt = 1; aYIt < _subRegions+1; ++aYIt)
		{
			for(int aXIt = 1; aXIt < _subRegions+1; ++aXIt)
			{
				for(int aVIt = 0; aVIt < iVecLen; ++aVIt)
				{
					double a = _cmp[aYIt][aXIt][aVIt];
					ret.data[aV] = a;
					aV++;
				}
			}
		}

		// normalize feature vector to have an Euclidean length of 1 adds light invariance
		if( normalize )
			UtilFeature.normalizeL2(ret);
	}

	private void zeroCmp() {
		for (int aYIt = 0; aYIt < _cmp.length; ++aYIt)
		{
			for (int aXIt = 0; aXIt < _cmp[aYIt].length; ++aXIt)
			{
				double d[] = _cmp[aYIt][aXIt];
				for (int aVIt = 0; aVIt < d.length; ++aVIt)
					d[aVIt] = 0;
			}
		}
	}

	protected void foo( double aX , double aY , double aS , double c , double s ) {
		int _subRegions = widthLargeGrid;
		double _magFactor = 12.0 / _subRegions;
		
		// make integer values from double ones
		int aIntX = (int) Math.round(aX);
		int aIntY = (int) Math.round(aY);
		int aIntS = (int) Math.round(aS / 2.0);
		if (aIntS < 1) aIntS = 1;

		// calc subpixel shift
		double aSubX = aX - aIntX;
		double aSubY = aY - aIntY;

		// calc subpixel shift in rotated coordinates
		double aSubV = c * aSubY + s * aSubX;
		double aSubU = - s * aSubY + c * aSubX;

		// calc step of sampling
		double aStepSample = aS * _magFactor;

		// make a bounding box around the rotated patch square.
		double aRadius = (1.414 * aStepSample) * (_subRegions + 1) / 2.0;
		int aIntRadius = (int) Math.round(aRadius / aIntS);

		double sigma = FactoryKernelGaussian.sigmaForRadius(aIntRadius,2);
		weight = FactoryKernelGaussian.gaussianWidth(sigma*3, aIntRadius*2+1);

		// go through all the pixels in the bounding box
		for (int aYIt = -aIntRadius; aYIt <= aIntRadius; ++aYIt)
		{
			for (int aXIt = -aIntRadius; aXIt <= aIntRadius; ++aXIt)
			{
				// calculate U,V rotated values from X,Y taking in account subpixel correction
				// divide by step sample to put in index units
				double aU = ((( - s * aYIt + c * aXIt) * aIntS) - aSubU) / aStepSample;
				double aV = (((c * aYIt + s * aXIt) * aIntS) - aSubV) / aStepSample;

				// compute location of sample in terms of real array coordinates
				double aUIdx = _subRegions / 2.0 - 0.5 + aU;
				double aVIdx = _subRegions / 2.0 - 0.5 + aV;

				// test if some bins will be filled.
				if (aUIdx >= -1.0 && aUIdx < _subRegions &&
						aVIdx >= -1.0 && aVIdx < _subRegions )
				{
					int aXSample = aIntS * aXIt + aIntX;
					int aYSample = aIntS * aYIt + aIntY;

					if (!gradient.isInBounds(aXSample, aYSample))
						continue;

					GradientValue v = gradient.compute(aXSample, aYSample);
					
					double aExp = weight.get(aXIt + aIntRadius, aYIt + aIntRadius);

					double aWavX = v.getY() * aExp;
					double aWavY = v.getX() * aExp;

					double aWavXR = (c * aWavX + s * aWavY);
					double aWavYR = (s * aWavX - c * aWavY);

					// due to the rotation, the patch has to be dispatched in 2 bins in each direction
					// get the bins and weight for each of them
					// shift by 1 to avoid checking bounds
					final int aBin1U = (int)(aUIdx + 1.0);
					final int aBin2U = aBin1U + 1;
					final int aBin1V = (int)(aVIdx + 1.0);
					final int aBin2V = aBin1V + 1;

					final double aWeightBin1U = aBin1U - aUIdx;
					final double aWeightBin2U = 1 - aWeightBin1U;

					final double aWeightBin1V = aBin1V - aVIdx;
					final double aWeightBin2V = 1 - aWeightBin1V;

					int aBin = (aWavXR <= 0) ? 0 : 1;
					_cmp[aBin1V][aBin1U][aBin] += aWeightBin1V * aWeightBin1U * aWavXR;
					_cmp[aBin2V][aBin1U][aBin] += aWeightBin2V * aWeightBin1U * aWavXR;
					_cmp[aBin1V][aBin2U][aBin] += aWeightBin1V * aWeightBin2U * aWavXR;
					_cmp[aBin2V][aBin2U][aBin] += aWeightBin2V * aWeightBin2U * aWavXR;

					aBin = (aWavYR <= 0) ? 2 : 3;
					_cmp[aBin1V][aBin1U][aBin] += aWeightBin1V * aWeightBin1U * aWavYR;
					_cmp[aBin2V][aBin1U][aBin] += aWeightBin2V * aWeightBin1U * aWavYR;
					_cmp[aBin1V][aBin2U][aBin] += aWeightBin1V * aWeightBin2U * aWavYR;
					_cmp[aBin2V][aBin2U][aBin] += aWeightBin2V * aWeightBin2U * aWavYR;
				}
			}
		}
	}
}
