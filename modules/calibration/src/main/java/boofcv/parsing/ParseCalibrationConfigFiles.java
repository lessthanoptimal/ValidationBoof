package boofcv.parsing;

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.common.misc.ParseHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * @author Peter Abeles
 */
public class ParseCalibrationConfigFiles {
    public static ConfigGridDimen parseGridDimen2(File descriptionFile) {
        int numRows,numCols;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(descriptionFile));
            String line = ParseHelper.skipComments(reader);
            String words[] = line.split(" ");
            numRows = Integer.parseInt(words[0]);
            numCols = Integer.parseInt(words[1]);
            reader.close();

        } catch (Exception e) {
            throw new RuntimeException(descriptionFile.getPath(),e);
        }

        return new ConfigGridDimen(numRows,numCols,1.0);
    }

    public static ConfigGridDimen parseGridDimen3(File descriptionFile) {
        int numRows,numCols;
        double shapeSize;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(descriptionFile));
            String line = ParseHelper.skipComments(reader);
            String words[] = line.split(" ");
            numRows = Integer.parseInt(words[0]);
            numCols = Integer.parseInt(words[1]);
            shapeSize = Double.parseDouble(words[2]);
            reader.close();

        } catch (Exception e) {
            throw new RuntimeException(descriptionFile.getPath(),e);
        }

        return new ConfigGridDimen(numRows,numCols,shapeSize);
    }

    public static ConfigGridDimen parseGridDimen4(File descriptionFile) {
        int numRows,numCols;
        double shapeSize,shapeDistance;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(descriptionFile));
            String line = ParseHelper.skipComments(reader);
            String words[] = line.split(" ");
            numRows = Integer.parseInt(words[0]);
            numCols = Integer.parseInt(words[1]);
            shapeSize = Double.parseDouble(words[2]);
            shapeDistance = Double.parseDouble(words[3]);
            reader.close();

        } catch (Exception e) {
            throw new RuntimeException(descriptionFile.getPath(),e);
        }

        return new ConfigGridDimen(numRows,numCols,shapeSize,shapeDistance);
    }

}
