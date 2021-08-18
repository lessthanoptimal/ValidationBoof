package boofcv.parsing;

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.app.PaperSize;
import boofcv.common.misc.ParseHelper;
import boofcv.generate.Unit;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.PointIndex2D_F64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ParseCalibrationConfigFiles {
    public static ConfigGridDimen parseGridDimen2(File descriptionFile) {
        int numRows, numCols;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(descriptionFile));
            String line = ParseHelper.skipComments(reader);
            String words[] = line.split(" ");
            numRows = Integer.parseInt(words[0]);
            numCols = Integer.parseInt(words[1]);
            reader.close();

        } catch (Exception e) {
            throw new RuntimeException(descriptionFile.getPath(), e);
        }

        return new ConfigGridDimen(numRows, numCols, 1.0);
    }

    public static ConfigGridDimen parseGridDimen3(File descriptionFile) {
        int numRows, numCols;
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
            throw new RuntimeException(descriptionFile.getPath(), e);
        }

        return new ConfigGridDimen(numRows, numCols, shapeSize);
    }

    public static ConfigGridDimen parseGridDimen4(File descriptionFile) {
        int numRows, numCols;
        double shapeSize, shapeDistance;
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
            throw new RuntimeException(descriptionFile.getPath(), e);
        }

        return new ConfigGridDimen(numRows, numCols, shapeSize, shapeDistance);
    }

    public static MarkerDocumentLandmarks parseDocumentLandmarks(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = ParseHelper.skipComments(reader);
            PaperSize paper = PaperSize.lookup(line.split("=")[1]);
            line = ParseHelper.skipComments(reader);
            Unit unit = Unit.lookup(line.split("=")[1]);
            line = ParseHelper.skipComments(reader);

            var document = new MarkerDocumentLandmarks(paper, unit);

            int count = Integer.parseInt(line.split("=")[1]);
            for (int i = 0; i < count; i++) {
                String[] words = reader.readLine().split(" ");
                document.landmarks.grow().setTo(Double.parseDouble(words[0]), Double.parseDouble(words[1]));
            }

            return document;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<UniqueMarkerObserved> parseObservedECoCheck(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<UniqueMarkerObserved> found = new ArrayList<>();

            String line = ParseHelper.skipComments(reader);
            BoofMiscOps.checkTrue(line.startsWith("image.shape"));
            int numMarkers = Integer.parseInt(reader.readLine().split("=")[1]);
            for (int markerIdx = 0; markerIdx < numMarkers; markerIdx++) {
                UniqueMarkerObserved marker = new UniqueMarkerObserved();
                marker.markerID = Integer.parseInt(reader.readLine().split("=")[1]);
                BoofMiscOps.checkTrue(reader.readLine().startsWith("decoded_cells"));
                BoofMiscOps.checkTrue(reader.readLine().startsWith("shape="));
                int numCorners = Integer.parseInt(reader.readLine().split("=")[1]);
                for (int cornerIdx = 0; cornerIdx < numCorners; cornerIdx++) {
                    String[] words = reader.readLine().split(" ");
                    PointIndex2D_F64 landmark = marker.landmarks.grow();
                    landmark.index = Integer.parseInt(words[0]);
                    landmark.p.x = Double.parseDouble(words[1]);
                    landmark.p.y = Double.parseDouble(words[2]);
                }
                found.add(marker);
            }

            return found;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<UniqueMarkerObserved> parseUniqueMarkerTruth(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<UniqueMarkerObserved> found = new ArrayList<>();

            String line = ParseHelper.skipComments(reader);
            BoofMiscOps.checkTrue(line.startsWith("image.shape"));
            int numMarkers = Integer.parseInt(reader.readLine().split("=")[1]);
            for (int markerIdx = 0; markerIdx < numMarkers; markerIdx++) {
                UniqueMarkerObserved marker = new UniqueMarkerObserved();
                marker.markerID = Integer.parseInt(reader.readLine().split("=")[1]);
                int numCorners = Integer.parseInt(reader.readLine().split("=")[1]);
                for (int cornerIdx = 0; cornerIdx < numCorners; cornerIdx++) {
                    String[] words = reader.readLine().split(" ");
                    PointIndex2D_F64 landmark = marker.landmarks.grow();
                    landmark.index = Integer.parseInt(words[0]);
                    landmark.p.x = Double.parseDouble(words[1]);
                    landmark.p.y = Double.parseDouble(words[2]);
                }
                found.add(marker);
            }

            return found;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
