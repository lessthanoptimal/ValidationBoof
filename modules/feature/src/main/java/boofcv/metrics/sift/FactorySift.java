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

import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.factory.feature.describe.FactoryDescribeAlgs;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public class FactorySift {

	public static DescribePointRadiusAngle<GrayF32, TupleDesc_F64>
	createDescriptor() {
		SiftScaleSpace ss = new SiftScaleSpace(-1,5,3,2.75f);
		OrientationHistogramSift orientation = FactoryOrientationAlgs.sift(null,GrayF32.class);
		DescribePointSift sift = FactoryDescribeAlgs.sift(null,GrayF32.class);

		return new DescribeOrientationSift(orientation,sift,ss);
	}

	public static InterestPointDetector<GrayF32>
	createDetector() {
		return FactoryInterestPoint.sift(null,null,GrayF32.class);
	}

	public static DetectDescribePoint<GrayF32,TupleDesc_F64>
	detectDescribe() {
		return FactoryDetectDescribe.sift(null,GrayF32.class);
	}
}
