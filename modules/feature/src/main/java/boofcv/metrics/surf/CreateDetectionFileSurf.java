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

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.metrics.homography.CreateDetectionFile;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class CreateDetectionFileSurf {

	public static <T extends ImageGray<T>>
	void doStuff( String directory , String suffix , Class<T> imageType ) throws FileNotFoundException {
		// below are the settings used for detect stability test
		// graf image 1 with 2000 features
		InterestPointDetector<T> alg = FactoryInterestPoint.fastHessian(new ConfigFastHessian(80,1,-1,1,9,4,4));
		// below is the settings used for describe stability test
//		InterestPointDetector<T> alg = FactoryInterestPoint.fastHessian(3, 2, -1, 1, 9, 4, 4);

		CreateDetectionFile<T> cdf = new CreateDetectionFile<T>(alg,null,imageType,"FH");
		cdf.directory(directory,suffix);
	}

	public static void main( String args[] ) throws FileNotFoundException {
		Class imageType = GrayF32.class;

		doStuff("data/bikes/",".png",imageType);
		doStuff("data/boat/",".png",imageType);
		doStuff("data/graf/",".png",imageType);
		doStuff("data/leuven/",".png",imageType);
		doStuff("data/ubc/",".png",imageType);
		doStuff("data/trees/",".png",imageType);
		doStuff("data/wall/",".png",imageType);
		doStuff("data/bark/",".png",imageType);
	}
}
