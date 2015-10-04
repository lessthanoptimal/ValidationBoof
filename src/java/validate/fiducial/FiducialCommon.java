package validate.fiducial;

import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import validate.misc.ParseHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FiducialCommon {

	public static void saveIntrinsic( IntrinsicParameters intrinsic , File file ) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(file));

			out.printf("%.10f %.10f %.10f\n", intrinsic.fx, intrinsic.skew, intrinsic.cx);
			out.printf("0 %.10f %.10f\n", intrinsic.fy, intrinsic.cy);
			out.printf("0 0 1\n");
			out.printf("%d %d\n",intrinsic.width,intrinsic.height);

			out.close();
		} catch( FileNotFoundException ignore ) {}
	}

	public static IntrinsicParameters parseIntrinsic( File file ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			List<String> words = new ArrayList<String>();
			for (int i = 0; i < 4; i++) {
				String w[] = reader.readLine().split(" ");
				for (int j = 0; j < w.length; j++) {
					words.add(w[j]);
				}
			}
			reader.close();

			IntrinsicParameters out = new IntrinsicParameters();
			out.fx = Double.parseDouble(words.get(0));
			out.skew = Double.parseDouble(words.get(1));
			out.cx = Double.parseDouble(words.get(2));
			out.fy = Double.parseDouble(words.get(4));
			out.cy = Double.parseDouble(words.get(5));
			out.width = Integer.parseInt(words.get(9));
			out.height = Integer.parseInt(words.get(10));
			return out;
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public static List<Detected> parseDetections( File file ) {
		try {
			List<Detected> ret = new ArrayList<Detected>();

			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			while( line != null ) {
				Detected detected = new Detected();
				detected.id = Integer.parseInt(line);
				detected.fiducialToCamera = ParseHelper.parseRigidBody(reader.readLine(),reader);
				line = reader.readLine();
				ret.add(detected);
			}

			return ret;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Library parseScenario( File file ) {
		Library library;
		if( file.getPath().contains("binary")) {
			library = new LibraryBinary();
		} else {
			library = new LibraryImage();
		}
		parseScenario(file, library);
		return library;
	}

	public static List<String> parseVisibleFile( File file ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			List<String> visible = new ArrayList<String>();
			String line = ParseHelper.skipComments(reader);

			while( line != null ) {
				visible.add(line);
				line = reader.readLine();
			}

			reader.close();
			return visible;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void parseScenario(File file, Library library) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			List<Double> widths = new ArrayList<Double>();
			List<String> names = new ArrayList<String>();

			while( line != null ) {
				String words[] = line.split(" ");
				names.add(words[1]);
				widths.add(Double.parseDouble(words[0]));
				line = reader.readLine();
			}

			library.init(widths, names);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<Landmarks> parseLandmarks(File file ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			List<Landmarks> landmarks = new ArrayList<Landmarks>();

			while( line != null ) {
				Landmarks landmark = new Landmarks();
				String words[] = line.split(" ");
				landmark.id = Integer.parseInt(words[0]);

				for (int i = 1; i < words.length; i +=2 ) {
					Point3D_F64 p = new Point3D_F64();
					p.x = Double.parseDouble(words[i]);
					p.y = Double.parseDouble(words[i+1]);
					landmark.points.add(p);
				}

				landmarks.add(landmark);

				line = reader.readLine();
			}

			return landmarks;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Landmarks
	{
		int id;
		List<Point3D_F64> points = new ArrayList<Point3D_F64>();
	}

	public interface Library
	{
		void init( List<Double> widths, List<String> names );

		List<String> getAllNames();

		int nameToID( String name );

		double getWidth( int id );
	}

	public static class LibraryBinary implements Library
	{
		int expectedId[];
		double widths[];

		@Override
		public void init(List<Double> widths, List<String> names) {
			this.widths = new double[widths.size()];
			this.expectedId = new int[widths.size()];

			for (int i = 0; i < widths.size(); i++) {
				this.widths[i] = widths.get(i);
				this.expectedId[i] = Integer.parseInt(names.get(i));
			}
		}

		@Override
		public List<String> getAllNames() {
			return null;
		}

		@Override
		public int nameToID(String name) {
			return Integer.parseInt(name);
		}

		@Override
		public double getWidth(int id) {
			for (int i = 0; i < expectedId.length; i++) {
				if( expectedId[i] == id)
					return widths[i];
			}
			// just return the first one as a default.  In all the current datasets  everything is the same size
			return widths[0];
		}
	}

	public static class LibraryImage implements Library
	{
		List<String> names = new ArrayList<String>();
		double widths[];

		@Override
		public void init(List<Double> widths, List<String> names) {
			this.names = names;
			this.widths = new double[widths.size()];

			for (int i = 0; i < widths.size(); i++) {
				this.widths[i] = widths.get(i);
			}
		}

		@Override
		public List<String> getAllNames() {
			return names;
		}

		@Override
		public int nameToID(String name) {
			return names.indexOf(name);
		}

		@Override
		public double getWidth(int id) {
			if( id >= 0 && id < widths.length )
				return widths[id];
			return widths[0];
		}
	}

	public static class Detected {
		int id;
		Se3_F64 fiducialToCamera;
	}
}
