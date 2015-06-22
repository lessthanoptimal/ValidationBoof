package validate.fiducial;

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

	public static int[] parseExpectedIds( File file , Scenario scenario ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			List<String> lines = new ArrayList<String>();
			while( line != null ) {
				lines.add(line);
				line = reader.readLine();
			}

			int expected[] = new int[ lines.size() ];
			for( int i = 0; i < lines.size(); i++ ) {
				expected[i] = scenario.nameToID(lines.get(i));
			}

			return expected;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Scenario parseScenario( File file ) {
		if( file.getPath().contains("binary")) {
			return parseScenarioBinary(file);
		} else {
			return parseScenarioImage(file);
		}
	}

	public static ScenarioBinary parseScenarioBinary(File file) {
		ScenarioBinary scenario = new ScenarioBinary();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			scenario.width = Double.parseDouble(line);

			List<Integer> ids = new ArrayList<Integer>();
			line = reader.readLine();
			while( line != null ) {
				ids.add( Integer.parseInt(line));
				line = reader.readLine();
			}

			scenario.expectedId = new int[ ids.size() ];
			for (int i = 0; i < ids.size(); i++) {
				int value = ids.get(i);
				scenario.expectedId[i] = value;
			}

			return scenario;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static ScenarioImage parseScenarioImage(File file) {
		ScenarioImage scenario = new ScenarioImage();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			scenario.width = Double.parseDouble(line);

			line = reader.readLine();
			while( line != null ) {
				scenario.names.add(line);
				line = reader.readLine();
			}

			return scenario;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static abstract class Scenario
	{
		double width;

		public abstract int[] getKnownIds();

		public abstract int nameToID( String name );

		public double getWidth() {
			return width;
		}
	}

	public static class ScenarioBinary extends Scenario
	{
		int expectedId[];

		@Override
		public int[] getKnownIds() {
			return expectedId;
		}

		@Override
		public int nameToID(String name) {
			return Integer.parseInt(name);
		}
	}

	public static class ScenarioImage extends Scenario
	{
		List<String> names = new ArrayList<String>();

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
	}
}
