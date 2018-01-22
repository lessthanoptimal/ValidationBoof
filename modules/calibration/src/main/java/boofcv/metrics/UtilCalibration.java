package boofcv.metrics;

import georegression.struct.line.LineSegment2D_F64;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class UtilCalibration {

	public static void saveChessboardBoofcv( File file , int rows , int cols , double width )
	{
		double centerY = rows*width/2.0;
		double centerX = cols*width/2.0;

		saveChessboard(file,rows,cols,width,ValidateUnits.MM,centerX,centerY);
	}

	public static void saveChessboard( File file , int rows , int cols , double width , ValidateUnits units, double centerX , double centerY )
	{
		try {
			PrintStream output = new PrintStream(file);

			output.print("# Describes the chessboard calibration target\n" +
					"# The origin is specified relative to the bottom left corner.\n" +
					"# origin at center would be at this coordinate = [ numCols*width/2,numRows*width/2 ] as a number not symbolic\n" +
					"# (num rows) (num cols) (square width) (units) (origin x) (origin y)\n");

			output.printf("%d %d %f %s %f %f",rows,cols,width,units.getShortName(),centerX,centerY);

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static DescriptionChessboard load( File file ) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line = reader.readLine();
		while( line.charAt(0) == '#') {
			line = reader.readLine();
		}

		DescriptionChessboard ret = new DescriptionChessboard();
		String words[] = line.split(" ");

		if( words.length != 6 ) {
			throw new IOException("Unexpected number of words. exepected 6 found "+words.length);
		}

		ret.numRows = Integer.parseInt(words[0]);
		ret.numCols = Integer.parseInt(words[1]);
		ret.width = Double.parseDouble(words[2]);
		ValidateUnits units = ValidateUnits.lookup(words[3]);
		ret.centerX = Double.parseDouble(words[4]);
		ret.centerY = Double.parseDouble(words[5]);

		if( units == null )
			throw new IOException("Unknown units "+words[3]);

		ret.width   *= units.toMeters;
		ret.centerX *= units.toMeters;
		ret.centerY *= units.toMeters;

		return ret;
	}

	public static List<LineSegment2D_F64> chessboardEdges( DescriptionChessboard target )
	{
		List<LineSegment2D_F64> ret = new ArrayList<LineSegment2D_F64>();

		for (int row = 0; row < target.numRows; row++) {
			double y0 = target.width*row;
			double y1 = y0 + target.width;
			for (int col = row%2; col < target.numCols; col += 2) {
				double x0 = target.width*col;
				double x1 = x0 + target.width;

				ret.add( new LineSegment2D_F64(x0,y0,x1,y0));
				ret.add( new LineSegment2D_F64(x1,y0,x1,y1));
				ret.add( new LineSegment2D_F64(x1,y1,x0,y1));
				ret.add( new LineSegment2D_F64(x0,y1,x0,y0));
			}
		}

		// adjust for the center
		for( LineSegment2D_F64 s : ret ) {
			s.a.x -= target.centerX;
			s.a.y -= target.centerY;
			s.b.x -= target.centerX;
			s.b.y -= target.centerY;
		}

		return ret;
	}

	/**
	 * Converts a set of line segments into validation points.  It is assumed that each line
	 * traces along a shape in a clock-wise direction.  Therefor the darkest part will be to
	 * the line's right.  Sample points are not placed on edges since those tend to resolve poorly
	 * on the image and the orientation becomes unclear, when looking at each line one at a time
	 *
	 */
	public static List<CalibValidPoint> linesToCalibPoints(List<LineSegment2D_F64> lines,
														   int numSegments, double lengthFraction)
	{
		List<CalibValidPoint> ret = new ArrayList<CalibValidPoint>();

		double offset = (1.0-lengthFraction)/2.0;

		for( LineSegment2D_F64 line : lines ) {
			double slopeX = line.slopeX();
			double slopeY = line.slopeY();

			// sample the inner portion of the line, avoiding the corners
			double x0 = line.a.x + slopeX*offset;
			double y0 = line.a.y + slopeY*offset;
			double x1 = line.b.x - slopeX*offset;
			double y1 = line.b.y - slopeY*offset;

			double tanX = -slopeY;
			double tanY = slopeX;

			double r = Math.sqrt(tanX*tanX + tanY*tanY);
			tanX /= r;
			tanY /= r;

			for (int i = 0; i < numSegments; i++) {
				double x = x0 + i*(x1-x0)/(numSegments-1);
				double y = y0 + i*(y1-y0)/(numSegments-1);

				ret.add( new CalibValidPoint(x,y,tanX,tanY));
			}
		}

		return ret;
	}
}
