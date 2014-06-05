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

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.struct.image.ImageFloat32;
import validate.features.homography.BenchmarkFeatureDetectRuntime;

import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class BenchmarkRuntimeDetectSurf {
	public static void main( String args[] ) throws IOException {

		InterestPointDetector<ImageFloat32> alg = FactoryInterestPoint.
				fastHessian(new ConfigFastHessian(100, 2, -1, 1, 9, 4, 4));


		BenchmarkFeatureDetectRuntime<ImageFloat32> benchmark =
				new BenchmarkFeatureDetectRuntime<ImageFloat32>(ImageFloat32.class,alg);

		benchmark.benchmark("data/boat", 1);
	}
}
