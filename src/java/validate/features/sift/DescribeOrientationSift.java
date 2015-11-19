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
import boofcv.abst.feature.detect.interest.WrapSiftDetector;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
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
		implements DescribeRegionPoint<ImageFloat32,SurfFeature>
{
	SiftImageScaleSpace ss;

	OrientationHistogramSift orientation;
	DescribePointSift describe;

	public DescribeOrientationSift(OrientationHistogramSift orientation,
								   DescribePointSift describe,
								   SiftImageScaleSpace ss) {
		this.orientation = orientation;
		this.describe = describe;
		this.ss = ss;
	}

	@Override
	public void setImage(ImageFloat32 image) {
		ss.constructPyramid(image);
		ss.computeDerivatives();

		orientation.setScaleSpace(ss);
		describe.setScaleSpace(ss);
	}

	@Override
	public SurfFeature createDescription() {
		return new SurfFeature(describe.getDescriptorLength());
	}

	@Override
	public boolean process(double x, double y, double angle, double radius, SurfFeature ret) {

		double scale = radius/WrapSiftDetector.SCALE_TO_RADUS;
		orientation.process(x,y,scale);

		angle = orientation.getPeakOrientation();
		describe.process(x,y,scale,angle,
				orientation.getImageIndex(),orientation.getPixelScale(), ret);

//		describe.process(x,y,scale,-angle, ret);
		return true;
	}

	public List<SurfFeature> process( double x , double y , double scale ) {

		List<SurfFeature> ret = new ArrayList<SurfFeature>();

		orientation.process(x,y,scale);
		GrowQueue_F64 found = orientation.getOrientations();

		int imageIndex = orientation.getImageIndex();
		double pixelScale = orientation.getPixelScale();

		for( int i = 0; i < found.size; i++ ) {
			double angle = found.get(i);

			SurfFeature f = createDescription();
			describe.process(x,y,scale,angle,imageIndex,pixelScale,f);

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
	public ImageType<ImageFloat32> getImageType() {
		return null;
	}

	@Override
	public double getCanonicalWidth() {
		throw new RuntimeException("Implement");
	}

	@Override
	public Class<SurfFeature> getDescriptionType() {
		return SurfFeature.class;
	}
}
