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
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
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
public class CreateDetectDescribeFile<T extends ImageBase<T>, D extends TupleDesc_F64> {

	// algorithm that detects the features
	DetectDescribePoint<T,D> alg;
	// type of input image
	ImageType<T> imageType;
	// name of the detector
	String algName;

	/**
	 * Configures detector
	 *
	 * @param alg Algorithm used to detect and describe interest points.
	 * @param imageType Primitive of input image that is processed.
	 * @param algName Name of the detector.  Put into output file name.
	 */
	public CreateDetectDescribeFile(DetectDescribePoint<T,D> alg,
									ImageType<T> imageType, String algName) {
		this.alg = alg;
		this.imageType = imageType;
		this.algName = algName;
	}

	/**
	 * Processes all images found inside a directory.
	 *
	 *
	 * @param inputDirectory Path to directory containing input images.
	 * @throws java.io.FileNotFoundException
	 */
	public void directory(String inputDirectory, String outputDirectory) throws FileNotFoundException {
		File dir = new File(inputDirectory);
		if( !dir.isDirectory() )
			throw new IllegalArgumentException("Path does not point to a directory!");

		String dataSetName = dir.getName();

		System.out.println(inputDirectory);
		int totalProcessed = 0;
		File[] files = dir.listFiles();
		for( File f : files ) {
			if( !f.isFile() || (!f.getName().endsWith(".ppm") && !f.getName().endsWith(".pgm"))) {
				continue;
			}

			BufferedImage image = UtilImageIO.loadImage(f.getPath());

			String imageName = f.getName();
			inputDirectory = f.getParent();
			imageName = imageName.substring(0,imageName.length()-4);

			String detectName = outputDirectory + "DETECTED_" + dataSetName + "_" + imageName + "_" + algName + ".txt";
			String describeName = outputDirectory+"DESCRIBE_" + dataSetName + "_" + imageName + "_" + algName + ".txt";

			process(image,detectName,describeName);
			System.out.println("Detected features inside of: " + f.getName() + "  total " + alg.getNumberOfFeatures());
			totalProcessed++;
		}
		System.out.println("Total Processed: "+totalProcessed);
	}

	/**
	 * Detects features in the specified image and saves the output to the specified file.
	 * @param input Input image that features are detected inside of.
	 * @throws java.io.FileNotFoundException
	 */
	public void process( BufferedImage input , String detectName , String describeName ) throws FileNotFoundException {
		T image = imageType.createImage(input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFrom(input, image, true);

		alg.detect(image);

		PrintStream outDetect = new PrintStream(new FileOutputStream(detectName));
		PrintStream outDescribe = new PrintStream(new FileOutputStream(describeName));

		TupleDesc_F64 desc = alg.getDescription(0);

		outDescribe.printf("%d\n", desc.size());

		for( int i = 0; i < alg.getNumberOfFeatures(); i++ ) {
			Point2D_F64 pt = alg.getLocation(i);
			double radius = alg.getRadius(i);
			double yaw = alg.getOrientation(i);

			if( Double.isNaN(pt.x) || Double.isNaN(pt.y))
				throw new IllegalArgumentException("NaN detected in location");

			outDetect.printf("%.3f %.3f %.5f %.5f\n", pt.getX(), pt.getY(), radius, yaw);

			desc = alg.getDescription(i);
			outDescribe.printf("%.3f %.3f %f",pt.getX(), pt.getY(),yaw);
			for( int j = 0; j < desc.value.length; j++ ) {
				if( Double.isNaN(desc.value[j]))
					throw new IllegalArgumentException("NaN detected in description");
				outDescribe.printf(" %.10f",desc.value[j]);
			}
			outDescribe.println();
		}
		outDetect.close();
		outDescribe.close();
	}
}
