package validate.shape;

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.factory.shape.ConfigEllipseDetector;
import boofcv.factory.shape.ConfigPolygonDetector;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import validate.misc.ParseHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class UtilShapeDetector {

	public static ConfigPolylineSplitMerge configurePolylineSplitMerge( File file ) {
		int minSides=3,maxSides=6;
		boolean convex = true;
		boolean loop = true;

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
				} else if( words[0].equalsIgnoreCase("loop")) {
					loop = Boolean.parseBoolean(words[1]);
				}

				line = reader.readLine();
			}
		} catch (FileNotFoundException ignore) {
			// just go with the defaults
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ConfigPolylineSplitMerge config = new ConfigPolylineSplitMerge();

		config.minSides = minSides;
		config.maxSides = maxSides;
		config.convex = convex;
//		config.loop = loop;

		return config;
	}

	public static ConfigPolygonDetector configurePolygon(File file ) {

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

		ConfigPolygonDetector config = new ConfigPolygonDetector(3,6);

		config.detector.minimumSides = minSides;
		config.detector.maximumSides = maxSides;
		config.detector.convex = convex;
		config.detector.canTouchBorder = border;

		return config;
	}

	public static ConfigEllipseDetector configureEllipse( boolean subpixel ) {
		ConfigEllipseDetector config = new ConfigEllipseDetector();

		if( !subpixel )
			config.maxIterations = 0;

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

	public static void saveResultsPolyline(List<List<Point2D_I32>> polygons , File file ) {
		try {
			PrintStream out = new PrintStream(file);

			out.println("# Detected polylines in an image");
			out.println("# (Number of corners) (corner X) (corner Y) ...");
			for (int i = 0; i < polygons.size(); i++) {
				List<Point2D_I32> p = polygons.get(i);

				out.print(p.size());
				for (int j = 0; j < p.size(); j++) {
					Point2D_I32 c = p.get(j);
					out.printf(" %d %d", c.x,c.y);
				}
				out.println();
			}

			out.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<List<Point2D_I32>> loadResultsPolyline( File file ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = reader.readLine();
			while( line != null && line.length() >= 1 ) {
				if( line.charAt(0) != '#')
					break;
				line = reader.readLine();
			}

			List<List<Point2D_I32>> out = new ArrayList<>();
			while( line != null ) {

				String words[] = line.split(" ");
				int size = Integer.parseInt(words[0]);

				List<Point2D_I32> poly = new ArrayList<>();

				for (int i = 0; i < size; i++) {
					Point2D_I32 p = new Point2D_I32();
					p.x = Integer.parseInt(words[1 + i*2 ]);
					p.y = Integer.parseInt(words[2 + i*2 ]);
					poly.add(p);
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
