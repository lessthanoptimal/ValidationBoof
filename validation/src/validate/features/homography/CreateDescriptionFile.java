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

package validate.features.homography;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.core.image.ConvertBufferedImage;
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
import java.util.ArrayList;
import java.util.List;


/**
 * Creates a file describing each detected image feature.  The input directory is scanned for images
 * and when is found the specified detection file is loaded.
 *
 * @author Peter Abeles
 */
public class CreateDescriptionFile<T extends ImageBase, D extends TupleDesc_F64> {

	// algorithm that describes the features
	protected DescribeRegionPoint<T,D> describe;

	// type of input image
   ImageType<T> imageType;
	// name of the description
	String descriptionName;

	/**
	 * Defines the set of images and detection files that are to be processed.
	 *
	 * @param describe Algorithm which creates a description for the feature.
	 * @param imageType Type of input file.
	 * @param descriptionName The name of the description algorithm.  This name is appended to output files.
	 */
	public CreateDescriptionFile(DescribeRegionPoint<T,D> describe,
                                ImageType<T> imageType,
								 String descriptionName ) {
		this.describe = describe;
		this.imageType = imageType;
		this.descriptionName = descriptionName;
	}

	/**
	 * Scans a all the files in a directory looking for matching image files.  Once a match is found it then
	 * looks up the corresponding detection file.  For each input image a description of all the detected
	 * features is saved to a file.
	 *
	 * @param pathToDirectory Location of the directory being searched.
	 * @param imageSuffix Input image type.  All images have this suffix.
	 * @param detectionSuffix Name of the detection log.
	 * @throws java.io.FileNotFoundException
	 */
	public void directory( String pathToDirectory , String imageSuffix , String detectionSuffix ) throws FileNotFoundException {
		File dir = new File(pathToDirectory);
		if( !dir.isDirectory() )
			throw new IllegalArgumentException("Path does not point to a directory!");

		System.out.println("Directory: "+pathToDirectory);
		int filesFound = 0;
		File[] files = dir.listFiles();
		for( File f : files ) {
			if( !f.isFile() || !f.getName().endsWith(imageSuffix))
				continue;

			System.out.println("Describing features inside of: "+f.getName());

			String imageName = f.getName();
			String directoryPath = f.getParent();
			imageName = imageName.substring(0,imageName.length()-imageSuffix.length());

			File detectionFile = new File(directoryPath+"/DETECTED_"+imageName+"_"+detectionSuffix);
			if( !detectionFile.exists() )
				throw new RuntimeException("Detection file does not exist: "+detectionFile.getName());

			BufferedImage image = UtilImageIO.loadImage(f.getPath());
			process(image,detectionFile.getPath(),directoryPath+"/DESCRIBE_"+imageName+"_"+descriptionName+".txt");
			filesFound++;
		}
		System.out.println("Total files processed: "+filesFound);
	}

	/**
	 * Given the input image, load the specified detection file and save the description of each feature.
	 *
	 * @param input Image being processed.
	 * @param detectionName Path to detection file.
	 * @param outputName Path to output file.
	 * @throws java.io.FileNotFoundException
	 */
	public void process( BufferedImage input , String detectionName , String outputName ) throws FileNotFoundException {
		T image = imageType.createImage(input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFrom(input, image,true);

		describe.setImage(image);

		List<DetectionInfo> detections = LoadHomographyBenchmarkFiles.loadDetection(detectionName);

		FileOutputStream fos = new FileOutputStream(outputName);
		PrintStream out = new PrintStream(fos);

		out.printf("%d\n", describe.createDescription().size());
		for( DetectionInfo d : detections  ) {
			Point2D_F64 p = d.location;
			List<Description> descList = process(p.x, p.y, d.yaw, d.scale);
			for( Description result : descList ) {
				// save the location and tuple description
				out.printf("%.3f %.3f %f",result.x,result.y,result.yaw);
				D desc = result.desc;
				for( int i = 0; i < desc.value.length; i++ ) {
					if( Double.isNaN(desc.value[i]))
						throw new IllegalArgumentException("NaN detected in description");
					out.printf(" %.10f",desc.value[i]);
				}
				out.println();
			}
		}
		out.close();
	}

	protected List<Description> process( double x , double y , double theta , double scale )
	{
		List<Description> ret = new ArrayList<Description>();

		D found = describe.createDescription();
		if( describe.process(x,y,theta,scale,found) ) {
			Description d = new Description();
			d.x = x;
			d.y = y;
			d.yaw = theta;
			d.scale = scale;
			d.desc = found;
			ret.add(d);
		}

		return ret;
	}

	protected class Description
	{
		public double x,y, yaw,scale;
		public D desc;

		public Description() {
		}
	}
}
