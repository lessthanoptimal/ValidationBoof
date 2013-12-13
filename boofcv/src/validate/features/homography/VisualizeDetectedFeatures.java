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

import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeDetectedFeatures {
	public static void main( String args[] ) {

		display("data/bikes", "img1", "SIFT_REFERENCE");
		display("data/bikes", "img2", "SIFT_REFERENCE");
		display("data/bikes", "img3", "SIFT_REFERENCE");
//		display("data/trees", "img2", "OpenSIFT");

//		display("data/trees", "img1", "PanOMatic");
//		display("data/trees", "img1", "SURF");
//		display("data/trees", "img1", "FH");
//		display("data/trees", "img1", "OpenSURF");
	}

	private static void display(String directory, String imageName, String algSuffix) {
		String detectName = String.format("%s/DETECTED_%s_%s.txt", directory, imageName, algSuffix);
		List<DetectionInfo> detections = LoadHomographyBenchmarkFiles.loadDetection(detectName);

		BufferedImage image = UtilImageIO.loadImage(String.format("%s/%s.png", directory, imageName));

		Graphics2D g2 = image.createGraphics();

		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));

		for( DetectionInfo d : detections ) {
			double r = d.getScale()*2.5;

			int w = (int) Math.round(r * 2);
			int x = (int) Math.round(d.location.x - r);
			int y = (int) Math.round(d.location.y - r);

			g2.drawOval(x,y,w,w);
		}
		System.out.println(algSuffix+" Total detected "+detections.size());
		ShowImages.showWindow(image, "Detected "+algSuffix);
	}
}
