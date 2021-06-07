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

import boofcv.common.BoofRegressionConstants;
import boofcv.io.image.UtilImageIO;
import boofcv.regression.DetectDescribeRegression;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.ConvertFloatType;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.transform.homography.HomographyPointOps_F32;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Benchmarks algorithms against a sequence of real images where the homography between the images
 * is known.
 *
 * @author Peter Abeles
 */
public class BenchmarkFeatureDetectStability {
	List<Homography2D_F32> transforms;
	double tolerance;
	double scaleTolerance = 0.25;

	List<String> nameBase = new ArrayList<String>();

	int numMatches;
	double fractionCorrect;

	double fractionAmbiguous;

	int totalMatches;
	double totalCorrect;
	double totalAmbiguous;

	List<String> directories = new ArrayList<String>();

	String outputLocation;
	String inputLocation;
	PrintStream output;

	// image dimensions
	int width;
	int height;

	public BenchmarkFeatureDetectStability(String outputLocation,
										   String inputLocation,
										   double tolerance)
	{
		this.outputLocation = outputLocation;
		this.inputLocation = inputLocation;
		this.tolerance = tolerance;
	}

	public void addDirectory( String dir ) {
		directories.add(dir);
	}

	/**
	 * Scans the directory for images with the specified suffix. These names are
	 * used to find all the description files.
	 *
	 * @param directory Directory containing images and description files.
	 * @param imageSuffix Type of input images.
	 * @return Names of input images.
	 */
	private List<String> loadNameBase(String directory, String imageSuffix) {
		List<String> ret = new ArrayList<String>();

		List<File> files = BoofRegressionConstants.listAndSort(new File(directory));

		for( File f : files ) {
			if( !(f.isFile() && f.getName().endsWith(imageSuffix))) {
				continue;
			}

			String name = f.getName();
			ret.add( name.substring(0,name.length()-imageSuffix.length()));
		}

		return ret;
	}

	/**
	 * For each input image it loads the specified descriptions. These are then associated
	 * against each other and the results compared.
	 *
	 * @param algSuffix String used to identify feature description files.
	 */
	public void evaluate( String algSuffix ) throws FileNotFoundException {
		System.out.println("\n"+algSuffix);
		output = new PrintStream(new File(outputLocation,"ACC_detect_stability_"+algSuffix+".txt"));
		BoofRegressionConstants.printGenerator(output, DetectDescribeRegression.class);
		output.println("tolerance = "+tolerance);
		output.println("scaleTolerance = "+scaleTolerance);
		output.println();

		totalCorrect = 0;
		totalMatches = 0;
		for( String dataSetDirectory : directories ) {
			try {
				processDirectory(dataSetDirectory,algSuffix);
			} catch( RuntimeException e ) {
				// data missing, just skip over it
			}
		}

		System.out.println("Summary Score:");
		System.out.println("   num matches     = "+totalMatches);
		System.out.println("   total correct   = "+totalCorrect);
		System.out.println("   total ambiguous = "+totalAmbiguous);
		output.println("Summary Score:");
		output.println("   num matches     = "+totalMatches);
		output.println("   total correct   = "+totalCorrect);
		output.println("   total ambiguous = "+totalAmbiguous);

		output.close();
	}

	private void processDirectory( String dataSetDirectory , String algSuffix ) {
//		System.out.println("Directory: "+directory);
		output.println("---------- Directory: "+dataSetDirectory);

		findImageSize(dataSetDirectory);

		String dataSetName = new File(dataSetDirectory).getName();

		nameBase = BenchmarkFeatureDescribeStability.loadNameBase(dataSetDirectory);

		transforms = new ArrayList<Homography2D_F32>();
		for( int i=1; i < nameBase.size(); i++ ) {
			String fileName = "H1to"+(i+1)+"p";
			Homography2D_F64 H64 = LoadHomographyBenchmarkFiles.loadHomography(new File(dataSetDirectory , fileName).getPath());
			Homography2D_F32 h = ConvertFloatType.convert(H64, null);
			transforms.add(h);
		}

		List<DetectionInfo>[] detections = new ArrayList[nameBase.size()];
		for( int i = 0; i < nameBase.size(); i++ ) {
			String detectName = new File(inputLocation,String.format("DETECTED_%s_%s_%s.txt", dataSetName, nameBase.get(i), algSuffix)).getPath();
			detections[i] = LoadHomographyBenchmarkFiles.loadDetection(detectName);
		}

		List<Integer> matches = new ArrayList<>();
		List<Double> fractions = new ArrayList<>();

		for( int i = 1; i < nameBase.size(); i++ ) {

			Homography2D_F32 keyToTarget = transforms.get(i-1);

			associationScore(detections[0],detections[i],keyToTarget);
			totalCorrect += fractionCorrect;
			totalMatches += numMatches;
			totalAmbiguous += fractionAmbiguous;
			matches.add(numMatches);
			fractions.add(fractionCorrect);
			output.print(nameBase.get(i)+" ");
//			System.out.printf(" %5d %4.2f %4.2f\n",numMatches,fractionCorrect,fractionAmbiguous);
		}
		output.println();

		for( int m : matches ) {
			output.print(m+" ");
		}
		output.println();
		for( double f : fractions ) {
			output.printf("%6.4f ", f);
		}
		output.println();
	}

