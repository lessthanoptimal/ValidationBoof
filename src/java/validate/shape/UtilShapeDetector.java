package validate.shape;

import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class UtilShapeDetector {

	/**
	 * Polygon detector which fits the entire line
	 */
	public static <T extends ImageSingleBand>
	BinaryPolygonConvexDetector<T> createPolygonLine( Class<T> imageType ) {
		ConfigPolygonDetector config = new ConfigPolygonDetector(3,4,5,6);
		config.refineWithCorners = false;
		config.refineWithLines = true;

		return FactoryShapeDetector.polygon(config,imageType);
	}

	/**
	 * Polygon detector which fits only around the corners
	 */
	public static <T extends ImageSingleBand>
	BinaryPolygonConvexDetector<T> createPolygonCorner( Class<T> imageType ) {
		ConfigPolygonDetector config = new ConfigPolygonDetector(3,4,5,6);
		config.refineWithCorners = true;
		config.refineWithLines = false;

		return FactoryShapeDetector.polygon(config,imageType);
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

			String line = reader.readLine();
			while( line != null && line.length() >= 1 ) {
				if( line.charAt(0) != '#')
					break;
				line = reader.readLine();
			}

			List<PolygonTruthIndexes> out = new ArrayList<PolygonTruthIndexes>();
			while( line != null ) {

				String words[] = line.split(" ");

				PolygonTruthIndexes polygon = new PolygonTruthIndexes();
				polygon.name = words[0];
				polygon.indexes = new int[ words.length - 1 ];
				for (int i = 0; i < polygon.indexes.length; i++) {
					polygon.indexes[i] = Integer.parseInt(words[i+1]);
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
