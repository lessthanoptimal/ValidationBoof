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

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.metrics.homography.BenchmarkFeatureDescribeRuntime;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;

import java.io.IOException;

import static boofcv.metrics.surf.FactorySurf.surf;

/**
 * @author Peter Abeles
 */
public class BenchmarkRuntimeDescribeSurf {
	public static void main( String args[] ) throws IOException {

		DescribeRegionPoint<GrayF32, TupleDesc_F64> alg = surf(false, ImageType.single(GrayF32.class));

		BenchmarkFeatureDescribeRuntime<GrayF32,TupleDesc_F64> benchmark =
				new BenchmarkFeatureDescribeRuntime<GrayF32,TupleDesc_F64>(GrayF32.class,alg);

		benchmark.benchmark("data/boat", 1, "FH");
	}
}
