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

import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import validate.features.homography.CreateDescriptionFile;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CreateDescriptionFileSiftN extends CreateDescriptionFile<ImageFloat32,SurfFeature> {
	/**
	 * Defines the set of images and detection files that are to be processed.
	 *
	 * @param descriptionName The name of the description algorithm.  This name is appended to output files.
	 */
	public CreateDescriptionFileSiftN(String descriptionName) {
		super(FactorySift.createDescriptor(), ImageType.single(ImageFloat32.class), descriptionName);
	}

	@Override
	protected List<Description> process( double x , double y , double theta , double scale )
	{
		List<Description> ret = new ArrayList<Description>();

		DescribeOrientationSift sift = (DescribeOrientationSift)describe;

		List<SurfFeature> found = sift.process(x,y,scale);

		for( int i = 0; i < found.size(); i++ ) {
			Description d = new Description();
			d.desc = found.get(i);
			d.yaw = sift.orientation.getOrientations().get(i);
			d.x = x;
			d.y = y;
			d.scale = scale;
			ret.add(d);
		}

		return ret;
	}

	public static void doStuff( String directory , String imageSuffix ) throws FileNotFoundException {

		CreateDescriptionFileSiftN cdf =
				new CreateDescriptionFileSiftN("BOOFCV_SIFTN");

		cdf.directory(directory,imageSuffix,"OpenIMAJ_SIFT.txt");
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
