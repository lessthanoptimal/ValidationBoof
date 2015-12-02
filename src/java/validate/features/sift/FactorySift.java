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
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class FactorySift {

	public static DescribeRegionPoint<ImageFloat32,BrightFeature>
	createDescriptor() {
		SiftScaleSpace ss = new SiftScaleSpace(-1,5,3,2.75f);
		OrientationHistogramSift orientation = FactoryOrientationAlgs.sift(null,ImageFloat32.class);
		DescribePointSift sift = FactoryDescribePointAlgs.sift(null,ImageFloat32.class);

		return new DescribeOrientationSift(orientation,sift,ss);
	}

	public static InterestPointDetector<ImageFloat32>
	createDetector() {
		return FactoryInterestPoint.sift(null,null,ImageFloat32.class);
	}

	public static DetectDescribePoint<ImageFloat32,BrightFeature>
	detectDescribe() {
		return FactoryDetectDescribe.sift(null);
	}
}
