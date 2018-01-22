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

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;


/**
 * Detects the locations of features inside each image and saves to a file.  All the files
 * in a directory are processed that have the appropriate extension.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class CreateDetectionFile<T extends ImageGray<T>> {

	// algorithm that detects the features
	InterestPointDetector<T> alg;
	// estimates the orientation of the region
	OrientationImage<T> orientation;
	// type of input image
	Class<T> imageType;
	// name of the detector
	String detectorName;

	/**
	 * Configures detector
	 *
	 * @param alg Algorithm used to detect interest points.
	 * @param imageType Primitive of input image that is processed.
	 * @param detectorName Name of the detector.  Put into output file name.
	 */
	public CreateDetectionFile(InterestPointDetector<T> alg, OrientationImage<T> orientation,
							   Class<T> imageType, String detectorName) {
		this.alg = alg;
		this.orientation = orientation;
		this.imageType = imageType;
		this.detectorName = detectorName;
	}

	/**
	 * Processes all images found inside a directory.
	 *
	 * @param directoryPath Path to directory containing input images.
	 * @param imageSuffix Type of input image.
	 * @throws java.io.FileNotFoundException
	 */
	public void directory( String directoryPath , String imageSuffix ) throws FileNotFoundException {
		File dir = new File(directoryPath);
		if( !dir.isDirectory() )
			throw new IllegalArgumentException("Path does not point to a directory!");

		System.out.println(directoryPath);
		int totalProcessed = 0;
		File[] files = dir.listFiles();
		for( File f : files ) {
			if( !f.isFile() || !f.getName().endsWith(imageSuffix))
				continue;


			BufferedImage image = UtilImageIO.loadImage(f.getPath());

			String imageName = f.getName();
			directoryPath = f.getParent();
			imageName = imageName.substring(0,imageName.length()-imageSuffix.length());

			process(image, directoryPath + "/DETECTED_" + imageName + "_" + detectorName + ".txt");
			System.out.println("Detected features inside of: " + f.getName() + "  total " + alg.getNumberOfFeatures());
			totalProcessed++;
		}
		System.out.println("Total Processed: "+totalProcessed);
	}

	/**
	 * Detects features in the specified image and saves the output to the specified file.
	 * @param input Input image that features are detected inside of.
	 * @param outputName Name of output file.
	 * @throws java.io.FileNotFoundException
	 */
	public void process( BufferedImage input , String outputName ) throws FileNotFoundException {
		T image = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		alg.detect(image);
		if( orientation != null)
			orientation.setImage(image);

		FileOutputStream fos = new FileOutputStream(outputName);
		PrintStream out = new PrintStream(fos);

		for( int i = 0; i < alg.getNumberOfFeatures(); i++ ) {
			Point2D_F64 pt = alg.getLocation(i);

			if( Double.isNaN(pt.x) || Double.isNaN(pt.y))
				throw new IllegalArgumentException("NaN detected in location");

			double radius = alg.getRadius(i);
			double yaw = 0;
			if( orientation != null ) {
				orientation.setObjectRadius(radius);
				yaw = orientation.compute(pt.getX(),pt.getY());
			}
			out.printf("%.3f %.3f %.5f %.5f\n",pt.getX(),pt.getY(),radius,yaw);
		}
		out.close();
	}
}
