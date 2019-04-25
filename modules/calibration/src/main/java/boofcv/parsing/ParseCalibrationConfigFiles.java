package boofcv.parsing;

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.common.misc.ParseHelper;

import java.io.*;

/**
 * @author Peter Abeles
 */
public class ParseCalibrationConfigFiles {
    public static ConfigGridDimen parseGridDimen3(File descriptionFile) {
        int numRows,numCols;
        double shapeSize,centerDistance;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(descriptionFile));
            String line = ParseHelper.skipComments(reader);
            String words[] = line.split(" ");
            numRows = Integer.parseInt(words[0]);
            numCols = Integer.parseInt(words[1]);
            shapeSize = Double.parseDouble(words[2]);
            reader.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ConfigGridDimen(numRows,numCols,shapeSize);
    }

    public static ConfigGridDimen parseGridDimen4(File descriptionFile) {
        int numRows,numCols;
        double shapeSize,shapeDistance;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(descriptionFile));
            String line = ParseHelper.skipComments(reader);
            String words[] = line.split(" ");
            numRows = Integer.parseInt(words[0]);
            numCols = Integer.parseInt(words[1]);
            shapeSize = Double.parseDouble(words[2]);
            shapeDistance = Double.parseDouble(words[3]);
            reader.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ConfigGridDimen(numRows,numCols,shapeSize,shapeDistance);
    }

}
