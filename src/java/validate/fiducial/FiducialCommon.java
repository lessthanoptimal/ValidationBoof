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

	public static int[] parseExpectedIds(File file) {
		ScenarioBinary scenario = new ScenarioBinary();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			List<String> lines = new ArrayList<String>();
			while( line != null ) {
				lines.add(line);
				line = reader.readLine();
			}

			int[] ret = new int[ lines.size() ];
			for (int i = 0; i < lines.size(); i++) {
				ret[i] = Integer.parseInt(lines.get(i));
			}
			return ret;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static double parseWidth(File file) {
		ScenarioBinary scenario = new ScenarioBinary();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = ParseHelper.skipComments(reader);

			return Double.parseDouble(line);
		} catch (IOException e) {
			throw new RuntimeException(e);
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

	public static class ScenarioBinary
	{
		double width;
		int expectedId[];
	}

	public static class ScenarioImage
	{
		double width;
		List<String> names = new ArrayList<String>();
	}
}
