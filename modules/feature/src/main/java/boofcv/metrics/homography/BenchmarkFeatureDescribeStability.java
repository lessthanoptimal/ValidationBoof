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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociateEuclideanSq;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.common.BoofRegressionConstants;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.regression.DetectDescribeRegression;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Benchmarks algorithms against a sequence of real images where the homography between the images
 * is known.
 *
 * @author Peter Abeles
 */
public class BenchmarkFeatureDescribeStability {
	AssociateDescription<TupleDesc_F64> assoc;
	List<Homography2D_F64> transforms;
	double tolerance;

	List<String> nameBase = new ArrayList<String>();

	int maxPossibleMatches;
	int numTruePositive;
	int numFalsePositive;

	// the most it could have matched
	int totalMatches;
	int totalFeatures;
	double sumPrecision;
	double sumRecall;
	double sumFMeasure;


	List<String> directories = new ArrayList<String>();

	PrintStream output;
	String outputLocation;
	String inputLocation;

	public BenchmarkFeatureDescribeStability(AssociateDescription<TupleDesc_F64> assoc,
											 String outputLocation,
											 String inputLocation,
											 double tolerance) {

		this.assoc = assoc;
		this.outputLocation = outputLocation;
		this.inputLocation = inputLocation;
		this.tolerance = tolerance;
	}

	public void addDirectory( String dir ) {
		directories.add(dir);
	}

	/**
	 * Scans the directory for images with the specified suffix.  These names are
	 * used to find all the description files.
	 *
	 * @param directory Directory containing images and description files.
	 * @return Names of input images.
	 */
	public static List<String> loadNameBase(String directory) {
		List<String> ret = new ArrayList<String>();
		File dir = new File(directory);

		for( File f : dir.listFiles() ) {
			if( !(f.isFile() && (f.getName().endsWith(".pgm") || f.getName().endsWith(".ppm")) )) {
				continue;
			}

			String name = f.getName();
			ret.add( name.substring(0,name.length()-4) );
		}

		// put the names into order
		Collections.sort(ret);

		return ret;
	}

	/**
	 * For each input image it loads the specified descriptions.  These are then associated
	 * against each other and the results compared.
	 *
	 * @param algSuffix String used to identify feature description files.
	 */
	public void evaluate( String algSuffix ) throws FileNotFoundException {
		System.out.println("\n"+algSuffix);
		output = new PrintStream(new File(outputLocation,"ACC_describe_stability_"+algSuffix+".txt"));
		BoofRegressionConstants.printGenerator(output, DetectDescribeRegression.class);
		output.println("tolerance = "+tolerance);
		output.println("# rows = Max Matches | Precision | Recall | F-measure");
		output.println();

		totalMatches = 0;
		totalFeatures = 0;
		sumPrecision = 0;
		sumRecall = 0;
		sumFMeasure = 0;

		for( String dir : directories ) {
			try {
				processDirectory(dir,algSuffix+".txt");
			} catch( RuntimeException e ) {
				// just ignore it
			}
		}

		System.out.println("Summary Score:");
		System.out.println("   num features  = " + totalFeatures);
		System.out.println("   max matches   = " + totalMatches);
		System.out.println("   sumPrecision  = " + sumPrecision);
		System.out.println("   sumRecall     = " + sumRecall);
		System.out.println("   sumFMeasure   = " + sumFMeasure);
		output.println("Summary Score:");
		output.println("   num features  = " + totalFeatures);
		output.println("   max matches   = " + totalMatches);
		output.println("   sumPrecision  = " + sumPrecision);
		output.println("   sumRecall     = " + sumRecall);
		output.println("   sumFMeasure   = " + sumFMeasure);

		output.close();
	}

	private void processDirectory( String imageDirectory , String algSuffix ) {
		System.out.println("Directory: "+imageDirectory);
		output.println("---------- Directory: "+imageDirectory);

		String dataSetName = new File(imageDirectory).getName();

		nameBase = loadNameBase( imageDirectory );

		transforms = new ArrayList<Homography2D_F64>();
		for( int i=1; i < nameBase.size(); i++ ) {
			String fileName = "H1to"+(i+1)+"p";
			transforms.add( LoadHomographyBenchmarkFiles.loadHomography(imageDirectory + "/" + fileName));
		}

		String descriptionName = new File(inputLocation,"DESCRIBE_"+dataSetName+"_"+nameBase.get(0)+"_"+algSuffix).getPath();
		// load descriptions in the keyframe
		List<FeatureInfo> keyFrame = LoadHomographyBenchmarkFiles.loadDescription(descriptionName);

		List<Integer> listMaxPossible = new ArrayList<Integer>();
		List<Double> listPrecision = new ArrayList<Double>();
		List<Double> listRecall = new ArrayList<Double>();
		List<Double> listFMeas = new ArrayList<Double>();

		for( int i = 1; i < nameBase.size(); i++ ) {
//			System.out.print("Examining "+nameBase.get(i));

			descriptionName = new File(inputLocation,"DESCRIBE_"+dataSetName+"_"+nameBase.get(i)+"_"+algSuffix).getPath();
			List<FeatureInfo> targetFrame = LoadHomographyBenchmarkFiles.loadDescription(descriptionName);

			Homography2D_F64 keyToTarget = transforms.get(i-1);

			associationScore(keyFrame,targetFrame,keyToTarget);

			double precision = numTruePositive/(double)(numTruePositive+numFalsePositive);
			double recall = numTruePositive/(double)maxPossibleMatches;
			double fmeas = 2*(precision*recall)/(precision + recall);

			if( Double.isInfinite(precision) )
				precision = 0;
			if( Double.isInfinite(recall) )
				recall = 0;
			if( Double.isNaN(fmeas) )
				fmeas = 0;

			totalMatches += maxPossibleMatches;
			totalFeatures += targetFrame.size();
			sumPrecision += precision;
			sumRecall += recall;
			sumFMeasure += fmeas;

			listMaxPossible.add(maxPossibleMatches);
			listPrecision.add(precision);
			listRecall.add(recall);
			listFMeas.add(fmeas);

			output.print(nameBase.get(i)+" ");
			System.out.printf(" %5s %5d %5.2f\n",nameBase.get(i), maxPossibleMatches,fmeas);
			System.gc();
		}
		output.println();

		for( int m : listMaxPossible ) {
			output.print(m+" ");
		}
		output.println();
		for( double f : listPrecision ) {
			output.printf("%6.4f ", f);
		}
		output.println();
		for( double f : listRecall ) {
			output.printf("%6.4f ", f);
		}
		output.println();
		for( double f : listFMeas ) {
			output.printf("%6.4f ", f);
		}
		output.println();
	}

