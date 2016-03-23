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

package validate.features.sift;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.detect.interest.UnrollSiftScaleSpaceGradient;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.GrowQueue_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * SIFT with a single hypothesis for orientation
 *
 * @author Peter Abeles
 */
public class DescribeOrientationSift
		implements DescribeRegionPoint<GrayF32,BrightFeature>
{
	UnrollSiftScaleSpaceGradient ss;

	OrientationHistogramSift orientation;
	DescribePointSift describe;

	public DescribeOrientationSift(OrientationHistogramSift orientation,
								   DescribePointSift describe,
								   SiftScaleSpace ss ) {
		this.orientation = orientation;
		this.describe = describe;
		this.ss = new UnrollSiftScaleSpaceGradient(ss);
	}

	@Override
	public void setImage(GrayF32 image) {
		ss.setImage(image);
	}

	@Override
	public BrightFeature createDescription() {
		return new BrightFeature(describe.getDescriptorLength());
	}

	@Override
	public boolean process(double x, double y, double angle, double radius, BrightFeature ret) {

		double sigma = radius / BoofDefaults.SIFT_SCALE_TO_RADIUS;

		UnrollSiftScaleSpaceGradient.ImageScale image = ss.lookup(sigma);

		orientation.setImageGradient(image.derivX,image.derivY);
		describe.setImageGradient(image.derivX,image.derivY);

		orientation.process(x,y,sigma);

		angle = orientation.getPeakOrientation();
		describe.process(x,y,sigma,angle,ret);
		return true;
	}

	public List<BrightFeature> process( double x , double y , double radius ) {

		double sigma = radius / BoofDefaults.SIFT_SCALE_TO_RADIUS;

		UnrollSiftScaleSpaceGradient.ImageScale image = ss.lookup(sigma);

		orientation.setImageGradient(image.derivX,image.derivY);
		describe.setImageGradient(image.derivX,image.derivY);

		orientation.process(x,y,sigma);

		GrowQueue_F64 found = orientation.getOrientations();

		List<BrightFeature> ret = new ArrayList<BrightFeature>();
		for( int i = 0; i < found.size; i++ ) {
			double angle = found.get(i);

			BrightFeature f = createDescription();
			describe.process(x,y,sigma,angle,f);

			ret.add(f);
		}

		return ret;
	}

	@Override
	public boolean requiresRadius() {
		return true;
	}

	@Override
	public boolean requiresOrientation() {
		return false;
	}

	@Override
	public ImageType<GrayF32> getImageType() {
		return null;
	}

	@Override
	public double getCanonicalWidth() {
		throw new RuntimeException("Implement");
	}

	@Override
	public Class<BrightFeature> getDescriptionType() {
		return BrightFeature.class;
	}
}
