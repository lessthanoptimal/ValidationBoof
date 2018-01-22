package boofcv.common.misc;

import georegression.struct.se.Se3_F64;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class ParseHelper {

	public static void deleteRecursive( File file ) {
		File[] files = file.listFiles();

		for (int i = 0; i < files.length; i++) {
			File f = files[i];

			if( f.isDirectory() ) {
				deleteRecursive(f);
			} else if( f.isFile() ) {
				if( !f.delete() ) {
					System.err.println("Can't delete "+f.getPath());
				}
			}
		}
		if( !file.delete() ) {
			System.err.println("Can't delete "+file.getPath());
		}
	}

	public static String skipComments( BufferedReader reader ) throws IOException {
		String line = reader.readLine();

		while( line != null && line.charAt(0) == '#') {
			line = reader.readLine();
		}

		return line;
	}

	public static Se3_F64 parseRigidBody( String line , BufferedReader reader ) throws IOException {
		Se3_F64 out = new Se3_F64();

		String words[] = line.split(" ");
		out.R.set(0,0,Double.parseDouble(words[0]));
		out.R.set(0,1,Double.parseDouble(words[1]));
		out.R.set(0,2,Double.parseDouble(words[2]));
		out.T.x = Double.parseDouble(words[3]);

		words = reader.readLine().split(" ");
		out.R.set(1,0,Double.parseDouble(words[0]));
		out.R.set(1,1,Double.parseDouble(words[1]));
		out.R.set(1,2,Double.parseDouble(words[2]));
		out.T.y = Double.parseDouble(words[3]);

		words = reader.readLine().split(" ");
		out.R.set(2,0,Double.parseDouble(words[0]));
		out.R.set(2,1,Double.parseDouble(words[1]));
		out.R.set(2,2,Double.parseDouble(words[2]));
		out.T.z = Double.parseDouble(words[3]);

		return out;
	}
}
