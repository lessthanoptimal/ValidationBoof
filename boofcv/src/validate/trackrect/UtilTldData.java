package validate.trackrect;

import georegression.metric.Intersection2D_F64;
import georegression.struct.shapes.RectangleCorner2D_F64;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class UtilTldData {

	public static double computeFMeasure( TldResults stats ) {

		double precision = stats.truePositives/(double)(stats.truePositives + stats.falsePositives);
		double recall = stats.truePositives/(double)(stats.truePositives + stats.falseNegatives);

		return 2.0*(precision*recall)/(precision + recall);
	}

	public static void updateStatistics( RectangleCorner2D_F64 expected , RectangleCorner2D_F64 found ,
										 FooResults stats ) {
		boolean isVisibleTruth = !Double.isNaN(expected.x0);
		boolean isVisibleFound = !Double.isNaN(found.x0);

		if( isVisibleTruth && isVisibleFound ) {


			RectangleCorner2D_F64 i = new RectangleCorner2D_F64();
			if( Intersection2D_F64.intersection(expected, found, i) ) {
				double areaI = i.area();

				double bottom = expected.area() + found.area() - areaI;

				double overlap = areaI/ bottom;

				stats.totalOverlap += overlap;
				stats.truePositive++;
			}
		}
		stats.total++;
	}


	public static void updateStatistics( RectangleCorner2D_F64 expected , RectangleCorner2D_F64 found ,
										 TldResults stats ) {
		boolean isVisibleTruth = !Double.isNaN(expected.x0);
		boolean isVisibleFound = !Double.isNaN(found.x0);

		if( isVisibleTruth && isVisibleFound ) {

			RectangleCorner2D_F64 i = new RectangleCorner2D_F64();
			if( !Intersection2D_F64.intersection(expected, found, i) ) {
				stats.falsePositives++;
			} else {
				double areaI = i.area();

				double bottom = expected.area() + found.area() - areaI;

				double overlap = areaI/ bottom;

//				System.out.println("overlap "+overlap);
				if( overlap > 0.25 ) {
					stats.truePositives++;
				} else {
					stats.falsePositives++;
				}
			}
		} else if( isVisibleTruth && !isVisibleFound ) {
			stats.falseNegatives++;
		} else if( !isVisibleTruth && isVisibleFound ) {
			stats.falsePositives++;
		} else if( !isVisibleTruth && !isVisibleFound ) {
			stats.trueNegatives++;
		}
	}

	public static void drawRectangle( RectangleCorner2D_F64 rect , Graphics2D g2 ) {
		int w = (int)rect.getWidth();
		int h = (int)rect.getHeight();

		g2.drawRect((int)rect.x0,(int)rect.y0,w,h);
	}

	public static RectangleCorner2D_F64 parseRectangle( String fileName ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));

			RectangleCorner2D_F64 ret = new RectangleCorner2D_F64();
			parseRect( reader.readLine() , ret );

			reader.close();

			return ret;

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void parseRect( String s , RectangleCorner2D_F64 rectangle ) {
		String tokens[] = s.split(",");

		if( tokens.length != 4 )
			throw new RuntimeException("Unexpected number of tokens in rectangle file");

		for( int i = 0; i < 4; i++ ) {
			if(tokens[i].compareTo("nan") == 0 )
				tokens[i] = "NaN";
		}

		rectangle.x0 = Double.parseDouble(tokens[0]);
		rectangle.y0 = Double.parseDouble(tokens[1]);
		rectangle.x1 = Double.parseDouble(tokens[2]);
		rectangle.y1 = Double.parseDouble(tokens[3]);
	}

	public static void parseFRect( String s , RectangleCorner2D_F64 rectangle ) {
		String tokens[] = s.split(",");

		if( tokens.length != 5 )
			throw new RuntimeException("Unexpected number of tokens in rectangle file");

		for( int i = 0; i < 4; i++ ) {
			if(tokens[i+1].compareTo("nan") == 0 )
				tokens[i+1] = "NaN";
		}

		rectangle.x0 = Double.parseDouble(tokens[1]);
		rectangle.y0 = Double.parseDouble(tokens[2]);
		rectangle.x1 = Double.parseDouble(tokens[3]);
		rectangle.y1 = Double.parseDouble(tokens[4]);
	}

	public static void parseRectWH( String s , RectangleCorner2D_F64 rectangle ) {
		String tokens[] = s.split(",");

		if( tokens.length != 4 )
			throw new RuntimeException("Unexpected number of tokens in rectangle file");

		for( int i = 0; i < 4; i++ ) {
			if(tokens[i].compareTo("nan") == 0 )
				tokens[i] = "NaN";
		}

		rectangle.x0 = Double.parseDouble(tokens[0]);
		rectangle.y0 = Double.parseDouble(tokens[1]);
		rectangle.x1 = rectangle.x0 + Double.parseDouble(tokens[2]);
		rectangle.y1 = rectangle.y0 + Double.parseDouble(tokens[3]);
	}
}
