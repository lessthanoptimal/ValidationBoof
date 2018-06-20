package boofcv.common.misc;

import georegression.struct.curve.EllipseRotated_F64;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static boofcv.common.misc.ParseHelper.skipComments;


/**
 * @author Peter Abeles
 */
public class EllipseFileCodec {
	public static void save( String path , String header, List<EllipseRotated_F64> ellipses ) {
		File f = new File(path);

		try {
			PrintStream out = new PrintStream(f);

			if( header != null)
				out.println("# "+header);
			out.println("# center.x center.y a b phi");
			out.println("# coordinates in pixels and angle in radians");

			for (int i = 0; i < ellipses.size(); i++) {
				EllipseRotated_F64 e = ellipses.get(i);
				out.printf("%.20f %.20f %.20f %.20f %.20f\n",e.center.x,e.center.y,e.a,e.b,e.phi);
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<EllipseRotated_F64> load( String path ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line = skipComments(reader);

			List<EllipseRotated_F64> out = new ArrayList<EllipseRotated_F64>();
			while( line != null ) {

				String words[] = line.split(" ");
				if( words.length != 5 ) {
					throw new RuntimeException("expected fives words");
				}

				EllipseRotated_F64 p = new EllipseRotated_F64();
				p.center.x = Double.parseDouble(words[0]);
				p.center.y = Double.parseDouble(words[1]);
				p.a = Double.parseDouble(words[2]);
				p.b = Double.parseDouble(words[3]);
				p.phi = Double.parseDouble(words[4]);

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
