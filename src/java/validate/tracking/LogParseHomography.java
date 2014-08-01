package validate.tracking;

import georegression.struct.homography.Homography2D_F64;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class LogParseHomography {

	public static List<Homography2D_F64> parse( String fileName ) throws FileNotFoundException {

		BufferedReader reader = new BufferedReader(new FileReader(fileName));

		List<Homography2D_F64> ret = new ArrayList<Homography2D_F64>();

		try {
			String line = reader.readLine();
			while( line != null ) {
				if( line.charAt(0) != '#') {
					String numbers[] = line.split("\\s");

					List<Double> found = new ArrayList<Double>();

					for( String s : numbers ) {
						if( s.length() != 0 ) {
							found.add( Double.parseDouble(s));
						}
					}

					if( found.size() != 9 )
						throw new RuntimeException("Unexpected size");

					Homography2D_F64 H = new Homography2D_F64();
					H.a11 = found.get(0);
					H.a12 = found.get(1);
					H.a13 = found.get(2);
					H.a21 = found.get(3);
					H.a22 = found.get(4);
					H.a23 = found.get(5);
					H.a31 = found.get(6);
					H.a32 = found.get(7);
					H.a33 = found.get(8);

					ret.add(H);
				}


				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return ret;
	}

}