	private void findImageSize(String directory) {

		if( directory.charAt(directory.length()-1) == '/')
			directory = directory.substring(0,directory.length()-1);

		String a = String.format("%s/img%d.ppm", directory, 1);
		String b = String.format("%s/img%d.pgm", directory, 1);

		String imageName = new File(a).exists() ? a : b;

		BufferedImage image = UtilImageIO.loadImage(imageName);
		width = image.getWidth();
		height = image.getHeight();
	}

	/**
	 * Associates two sets of features against each other.
	 */
	private void associationScore(List<DetectionInfo> keyFrame,
								  List<DetectionInfo> targetFrame,
								  Homography2D_F32 keyToTarget) {


		// the number of key frame features which have a correspondence
		int maxCorrect = 0;
		// number of correct associations
		int numCorrect = 0;
		// number of ambiguous matches
		int numAmbiguous = 0;

		Point2D_F32 src = new Point2D_F32();
		Point2D_F32 expected = new Point2D_F32();

		Point2D_F32[] sample = new Point2D_F32[4];
		for( int i = 0; i < sample.length; i++ )
			sample[i] = new Point2D_F32();
		Point2D_F32 sampleDst = new Point2D_F32();

		for( DetectionInfo k : keyFrame ) {
			src.setTo((float)k.location.x,(float)k.location.y);
			sample[0].setTo(src.x + 1, src.y);
			sample[1].setTo(src.x - 1, src.y);
			sample[2].setTo(src.x,src.y+1);
			sample[3].setTo(src.x,src.y-1);

			HomographyPointOps_F32.transform(keyToTarget, src, expected);
			// estimate how the transform would rescale the image
			double expectedScale = 0;
			for( Point2D_F32 s : sample ) {
				HomographyPointOps_F32.transform(keyToTarget, s, sampleDst);
				expectedScale += expected.distance(sampleDst);
			}
			expectedScale /= sample.length;
			expectedScale = k.getScale()*expectedScale;

			if( expected.x < 0 || expected.y < 0 || expected.x >= width || expected.y >= height) {
				continue;
			}

			int numMatched = 0;
			for( DetectionInfo t : targetFrame ) {
				double dist = UtilPoint2D_F64.distance(expected.x, expected.y, t.location.x, t.location.y);
				double scaleDiff = Math.abs(t.scale - expectedScale)/expectedScale;
				if( dist < tolerance && scaleDiff < scaleTolerance ) {
					numMatched++;
				}
			}

			if( numMatched <= 1 )
				maxCorrect++;

			if( numMatched == 1 ) {
				numCorrect++;
			} else if( numMatched > 1 )
				numAmbiguous++;
		}

		numMatches = maxCorrect;
		fractionCorrect = ((double)numCorrect)/((double)maxCorrect);
		fractionAmbiguous = ((double)numAmbiguous)/((double)maxCorrect);
	}

	public static void main(String[] args) throws FileNotFoundException {
		double tolerance = 1.5;

		BenchmarkFeatureDetectStability app = new BenchmarkFeatureDetectStability( "","",tolerance);

		app.addDirectory("data/bikes/");
		app.addDirectory("data/boat/");
		app.addDirectory("data/graf/");
		app.addDirectory("data/leuven/");
		app.addDirectory("data/ubc/");
		app.addDirectory("data/trees/");
		app.addDirectory("data/wall/");
		app.addDirectory("data/bark/");

//		app.evaluate("JavaSIFT");
//		app.evaluate("OpenIMAJ_SIFT");
		app.evaluate("BOOFCV_SIFT");
//		app.evaluate("OpenSIFT");

//		app.evaluate("FH");
//		app.evaluate("PanOMatic");
//		app.evaluate("OpenSURF");
//		app.evaluate("OpenCV");
//		app.evaluate("SURF_REFERENCE");
//		app.evaluate("JOpenSURF");
//		app.evaluate("JavaSURF");
	}
}
