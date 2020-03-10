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
import boofcv.abst.feature.associate.ScoreAssociateEuclideanSq_F64;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastArray;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeAssociatedFeatures {

	AssociateDescription<TupleDesc_F64> assoc;

	String directory;
	String name1,name2;
	String algSuffix;

	List<FeatureInfo> features1;
	List<FeatureInfo> features2;

	public VisualizeAssociatedFeatures(AssociateDescription<TupleDesc_F64> assoc,
									   String directory, String algSuffix, String name1, String name2) {
		this.assoc = assoc;
		this.directory = directory;
		this.algSuffix = algSuffix;
		this.name1 = name1;
		this.name2 = name2;
	}

	public void associate() {
		System.out.println("Loading Features");
		// load the features
		features1 = LoadHomographyBenchmarkFiles.loadDescription(directory + "DESCRIBE_" + name1 + "_" + algSuffix + ".txt");
		features2 = LoadHomographyBenchmarkFiles.loadDescription(directory + "DESCRIBE_" + name2 + "_" + algSuffix + ".txt");

		// associate
		FastArray<TupleDesc_F64> listSrc = new FastArray<>(TupleDesc_F64.class,features1.size());
		FastArray<TupleDesc_F64> listDst = new FastArray<>(TupleDesc_F64.class,features2.size());

		for( FeatureInfo f : features1 ) {
			listSrc.add(f.getDescription());
		}

		for( FeatureInfo f : features2 ) {
			listDst.add(f.getDescription());
		}

		System.out.println("Associating");
		assoc.setSource(listSrc);
		assoc.setDestination(listDst);
		assoc.associate();
	}

	public void display() {
		AssociationPanel panel = new AssociationPanel(2);

		BufferedImage image1 = UtilImageIO.loadImage(String.format("%s/%s.png", directory, name1));
		BufferedImage image2 = UtilImageIO.loadImage(String.format("%s/%s.png", directory, name2));

		List<Point2D_F64> loc1 = new ArrayList<Point2D_F64>();
		List<Point2D_F64> loc2 = new ArrayList<Point2D_F64>();


		for( FeatureInfo f : features1 )
			loc1.add(f.location);
		for( FeatureInfo f : features2 )
			loc2.add(f.location);


		panel.setImages(image1,image2);
		panel.setAssociation(loc1,loc2,assoc.getMatches());

		ShowImages.showWindow(panel,"Associations");
	}

	public static void main( String args[] ) {
		ScoreAssociation score = new ScoreAssociateEuclideanSq_F64();

		// No backwards validation.  Want to show strength of descriptor and post processing validation
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.greedy(score, Double.MAX_VALUE, false);

		VisualizeAssociatedFeatures app = new VisualizeAssociatedFeatures(assoc,"data/bikes/",
				"SIFT_REFERENCE","img1","img2");

		app.associate();
		app.display();
	}
}
