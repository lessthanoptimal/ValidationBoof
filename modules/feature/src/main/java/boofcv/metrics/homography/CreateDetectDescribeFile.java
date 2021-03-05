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
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray_F64;

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
public class CreateDetectDescribeFile<T extends ImageBase<T>> {

	// Creates the descriptor once the input image type is fully known
	Factory factory;
	// algorithm that detects the features
	DetectDescribePoint<T,TupleDesc_F64> alg;
	// type of input image
	ImageType<T> imageType;
	// name of the detector
	String algName;

	public final DogArray_F64 processingTimeMS = new DogArray_F64();

	/**
	 * Configures detector
	 *
	 * @param factory Creates a DetectDescribePoint once image type is known
	 * @param algName Name of the detector.  Put into output file name.
	 */
	public CreateDetectDescribeFile(Factory factory,
									ImageType.Family imageFamily, ImageDataType dataType, String algName) {
		this.factory = factory;
		this.imageType = new ImageType<>(imageFamily,dataType,3);
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

		processingTimeMS.reset();

		alg = factory.create(imageType);

		String dataSetName = dir.getName();

		System.out.println(inputDirectory);
		int totalProcessed = 0;
		File[] files = dir.listFiles();
		if( files == null )
			throw new RuntimeException("No files in directory "+dir.getName());
		for( File f : files ) {
			if( !f.isFile() || (!f.getName().endsWith(".ppm") && !f.getName().endsWith(".pgm"))) {
				continue;
			}

			BufferedImage image = UtilImageIO.loadImage(f.getPath());

			String imageName = f.getName();
			imageName = imageName.substring(0,imageName.length()-4);

			String detectName = new File(outputDirectory , "DETECTED_" + dataSetName + "_" + imageName + "_" + algName + ".txt").getPath();
			String describeName = new File(outputDirectory,"DESCRIBE_" + dataSetName + "_" + imageName + "_" + algName + ".txt").getPath();

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

		// If the input image gray scale it will not have the expected number of bands. This isn't known until the
		// image has been loaded. The number of bands is needed since it changes the descriptor size so it
		// can't be dynamically determined by the descriptor
		if( alg.getInputType().numBands != image.getImageType().numBands ) {
			alg = factory.create(image.getImageType());
		}

		long timeBefore = System.nanoTime();
		alg.detect(image);
		long timeAfter = System.nanoTime();

		processingTimeMS.add((timeAfter-timeBefore)*1e-6);

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
			for( int j = 0; j < desc.data.length; j++ ) {
				if( Double.isNaN(desc.data[j]))
					throw new IllegalArgumentException("NaN detected in description");
				outDescribe.printf(" %.10f",desc.data[j]);
			}
			outDescribe.println();
		}
		outDetect.close();
		outDescribe.close();
	}

	public interface Factory {
		<T extends ImageBase<T>>
		DetectDescribePoint<T,TupleDesc_F64> create( ImageType<T> imageType );
	}
}
