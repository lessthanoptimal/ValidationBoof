package boofcv.parsing;

import boofcv.abst.fiducial.calib.ConfigCircleHexagonalGrid;
import boofcv.common.misc.ParseHelper;

import java.io.*;

/**
 * @author Peter Abeles
 */
public class ParseCalibrationConfigFiles {
    public static ConfigCircleHexagonalGrid parseCircleHexagonalConfig(File descriptionFile) {
        int numRows,numCols;
        double diameter,centerDistance;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(descriptionFile));
            String line = ParseHelper.skipComments(reader);
            String words[] = line.split(" ");
            numRows = Integer.parseInt(words[0]);
            numCols = Integer.parseInt(words[1]);
            diameter = Double.parseDouble(words[2]);
            centerDistance = Double.parseDouble(words[3]);
            reader.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ConfigCircleHexagonalGrid(numRows,numCols,diameter,centerDistance);
    }

}
