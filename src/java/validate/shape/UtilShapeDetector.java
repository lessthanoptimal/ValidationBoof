package validate.shape;

import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.ConfigRefinePolygonCornersToImage;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;
import boofcv.struct.Configuration;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import validate.misc.ParseHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class UtilShapeDetector {

	public static ConfigPolygonDetector configure( boolean fitLines , File file ) {

		int minSides=3,maxSides=6;
		boolean convex = true;
		boolean border = false;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			while( line != null ) {

				String words[] = line.split(" ");
				if( words.length != 2 )
					throw new RuntimeException("Unexpected number of words on line");

				if( words[0].equalsIgnoreCase("convex")) {
					convex = Boolean.parseBoolean(words[1]);
				} else if( words[0].equalsIgnoreCase("min_sides")) {
					minSides = Integer.parseInt(words[1]);
				} else if( words[0].equalsIgnoreCase("max_sides")) {
					maxSides = Integer.parseInt(words[1]);
				} else if( words[0].equalsIgnoreCase("image_border")) {
					border = Boolean.parseBoolean(words[1]);
				}

				line = reader.readLine();
			}
		} catch (FileNotFoundException ignore) {
			// just go with the defaults
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ConfigPolygonDetector config = new ConfigPolygonDetector(3,4,5,6);
		Configuration configRefine = null;
		if( fitLines ) {
			configRefine = new ConfigRefinePolygonLineToImage();
		} else {
			configRefine = new ConfigRefinePolygonCornersToImage();
		}

		int sides[] = new int[maxSides - minSides+1];
		for (int i = 0; i < sides.length; i++) {
			sides[i] = i + minSides;
		}
		config.refine = configRefine;
		config.numberOfSides = sides;
		config.convex = convex;
//			config.border = border; TODO not implemented yet

		return config;
	}

	public static void saveResults( List<Polygon2D_F64> polygons , File file ) {
		try {
			PrintStream out = new PrintStream(file);

			out.println("# Detected polygons in an image");
			out.println("# (Number of corners) (corner X) (corner Y) ...");
			for (int i = 0; i < polygons.size(); i++) {
				Polygon2D_F64 p = polygons.get(i);

				out.print(p.size());
				for (int j = 0; j < p.size(); j++) {
					Point2D_F64 c = p.get(j);
					out.printf(" %f %f", c.x,c.y);
				}
				out.println();
			}

			out.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<PolygonTruthIndexes> loadDescription( File file ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			List<PolygonTruthIndexes> out = new ArrayList<PolygonTruthIndexes>();
			int index = 0;
			while( line != null ) {

				String words[] = line.split(" ");
				if( words.length != 2 )
					throw new RuntimeException("Unexpected number of words on line");

				PolygonTruthIndexes polygon = new PolygonTruthIndexes();
				polygon.name = words[0];

				int numCorners = Integer.parseInt(words[1]);

				polygon.indexes = new int[ numCorners ];
				for (int i = 0; i < numCorners; i++) {
					polygon.indexes[i] = index++;
				}

				out.add( polygon );
				line = reader.readLine();
			}

			return out;

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static List<Polygon2D_F64> loadResults( File file ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = reader.readLine();
			while( line != null && line.length() >= 1 ) {
				if( line.charAt(0) != '#')
					break;
				line = reader.readLine();
			}

			List<Polygon2D_F64> out = new ArrayList<Polygon2D_F64>();
			while( line != null ) {

				String words[] = line.split(" ");
				int size = Integer.parseInt(words[0]);

				Polygon2D_F64 poly = new Polygon2D_F64(size);

				for (int i = 0; i < size; i++) {
					Point2D_F64 p = poly.get(i);
					p.x = Double.parseDouble(words[1 + i*2 ]);
					p.y = Double.parseDouble(words[2 + i*2 ]);
				}
				out.add(poly);
				line = reader.readLine();
			}

			return out;

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String imageToDetectedName( String name ) {
		return name.substring(0,name.length()-4)+"_detected.txt";
	}
}
