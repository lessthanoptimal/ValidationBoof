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

package boofcv.metrics.sift;

import boofcv.BoofDefaults;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.detect.interest.UnrollSiftScaleSpaceGradient;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * SIFT with a single hypothesis for orientation
 *
 * @author Peter Abeles
 */
public class DescribeOrientationSift
		implements DescribeRegionPoint<GrayF32, TupleDesc_F64>
{
	SiftScaleSpace ss;
	UnrollSiftScaleSpaceGradient gradient = new UnrollSiftScaleSpaceGradient();

	OrientationHistogramSift orientation;
	DescribePointSift describe;

	public DescribeOrientationSift(OrientationHistogramSift orientation,
								   DescribePointSift describe,
								   SiftScaleSpace ss ) {
		this.orientation = orientation;
		this.describe = describe;
		this.ss = ss;
	}

	@Override
	public void setImage(GrayF32 image) {
		ss.process(image);
		gradient.process(ss);
	}

	@Override
	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(describe.getDescriptorLength());
	}

	@Override
	public boolean process(double x, double y, double angle, double radius, TupleDesc_F64 ret) {

		double sigma = radius / BoofDefaults.SIFT_SCALE_TO_RADIUS;

		UnrollSiftScaleSpaceGradient.ImageScale image = gradient.lookup(sigma);

		orientation.setImageGradient(image.derivX,image.derivY);
		describe.setImageGradient(image.derivX,image.derivY);

		orientation.process(x,y,sigma);

		angle = orientation.getPeakOrientation();
		describe.process(x,y,sigma,angle,ret);
		return true;
	}

	public List<TupleDesc_F64> process( double x , double y , double radius ) {

		double sigma = radius / BoofDefaults.SIFT_SCALE_TO_RADIUS;

		UnrollSiftScaleSpaceGradient.ImageScale image = gradient.lookup(sigma);

		orientation.setImageGradient(image.derivX,image.derivY);
		describe.setImageGradient(image.derivX,image.derivY);

		orientation.process(x,y,sigma);

		DogArray_F64 found = orientation.getOrientations();

		List<TupleDesc_F64> ret = new ArrayList<TupleDesc_F64>();
		for( int i = 0; i < found.size; i++ ) {
			double angle = found.get(i);

			TupleDesc_F64 f = createDescription();
			describe.process(x,y,sigma,angle,f);

			ret.add(f);
		}

		return ret;
	}

	@Override
	public boolean isScalable() {
		return true;
	}

	@Override
	public boolean isOriented() {
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
	public Class<TupleDesc_F64> getDescriptionType() {
		return TupleDesc_F64.class;
	}
}
