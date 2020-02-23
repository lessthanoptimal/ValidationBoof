package boofcv.metrics;

import boofcv.common.misc.ParseHelper;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FiducialCommon {

	public static void saveIntrinsic( CameraPinholeBrown intrinsic , File file ) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(file));

			out.printf("%.10f %.10f %.10f\n", intrinsic.fx, intrinsic.skew, intrinsic.cx);
			out.printf("0 %.10f %.10f\n", intrinsic.fy, intrinsic.cy);
			out.printf("0 0 1\n");
			out.printf("%d %d\n",intrinsic.width,intrinsic.height);

			if( intrinsic.radial != null ) {
				out.print("radial");
				for (int i = 0; i < intrinsic.radial.length; i++) {
					out.print(" "+intrinsic.radial[i]);
				}
				out.println();
			}

			if( intrinsic.t1 != 0 && intrinsic.t2 != 0 ) {
				out.println("tangential " + intrinsic.t1 + " " + intrinsic.t2);
			}

			out.close();
		} catch( FileNotFoundException ignore ) {}
	}

	public static CameraPinholeBrown parseIntrinsic( File file ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			List<String> words = new ArrayList<String>();
			for (int i = 0; i < 4; i++) {
				String w[] = reader.readLine().split(" ");
				for (int j = 0; j < w.length; j++) {
					words.add(w[j]);
				}
			}
			CameraPinholeBrown out = new CameraPinholeBrown();
			out.fx = Double.parseDouble(words.get(0));
			out.skew = Double.parseDouble(words.get(1));
			out.cx = Double.parseDouble(words.get(2));
			out.fy = Double.parseDouble(words.get(4));
			out.cy = Double.parseDouble(words.get(5));
			out.width = Integer.parseInt(words.get(9));
			out.height = Integer.parseInt(words.get(10));


			String line = reader.readLine();
			while( line != null ) {
				String w[] = line.split(" ");
				if( w[0].equalsIgnoreCase("radial")) {
					int N = w.length-1;
					out.radial = new double[N];
					for (int i = 0; i < N; i++) {
						out.radial[i] = Double.parseDouble(w[i+1]);
					}
				} else if( w[0].equalsIgnoreCase("tangential")) {
					out.t1 = Double.parseDouble(w[1]);
					out.t2 = Double.parseDouble(w[2]);
				}
				line = reader.readLine();
			}

			reader.close();
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
		// TODO fix this horrible hack
		if( file.getPath().contains("binary") ||
				file.getPath().contains("chessboard") ||
				file.getPath().contains("uchiya")) {
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

			while( line != null && line.length() != 0) {
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

		long nameToID( String name );

		double getWidth( long id );
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
			List<String> names = new ArrayList<String>();
			for (int i = 0; i < expectedId.length; i++) {
				names.add(""+expectedId[i]);
			}
			return names;
		}

		@Override
		public long nameToID(String name) {
			return Integer.parseInt(name);
		}

		@Override
		public double getWidth(long id) {
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
		public long nameToID(String name) {
			return names.indexOf(name);
		}

		@Override
		public double getWidth(long id) {
			if( id >= 0 && id < widths.length )
				return widths[(int)id];
			return widths[0];
		}
	}

	public static class Detected {
		int id;
		Se3_F64 fiducialToCamera;
	}
}
