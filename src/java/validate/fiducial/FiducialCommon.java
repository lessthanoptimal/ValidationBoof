package validate.fiducial;

import boofcv.struct.calib.IntrinsicParameters;
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

	public static Scenario parseScenario( File file ) {
		Scenario scenario;
		if( file.getPath().contains("binary")) {
			scenario = new ScenarioBinary();
		} else {
			scenario = new ScenarioImage();
		}
		parseScenario(file,scenario);
		return scenario;
	}

	public static void parseScenario(File file, Scenario scenario ) {
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

			scenario.init(widths,names);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public interface Scenario
	{
		void init( List<Double> widths, List<String> names );

		List<String> getNames();

		int[] getKnownIds();

		int nameToID( String name );

		double getWidth( int id );
	}

	public static class ScenarioBinary implements Scenario
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
		public List<String> getNames() {
			return null;
		}

		@Override
		public int[] getKnownIds() {
			return expectedId;
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
			throw new RuntimeException("Unknown ID");
		}
	}

	public static class ScenarioImage implements Scenario
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
		public List<String> getNames() {
			return names;
		}

		@Override
		public int[] getKnownIds() {
			int[] expected = new int[names.size()];
			for (int i = 0; i < names.size(); i++) {
				// the ID for an image is the order in which it was added, so duplicates will have
				// the ID of the first instance
				expected[i] = names.indexOf(names.get(i));
			}
			return expected;
		}

		@Override
		public int nameToID(String name) {
			return names.indexOf(name);
		}

		@Override
		public double getWidth(int id) {
			return widths[id];
		}
	}

	public static class Detected {
		int id;
		Se3_F64 fiducialToCamera;
	}
}
