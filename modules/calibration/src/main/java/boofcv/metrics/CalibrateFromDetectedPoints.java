package boofcv.metrics;

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang99;
import boofcv.alg.geo.calibration.cameras.Zhang99CameraBrown;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes calibration parameters using saved detected calibration points
 *
 * @author Peter Abeles
 */
public class CalibrateFromDetectedPoints {

    PrintStream outputResults = System.out;
    PrintStream err = System.err;

    public void processStereo(File stereoDetections, boolean tangential) throws IOException {
        DetectSingleFiducialCalibration targetDesc = FactoryFiducialCalibration.chessboardX(
                null, new ConfigGridDimen(7, 5, 30));
        CalibrationPlanarGridZhang99 zhang99 = new CalibrationPlanarGridZhang99(targetDesc.getLayout(),
                new Zhang99CameraBrown(targetDesc.getLayout(), true, tangential, 2));

        List<CalibrationObservation> left = new ArrayList<>();
        List<CalibrationObservation> right = new ArrayList<>();

        loadObservations(stereoDetections, left, right);

        outputResults.println("=================================================================");
        outputResults.println("FILE: " + stereoDetections);
        outputResults.println("LEFT");
        List<ImageResults> errors = calibrate(zhang99, left);
        printErrors(errors);
        outputResults.println();
        outputResults.println("RIGHT");
        errors = calibrate(zhang99, right);
        printErrors(errors);
    }

    public void setOutputResults(PrintStream outputResults) {
        this.outputResults = outputResults;

    }

    public void setErrorStream(PrintStream err) {
        this.err = err;
    }

    public static void loadObservations(File file,
                                        List<CalibrationObservation> left,
                                        List<CalibrationObservation> right)
            throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line;
        while ((line = reader.readLine()) != null) {
            CalibrationObservation target = new CalibrationObservation(0, 0);

            String[] s = line.split(" ");
            String fileName = s[0];

            int N = Integer.parseInt(s[1]);
            for (int i = 0; i < N; i++) {
                float x = Float.parseFloat(s[i * 2 + 2]);
                float y = Float.parseFloat(s[i * 2 + 3]);
                target.add(new Point2D_F64(x, y), i);
            }

            if (fileName.contains("left"))
                left.add(target);
            else if (fileName.contains("right"))
                right.add(target);
            else
                throw new RuntimeException("Unknown");
        }
    }

    private List<ImageResults> calibrate(CalibrationPlanarGridZhang99 zhang99, List<CalibrationObservation> observations) {
        if (!zhang99.process(observations))
            throw new RuntimeException("Calibration failed!");

        // Get camera parameters and extrinsic target location in each image
        SceneStructureMetric structure = zhang99.structure;

        // Convenient function for converting from specialized Zhang99 format to generalized
        CameraPinholeBrown param = (CameraPinholeBrown) zhang99.getCameraModel();

        // print the results to standard out
//		param.print();

        outputResults.println("# Intrinsic matrix");
        outputResults.printf("%1.15f %1.15f %1.15f %1.15f %1.15f\n", param.fx, param.fy, param.skew, param.cx, param.cy);
        outputResults.println("# Radial Distortion");
        outputResults.printf("%d", param.radial.length);
        for (int i = 0; i < param.radial.length; i++)
            outputResults.printf(" %1.15f", param.radial[i]);
        outputResults.println();
        outputResults.println("# Tangential Distortion");
        outputResults.printf("%1.15f %1.15f\n", param.t1, param.t2);
        outputResults.println(structure.views.size);
        Rodrigues_F64 rod = new Rodrigues_F64();
        for (SceneStructureMetric.View v : structure.views.toList()) {
            Se3_F64 world_to_view = structure.motions.get(v.parent_to_view).motion;
            ConvertRotation3D_F64.matrixToRodrigues(world_to_view.R, rod);
            double rx = rod.unitAxisRotation.x * rod.theta;
            double ry = rod.unitAxisRotation.y * rod.theta;
            double rz = rod.unitAxisRotation.z * rod.theta;

            Vector3D_F64 T = world_to_view.T;

            outputResults.println("# Extrinsic");
            outputResults.printf("%1.15f %1.15f %1.15f %1.15f %1.15f %1.15f\n", rx, ry, rz, T.x, T.y, T.z);
        }
        return zhang99.computeErrors();
    }

    private void printErrors(List<ImageResults> results) {
        outputResults.println();
        outputResults.println("Errors");
        for (int i = 0; i < results.size(); i++) {
            ImageResults r = results.get(i);
            outputResults.printf("[%03d]  mean = %6f max = %6f\n", i, r.meanError, r.maxError);
        }
    }

    public static void main(String[] args) throws IOException {
        CalibrateFromDetectedPoints app = new CalibrateFromDetectedPoints();

        app.processStereo(new File("data/calib/stereo/points/bumblebee2_chess.txt"), false);

    }
}
