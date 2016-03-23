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

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.struct.image.GrayF32;
import validate.features.homography.CreateDetectionFile;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class CreateDetectionFileSift {
	public static void doStuff( String directory , String suffix ) throws FileNotFoundException {
		InterestPointDetector<GrayF32> alg = FactorySift.createDetector();

		CreateDetectionFile<GrayF32> cdf =
				new CreateDetectionFile<GrayF32>(alg,null,GrayF32.class,"BOOFCV_SIFT");
		cdf.directory(directory,suffix);
	}

	public static void main( String args[] ) throws FileNotFoundException {
		doStuff("data/bikes/",".png");
		doStuff("data/boat/",".png");
		doStuff("data/graf/",".png");
		doStuff("data/leuven/",".png");
		doStuff("data/ubc/",".png");
		doStuff("data/trees/",".png");
		doStuff("data/wall/",".png");
		doStuff("data/bark/",".png");
	}
}