	/**
	 * Associates two sets of features against each other.
	 * @param keyFrame
	 * @param targetFrame
	 * @param keyToTarget
	 */
	private void associationScore(List<FeatureInfo> keyFrame,
								  List<FeatureInfo> targetFrame,
								  Homography2D_F64 keyToTarget) {

		FastArray<TupleDesc_F64> listSrc = new FastArray<>(TupleDesc_F64.class,keyFrame.size());
		FastArray<TupleDesc_F64> listDst = new FastArray<>(TupleDesc_F64.class,keyFrame.size());

		for( FeatureInfo f : keyFrame ) {
			listSrc.add(f.getDescription());
		}

		for( FeatureInfo f : targetFrame ) {
			listDst.add(f.getDescription());
		}

		assoc.setSource(listSrc);
		assoc.setDestination(listDst);
		assoc.associate();

		FastAccess<AssociatedIndex> matches = assoc.getMatches();

		Point2D_F64 expected = new Point2D_F64();


		// number of correct associations
		numTruePositive = 0;
		numFalsePositive = 0;

		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex a = matches.get(i);
			Point2D_F64 s = keyFrame.get(a.src).getLocation();
			Point2D_F64 d = targetFrame.get(a.dst).getLocation();

			HomographyPointOps_F64.transform(keyToTarget, s, expected);

			double dist = UtilPoint2D_F64.distance(expected.x, expected.y, (float) d.x, (float) d.y);

			if( dist <= tolerance ) {
				numTruePositive++;
			} else {
				numFalsePositive++;
			}
		}

		maxPossibleMatches = computeMaxPossibleMatches(keyFrame,targetFrame,keyToTarget);
	}

	private int computeMaxPossibleMatches( List<FeatureInfo> keyFrame,
										   List<FeatureInfo> targetFrame,
										   Homography2D_F64 keyToTarget )
	{
		int total = 0;

		Point2D_F64 expected = new Point2D_F64();

		for( FeatureInfo a : keyFrame ) {
			HomographyPointOps_F64.transform(keyToTarget, a.location, expected);

			if( hasCorrespondence(expected,targetFrame) ) {
				total++;
			}
		}

		return total;
	}

	private boolean hasCorrespondence( Point2D_F64 expected, List<FeatureInfo> targetFrame) {

		for( FeatureInfo t : targetFrame ) {
			Point2D_F64 d = t.getLocation();
			double dist = UtilPoint2D_F64.distance(expected.x, expected.y, (float) d.x, (float) d.y);
			if( dist <= tolerance)
				return true;
		}
		return false;
	}

	public static void main( String args[] ) throws FileNotFoundException {
		double tolerance = 3;

		// No backwards validation.  Want to show strength of descriptor and post processing validation
		ConfigAssociateGreedy configGreedy = new ConfigAssociateGreedy();
		configGreedy.forwardsBackwards = false;
		configGreedy.maxErrorThreshold = -1;

		ScoreAssociation<TupleDesc_F64> score = new ScoreAssociateEuclideanSq.F64();
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.greedy(configGreedy,score);

		BenchmarkFeatureDescribeStability app = new BenchmarkFeatureDescribeStability(assoc, "","",tolerance);

		app.addDirectory("data/bikes/");
		app.addDirectory("data/boat/"); // Comment out for color
		app.addDirectory("data/graf/");
		app.addDirectory("data/leuven/");
		app.addDirectory("data/ubc/");
		app.addDirectory("data/trees/");
		app.addDirectory("data/wall/");
		app.addDirectory("data/bark/");

//		app.evaluate("JavaSIFT.txt");
//		app.evaluate("BOOFCV_SIFT1.txt");
//		app.evaluate("OpenIMAJ_SIFT.txt");
//		app.evaluate("VLFeat_SIFT.txt");
		app.evaluate("BOOFCV_SIFTN.txt");
//		app.evaluate("OpenSIFT.txt");
//		app.evaluate("SIFT_REFERENCE.txt");

//		app.evaluate("SURF.txt");
//		app.evaluate("JavaSURF.txt");
//		app.evaluate("PanOMatic.txt");
//		app.evaluate("JOpenSURF.txt");
//		app.evaluate("OpenSURF.txt");
//		app.evaluate("OpenCV_SURF.txt");
//		app.evaluate("BoofCV_SURF.txt");
//		app.evaluate("BoofCV_MSURF.txt");
//		app.evaluate("BoofCV_MSURF_COLOR.txt");

	}
}
