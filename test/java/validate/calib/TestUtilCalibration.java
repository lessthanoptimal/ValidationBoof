package validate.calib;

import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestUtilCalibration {

	public static final String tempName = "temp.txt";

	@After
	public void cleanup() {
		File f = new File(tempName);
		if( f.exists() ) {
			f.delete();
		}
	}

	@Test
	public void save_load_chessboard() throws IOException {
		DescriptionChessboard expected = new DescriptionChessboard();
		expected.numRows = 7;
		expected.numCols = 5;
		expected.width = 0.03;
		expected.centerX = 0.1;
		expected.centerY = 0.15;
		ValidateUnits units = ValidateUnits.MM;

		UtilCalibration.saveChessboard(new File(tempName),
				expected.numRows,expected.numCols,
				expected.width/units.toMeters,units,
				expected.centerX/units.toMeters,
				expected.centerY/units.toMeters);

		DescriptionChessboard found = UtilCalibration.load(new File(tempName));

		assertEquals(expected.numRows,found.numRows);
		assertEquals(expected.numCols,found.numCols);
		assertEquals(expected.width,found.width,1e-4);
		assertEquals(expected.centerX,found.centerX,1e-4);
		assertEquals(expected.centerY,found.centerY,1e-4);
	}

	@Test
	public void chessboardEdges() {
		DescriptionChessboard desc = new DescriptionChessboard();
		desc.numRows = 3;
		desc.numCols = 2;
		desc.width = 0.03;
		desc.centerX = 1;
		desc.centerY = 2;

		List<LineSegment2D_F64> found = UtilCalibration.chessboardEdges(desc);

		assertEquals(12,found.size());

		double w = desc.width;
		checkLines(0,0,desc,found,0);
		checkLines(w,w,desc,found,4);
		checkLines(0,2*w,desc,found,8);
	}

	private void checkLines( double x0 , double y0 , DescriptionChessboard desc ,
							 List<LineSegment2D_F64> lines , int index )
	{
		double centerX = desc.centerX;
		double centerY = desc.centerY;
		double w = desc.width;

		LineSegment2D_F64 a = lines.get(index);
		LineSegment2D_F64 b = lines.get(index+1);
		LineSegment2D_F64 c = lines.get(index+2);
		LineSegment2D_F64 d = lines.get(index+3);

		comparePoints(a.a,-centerX+x0  ,-centerY+y0);
		comparePoints(a.b,-centerX+x0+w,-centerY+y0);

		comparePoints(b.a,-centerX+x0+w,-centerY+y0);
		comparePoints(b.b,-centerX+x0+w,-centerY+y0+w);

		comparePoints(c.a,-centerX+x0+w,-centerY+y0+w);
		comparePoints(c.b,-centerX+x0  ,-centerY+y0+w);

		comparePoints(d.a,-centerX+x0  ,-centerY+y0+w);
		comparePoints(d.b,-centerX+x0  ,-centerY+y0);
	}

	private void comparePoints(Point2D_F64 p , double x , double y ) {
		assertEquals(x,p.x,1e-8);
		assertEquals(y,p.y,1e-8);
	}

	/**
	 * Sees if a point right next to the return line is inside or outside of a shape
	 */
	@Test
	public void linesToCalibPoints() {

		DescriptionChessboard desc = new DescriptionChessboard();
		desc.numRows = 1;
		desc.numCols = 1;
		desc.width = 0.03;
		desc.centerX = 0;
		desc.centerY = 0;

		List<LineSegment2D_F64> lines = UtilCalibration.chessboardEdges(desc);
		List<CalibValidPoint> points = UtilCalibration.linesToCalibPoints(lines, 10, 0.9);

		Rectangle2D_F64 rect = new Rectangle2D_F64(0,0,desc.width,desc.width);

		double w = 0.001;

		for( CalibValidPoint p : points ) {
			double tx = p.darkVector.x*w;
			double ty = p.darkVector.y*w;

			Point2D_F64 inside  = new Point2D_F64(p.p.x + tx, p.p.y + ty);
			Point2D_F64 outside = new Point2D_F64(p.p.x - tx, p.p.y - ty);

			assertTrue( Intersection2D_F64.contains(rect,inside.x,inside.y));
			assertFalse(Intersection2D_F64.contains(rect,outside.x,outside.y));
		}

	}
}
