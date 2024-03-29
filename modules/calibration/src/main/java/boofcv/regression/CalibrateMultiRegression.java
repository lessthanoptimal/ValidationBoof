package boofcv.regression;

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar.CameraStatistics;
import boofcv.abst.geo.calibration.DetectMultiFiducialCalibration;
import boofcv.alg.fiducial.calib.ConfigCalibrationTarget;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.CalibrationObservationSet;
import boofcv.alg.geo.calibration.SynchronizedCalObs;
import boofcv.common.*;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDataType;
import org.ddogleg.struct.DogArray_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Regression for Multi camera calibration. Computes residual error and runtime metrics.
 *
 * @author Peter Abeles
 */
public class CalibrateMultiRegression extends BaseRegression implements ImageRegression {
    String inputPath = "data/calibration_multi";

    PrintStream metricsOut;
    RuntimeSummary runtimeOut;

    public CalibrateMultiRegression() {
        super(BoofRegressionConstants.TYPE_CALIBRATION);
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        // Can only process floating point images
        if (type.isInteger())
            return;

        metricsOut = new PrintStream(new File(directoryMetrics, "ACC_CalibrateMulti.txt"));
        BoofRegressionConstants.printGenerator(metricsOut, getClass());

        runtimeOut = new RuntimeSummary();
        runtimeOut.out = new PrintStream(new File(directoryRuntime, "RUN_CalibrateMulti.txt"));
        BoofRegressionConstants.printGenerator(runtimeOut.out, getClass());
        runtimeOut.out.println("# All times are in milliseconds");
        runtimeOut.out.println();
        runtimeOut.out.println("Default:");

        int totalScenarios = 0;
        var rootDir = new File(inputPath);
        var children = Arrays.asList(Objects.requireNonNull(rootDir.listFiles()));
        Collections.sort(children);
        runtimeOut.printUnitsRow(false);
        for (File child : children) {
            if (!child.isDirectory() || child.isHidden())
                continue;

            if (!new File(child, "calibration_target.yaml").exists())
                continue;

            try {
                evaluateCase(child);
                totalScenarios++;
            } catch (RuntimeException e) {
                e.printStackTrace(errorLog);
                errorLog.println("Exception processing " + child.getName());
            }
        }
        if (totalScenarios == 0) {
            errorLog.println("No scenarios found!");
        }

        metricsOut.close();
        runtimeOut.out.close();
    }

    private void evaluateCase(File dataDir) {
        metricsOut.println("data_set: " + dataDir.getName());
        ConfigCalibrationTarget config = UtilIO.loadConfig(new File(dataDir, "calibration_target.yaml"));

        DetectMultiFiducialCalibration detector;
        if (config.type == CalibrationPatterns.ECOCHECK) {
            detector = FactoryFiducialCalibration.ecocheck(null, config.ecocheck);
        } else {
            throw new RuntimeException("Add support for calibration target " + config.type);
        }

        // Load list of images in for each camera
        var cameras = new ArrayList<List<String>>();
        var listDataFiles = Arrays.asList(Objects.requireNonNull(dataDir.listFiles()));
        Collections.sort(listDataFiles);
        for (var child : listDataFiles) {
            if (!child.isDirectory())
                continue;
            cameras.add(UtilIO.listSmartImages(child.getPath(), true));
        }

        var calibrator = new CalibrateMultiPlanar();
        calibrator.getCalibratorMono().configurePinhole(true, 3, false);
        calibrator.initialize(cameras.size(), 1);
        calibrator.setTargetLayout(0, detector.getLayout(0));

        // Determine image size for each camera
        for (int i = 0; i < cameras.size(); i++) {
            BufferedImage img = UtilImageIO.loadImageNotNull(cameras.get(i).get(0));
            calibrator.setCameraProperties(i, img.getWidth(), img.getHeight());
        }

        // Detect calibration target in each image
        int numImages = cameras.get(0).size();
        var timesMS = new DogArray_F64();
        for (int imageIdx = 0; imageIdx < numImages; imageIdx++) {
            var frameObs = new SynchronizedCalObs();
            long time0 = System.nanoTime();
            for (int camIdx = 0; camIdx < cameras.size(); camIdx++) {
                GrayF32 image = UtilImageIO.loadImage(cameras.get(camIdx).get(imageIdx), GrayF32.class);
                detector.process(image);
                CalibrationObservationSet set = frameObs.cameras.grow();
                set.cameraID = camIdx;
                for (int i = 0; i < detector.getDetectionCount(); i++) {
                    CalibrationObservation obs = detector.getDetectedPoints(i);
                    if (obs.target != 0)
                        continue;
                    set.targets.grow().setTo(obs);
                    break;
                }
            }
            long time1 = System.nanoTime();
            timesMS.add((time1 - time0) * 1e-6);
            calibrator.addObservation(frameObs);
        }

        runtimeOut.printStatsRow(dataDir.getName()+"_detect",timesMS);
        long timeNano = BoofMiscOps.timeNano(
                ()->BoofMiscOps.checkTrue(calibrator.process(), "Calibration Failed!"));
        timesMS.reset().add(timeNano*1e-6);
        runtimeOut.printStatsRow(dataDir.getName()+"_calib",timesMS);


        List<CameraStatistics> stats = calibrator.getStatistics().toList();
        for (int camIdx = 0; camIdx < stats.size(); camIdx++) {
            CameraStatistics cam = stats.get(camIdx);
            metricsOut.printf("  [%3d] mean=%.2f max=%.2f\n", camIdx, cam.overallMean, cam.overallMax);
        }
        metricsOut.println();


    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{CalibrateMultiRegression.class.getName(), ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{CalibrateMultiRegression.class.getName(), ImageDataType.U8.toString()});
    }
}
