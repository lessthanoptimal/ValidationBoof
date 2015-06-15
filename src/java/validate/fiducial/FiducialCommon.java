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

	public static Scenario parseScenario( File file ) {
		Scenario scenario = new Scenario();
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

	public static class Scenario
	{
		double width;
		int expectedId[];
	}
}
