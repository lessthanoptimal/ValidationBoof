package validate.fiducial;

import georegression.struct.se.Se3_F64;
import validate.misc.ParseHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FiducialCommon {

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
				widths.add(Double.parseDouble(words[0]));
				names.add(words[1]);
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
				expected[i] = i;
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
