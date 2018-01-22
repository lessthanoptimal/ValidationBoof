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

package boofcv.metrics.homography;


import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Evaluates how fast interest points are detected and described at runtime.
 *
 * @author Peter Abeles
 */
public class BenchmarkFeatureAllRuntime<T extends ImageGray<T>, D extends TupleDesc> {

	Class<T> imageType;
	DetectDescribePoint<T,D> alg;

	public BenchmarkFeatureAllRuntime(Class<T> imageType,
									  DetectDescribePoint<T,D> alg ) {
		this.imageType = imageType;
		this.alg = alg;
	}

	public void benchmark( String directory , int imageNumber )
			throws IOException
	{
		String imageName = String.format("%s/img%d.png", directory, imageNumber);

		BufferedImage image = ImageIO.read(new File(imageName));

		T input = ConvertBufferedImage.convertFromSingle(image, null, imageType);

		long best = Long.MAX_VALUE;

		for( int i = 0; i < 10; i++ ) {

			long before = System.currentTimeMillis();

			alg.detect(input);

			long after = System.currentTimeMillis();
			long elapsed = after-before;

			System.out.println("time = "+elapsed+" num detected "+ alg.getNumberOfFeatures());

			if( elapsed < best )
				best = elapsed;
		}

		System.out.println();
		System.out.println("Best = "+best);
	}
}
