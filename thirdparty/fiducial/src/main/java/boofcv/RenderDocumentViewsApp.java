package boofcv;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayF32;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Reads a document in as a PDF then renders it as it was viewed in different scenarios.
 *
 * @author Peter Abeles
 */
public class RenderDocumentViewsApp {
    // TODO take in landmark location file
    // TODO save projected landmark location for each rendered file

    @Option(name = "-i", aliases = {"--Input"}, usage = "PDF of marker")
    String inputFile;
    @Option(name = "-o", aliases = {"--Output"}, usage = "Output directory for rendered images.")
    String destinationDir = ".";

    double markerZ = 3.0;

    double markerWidth = .2159;
    double defaultNoise = 5.0;

    double blurSigma = 0.0;

    GrayF32 blurred = new GrayF32(1, 1);

    Random rand;

    public void process() {
        rand = new Random(234);
        GrayF32 markerImage = loadMarkerImage();

        SimulatePlanarWorld simulator = new SimulatePlanarWorld();
        simulator.setBackground(10);

        renderBrownScenarios(markerImage, simulator);
        renderFisheyeScenarios(markerImage, simulator);

        System.out.println("Done!");
    }

    private void renderBrownScenarios(GrayF32 markerImage, SimulatePlanarWorld simulator) {
        var intrinsic = new CameraPinholeBrown().fsetK(1000, 1000, 0, 800, 600, 1600, 1200);
        simulator.setCamera(intrinsic);
        Se3_F64 markerToWorld = SpecialEuclideanOps_F64.eulerXyz(0, 0, markerZ, 0.02, Math.PI, 0, null);
        simulator.addSurface(markerToWorld, markerWidth, markerImage);

        renderMovingAway(simulator);
        renderRotatingZ(simulator);
        renderBlurredRotatingAxis(simulator, markerImage);
    }

    private void renderFisheyeScenarios(GrayF32 markerImage, SimulatePlanarWorld simulator) {
        CameraUniversalOmni intrinsic = new CameraUniversalOmni(2);
        intrinsic.fsetRadial(0.2265, 6.720).fsetMirror(2.8).fsetK(1300, 1300, 0, 700, 700, 1400, 1400);
        simulator.setCamera(intrinsic);

        for (int blurCount = 0; blurCount < 3; blurCount++) {
            blurSigma = blurCount;

            File outputDir = new File(destinationDir, "fisheye_orbit_blur" + blurCount);
            if (!outputDir.exists())
                BoofMiscOps.checkTrue(outputDir.mkdirs());

            double fisheyeMarkerZ = 0.20;
            double span = Math.PI * 0.7;
            for (int indexYaw = 0; indexYaw < 30; indexYaw++) {
                simulator.resetScene();

                double yaw = -Math.PI / 2 + +(Math.PI - span) / 2.0 + indexYaw * span / 29.0;
                Se3_F64 markerToWorld = SpecialEuclideanOps_F64.eulerXyz(fisheyeMarkerZ * Math.sin(yaw), 0, fisheyeMarkerZ * Math.cos(yaw), 0.02, Math.PI + yaw, 0, null);
                simulator.addSurface(markerToWorld, markerWidth, markerImage);

                UtilImageIO.saveImage(noise(simulator.render(), defaultNoise), new File(outputDir, String.format("image%02d.png", indexYaw)).getPath());
            }
        }
    }

    @NotNull
    private GrayF32 loadMarkerImage() {
        try {
            PDDocument document = PDDocument.load(new File(inputFile));
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(0);

            return ConvertBufferedImage.convertFrom(image, (GrayF32) null);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
            throw new RuntimeException(e);
        }
    }

    private void renderMovingAway(SimulatePlanarWorld simulator) {
        File outputDir = new File(destinationDir, "brown_away");
        if (!outputDir.exists())
            BoofMiscOps.checkTrue(outputDir.mkdirs());

        int N = 30;
        for (int i = 0; i < N; i++) {
            double distance = (N - i - 1) * (markerZ - 0.3) / (N - 1);
            Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0, 0, -distance, 0., 0, 0, null);
            simulator.setWorldToCamera(worldToCamera);
            UtilImageIO.saveImage(noise(simulator.render(), defaultNoise), new File(outputDir, String.format("image%02d.png", i)).getPath());
        }
    }

    private void renderRotatingZ(SimulatePlanarWorld simulator) {
        File outputDir = new File(destinationDir, "brown_rotate_z");
        if (!outputDir.exists())
            BoofMiscOps.checkTrue(outputDir.mkdirs());

        int N = 40;
        for (int i = 0; i < N; i++) {
            double angle = 2.0 * Math.PI * i / N;
            Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0, 0, -(markerZ - 0.5), 0., 0, angle, null);
            simulator.setWorldToCamera(worldToCamera);
            UtilImageIO.saveImage(noise(simulator.render(), defaultNoise), new File(outputDir, String.format("image%02d.png", i)).getPath());
        }
    }

    private void renderBlurredRotatingAxis(SimulatePlanarWorld simulator, GrayF32 markerImage) {
        Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0, 0, -(markerZ - 0.5), 0., 0, 0, null);
        simulator.setWorldToCamera(worldToCamera);

        int N = 20;
        double sweep = Math.PI * 0.95;

        for (int blurCount = 0; blurCount < 5; blurCount++) {
            blurSigma = blurCount;

            File outputDir = new File(destinationDir, "brown_rotate_axis_sigma" + blurCount);
            if (!outputDir.exists())
                BoofMiscOps.checkTrue(outputDir.mkdirs());

            for (int i = 0; i < N; i++) {
                simulator.resetScene();

                double angle = -Math.PI / 2.0 + (Math.PI - sweep) / 2.0 + i * sweep / (N - 1);
                Se3_F64 markerToWorld = SpecialEuclideanOps_F64.eulerXyz(0, 0, markerZ, 0.02, Math.PI + angle, 0, null);
                simulator.addSurface(markerToWorld, markerWidth, markerImage);

                UtilImageIO.saveImage(noise(simulator.render(), defaultNoise), new File(outputDir, String.format("image%02d.png", i)).getPath());
            }
        }
    }

    private GrayF32 noise(GrayF32 image, double sigma) {
        if (blurSigma > 0) {
            GBlurImageOps.gaussian(image, blurred, blurSigma, -1, null);
            image.setTo(blurred);
        }
        GImageMiscOps.addGaussian(image, rand, sigma, 0, 255);
        return image;
    }

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    public static void main(String[] args) {
        RenderDocumentViewsApp generator = new RenderDocumentViewsApp();
        CmdLineParser parser = new CmdLineParser(generator);
        try {
            parser.parseArgument(args);
            if (generator.inputFile == null)
                printHelpExit(parser);
            generator.process();
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        }
    }
}
