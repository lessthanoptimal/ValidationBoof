package validate.misc;

import georegression.struct.point.Point2D_F64;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * For reading and writing simple list of points
 *
 * @author Peter Abeles
 */
public class PointFileCodec {

	public static void save( String path , String header, List<Point2D_F64> points ) {
		File f = new File(path);

		try {
			PrintStream out = new PrintStream(f);

			if( header != null)
				out.println("# "+header);

			for (int i = 0; i < points.size(); i++) {
				Point2D_F64 p = points.get(i);
				out.printf("%.20f %.20f\n",p.x,p.y);
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<Point2D_F64> load( String path ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line = reader.readLine();
			while( line != null && line.length() >= 1 ) {
				if( line.charAt(0) != '#')
					break;
				line = reader.readLine();
			}

			List<Point2D_F64> out = new ArrayList<Point2D_F64>();
			while( line != null ) {

				String words[] = line.split(" ");
				if( words.length != 2 ) {
					throw new RuntimeException("expected two words");
				}

				Point2D_F64 p = new Point2D_F64();
				p.x = Double.parseDouble(words[0]);
				p.y = Double.parseDouble(words[1]);

				out.add(p);
				line = reader.readLine();
			}

			return out;

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
