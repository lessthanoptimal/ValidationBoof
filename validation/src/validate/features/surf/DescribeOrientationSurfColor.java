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

package validate.features.surf;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurfMultiSpectral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;

/**
 * Adds orientation estimation to SURF description calculation.
 *
 * @author Peter Abeles
 */
public class DescribeOrientationSurfColor<T extends ImageSingleBand, II extends ImageSingleBand>
		implements DescribeRegionPoint<MultiSpectral<T>,SurfFeature>
{
	private OrientationIntegral<II> orientation;
	private DescribePointSurfMultiSpectral<II> describe;

	T gray;
	II grayII;
	MultiSpectral<II> bandII;

	ImageType<MultiSpectral<T>> imageType;

	public DescribeOrientationSurfColor(OrientationIntegral<II> orientation,
										DescribePointSurfMultiSpectral<II> describe,
										Class<T> imageType, Class<II> integralType ) {
		this.orientation = orientation;
		this.describe = describe;

		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		grayII = GeneralizedImageOps.createSingleBand(integralType,1,1);
		bandII = new MultiSpectral<II>(integralType,1,1,describe.getNumBands());

		this.imageType = ImageType.ms(describe.getNumBands(), imageType);
	}

	@Override
	public void setImage(MultiSpectral<T> image) {
		gray.reshape(image.width,image.height);
		grayII.reshape(image.width,image.height);
		bandII.reshape(image.width,image.height);

		GConvertImage.average(image, gray);
		GIntegralImageOps.transform(gray, grayII);
		for( int i = 0; i < image.getNumBands(); i++)
			GIntegralImageOps.transform(image.getBand(i), bandII.getBand(i));

		orientation.setImage(grayII);
		describe.setImage(grayII,bandII);
	}

	@Override
	public SurfFeature createDescription() {
		return describe.createDescription();
	}

	@Override
	public boolean process(double x, double y, double angle, double scale, SurfFeature ret) {

		orientation.setScale(scale);
		angle = orientation.compute(x,y);
		describe.describe(x,y, angle, scale, ret);

		return true;
	}

	@Override
	public boolean requiresScale() {
		return false;
	}

	@Override
	public boolean requiresOrientation() {
		return true;
	}

	@Override
	public ImageType<MultiSpectral<T>> getImageType() {
		return imageType;
	}

	@Override
	public Class<SurfFeature> getDescriptionType() {
		return SurfFeature.class;
	}
}
