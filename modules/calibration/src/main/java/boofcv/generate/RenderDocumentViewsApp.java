package boofcv.generate;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.app.PaperSize;
import boofcv.common.parsing.MarkerDocumentLandmarks;
import boofcv.common.parsing.ParseCalibrationConfigFiles;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

/**
 * Reads a document in as a PDF then renders it as it was viewed in different scenarios. If landmark locations
 * are specified then their apparent location in each image will be saved to disk too.
 *
 * @author Peter Abeles
 */
public class RenderDocumentViewsApp {
    @Option(name = "-i", aliases = {"--Input"}, usage = "PDF of marker")
    public String inputFile;
    @Option(name = "-o", aliases = {"--Output"}, usage = "Output directory for rendered images.")
    public String destinationDir = ".";
    @Option(name = "-l", aliases = {"--Landmarks"}, usage = "Landmarks file")
    public String landmarksFile;

    // Units the simulator uses
    Unit units = Unit.METER;
    // Size of the document read in. Determined from the PDF itself
    PaperSize paper = null;

    // nominal distance of the marker used in several scenarios
    double markerZ = 3.0;

    // Amount of Gaussian noise added to each pixel
    double defaultNoise = 5.0;

    // Amount of blur applied to each image
    double blurSigma = 0.0;

    // storage for blurred image
    GrayF32 blurred = new GrayF32(1, 1);

    Random rand;

    MarkerDocumentLandmarks landmarks;
    Point2D_F64 pixel = new Point2D_F64();

    public void process() {
        if (landmarksFile != null) {
            landmarks = ParseCalibrationConfigFiles.parseDocumentLandmarks(new File(landmarksFile));
        }

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
        simulator.addSurface(markerToWorld, paper.convertWidth(units), markerImage);

        renderMovingAway(simulator, markerImage);
        renderRotatingZ(simulator, markerImage);
        renderBlurredRotatingAxis(simulator, markerImage);
    }

    private void renderFisheyeScenarios(GrayF32 markerImage, SimulatePlanarWorld simulator) {
        simulator.resetScene();

        CameraUniversalOmni intrinsic = new CameraUniversalOmni(2);
        intrinsic.fsetRadial(0.2265, 6.720).fsetMirror(2.8).fsetK(1300, 1300, 0, 700, 700, 1400, 1400);
        simulator.setCamera(intrinsic);
        simulator.setWorldToCamera(new Se3_F64());

        for (int blurCount = 0; blurCount < 3; blurCount++) {
            blurSigma = blurCount;

            File outputDir = new File(destinationDir, "fisheye_blur" + blurCount);
            if (!outputDir.exists())
                BoofMiscOps.checkTrue(outputDir.mkdirs());

            double fisheyeMarkerZ = 0.20;
            double span = Math.PI * 0.7;
            for (int indexYaw = 0; indexYaw < 30; indexYaw++) {
                simulator.resetScene();

                double yaw = -Math.PI / 2 + (Math.PI - span) / 2.0 + indexYaw * span / 29.0;
                Se3_F64 markerToWorld = SpecialEuclideanOps_F64.eulerXyz(fisheyeMarkerZ * Math.sin(yaw), 0, fisheyeMarkerZ * Math.cos(yaw), 0.02, Math.PI + yaw, 0, null);
                simulator.addSurface(markerToWorld, paper.convertWidth(units), markerImage);

                saveSimulatedImage(simulator, outputDir, indexYaw);
            }
        }
    }

    @NotNull
    private GrayF32 loadMarkerImage() {
        try {
            PDDocument document = PDDocument.load(new File(inputFile));
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(0);
            document.close();

            double widthIn = document.getPage(0).getMediaBox().getWidth() / 72.0;
            double heightIn = document.getPage(0).getMediaBox().getHeight() / 72.0;
            paper = new PaperSize(widthIn, heightIn, Unit.INCH);
            System.out.println("Document size " + paper);

            return ConvertBufferedImage.convertFrom(image, (GrayF32) null);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
            throw new RuntimeException(e);
        }
    }

    private void renderMovingAway(SimulatePlanarWorld simulator, GrayF32 markerImage) {
        File outputDir = new File(destinationDir, "move_away");
        if (!outputDir.exists())
            BoofMiscOps.checkTrue(outputDir.mkdirs());

        int N = 30;
        for (int i = 0; i < N; i++) {
            double distance = (N - i - 1) * (markerZ - 0.3) / (N - 1);
            Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0, 0, -distance, 0., 0, 0, null);
            simulator.setWorldToCamera(worldToCamera);
            saveSimulatedImage(simulator, outputDir, i);
        }
    }

    private void renderRotatingZ(SimulatePlanarWorld simulator, GrayF32 markerImage) {
        File outputDir = new File(destinationDir, "rotate_z");
        if (!outputDir.exists())
            BoofMiscOps.checkTrue(outputDir.mkdirs());

        int N = 40;
        for (int i = 0; i < N; i++) {
            double angle = 2.0 * Math.PI * i / N;
            Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0, 0, -(markerZ - 0.5), 0., 0, angle, null);
            simulator.setWorldToCamera(worldToCamera);
            saveSimulatedImage(simulator, outputDir, i);
        }
    }

    private void renderBlurredRotatingAxis(SimulatePlanarWorld simulator, GrayF32 markerImage) {
        Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0, 0, -(markerZ - 0.5), 0., 0, 0, null);
        simulator.setWorldToCamera(worldToCamera);

        int N = 20;
        double sweep = Math.PI * 0.95;

        for (int blurCount = 0; blurCount < 5; blurCount++) {
            blurSigma = blurCount;

            File outputDir = new File(destinationDir, "axis_blur" + blurCount);
            if (!outputDir.exists())
                BoofMiscOps.checkTrue(outputDir.mkdirs());

            for (int i = 0; i < N; i++) {
                simulator.resetScene();

                double angle = -Math.PI / 2.0 + (Math.PI - sweep) / 2.0 + i * sweep / (N - 1);
                Se3_F64 markerToWorld = SpecialEuclideanOps_F64.eulerXyz(0, 0, markerZ, 0.02, Math.PI + angle, 0, null);
                simulator.addSurface(markerToWorld, paper.convertWidth(units), markerImage);

                saveSimulatedImage(simulator, outputDir, i);
            }
        }
    }

    private void saveSimulatedImage(SimulatePlanarWorld simulator, File outputDir, int i) {
        GrayF32 rendered = noise(simulator.render(), defaultNoise);
        UtilImageIO.saveImage(rendered, new File(outputDir, String.format("image%02d.png", i)).getPath());

        if (landmarks == null)
            return;

        double documentWidth = landmarks.paper.convertWidth(landmarks.units);
        double documentHeight = landmarks.paper.convertHeight(landmarks.units);

        try (PrintStream out = new PrintStream(new File(outputDir, String.format("landmarks_image%02d.txt", i)))) {
            out.println("# True marker landmark locations");
            out.println("image.shape=" + rendered.width + "x" + rendered.height);

            int total = 0;
            for (int landmarkIdx = 0; landmarkIdx < landmarks.landmarks.size; landmarkIdx++) {
                Point2D_F64 p = landmarks.landmarks.get(landmarkIdx);
                double convert = landmarks.units.convert(1.0, units);
                simulator.computePixel(0, convert * (p.x - documentWidth / 2), -convert * (p.y - documentHeight / 2), pixel);
                if (!rendered.isInBounds((int) pixel.x, (int) pixel.y))
                    continue;
                total++;
            }
            // If nothing is visible there are no markers
            if (total == 0) {
                out.println("markers=0");
                return;
            }
            out.println("markers=1"); // hard coded for 1 marker being visible
            out.println("marker=0");
            out.println("corners.size=" + total);
            for (int landmarkIdx = 0; landmarkIdx < landmarks.landmarks.size; landmarkIdx++) {
                Point2D_F64 p = landmarks.landmarks.get(landmarkIdx);
                double convert = landmarks.units.convert(1.0, units);
                simulator.computePixel(0, convert * (p.x - documentWidth / 2), convert * (p.y - documentHeight / 2), pixel);
                if (!rendered.isInBounds((int) pixel.x, (int) pixel.y))
                    continue;

                // The 1/2 a pixel to compensate for a bias in the rendering system that hasn't been root caused yet
                // manual inspection show's it's clearly off by about 1/2 a pixel. Possibly caused by interpolation
                // when rendering.
                out.printf("%d %.8f %.8f\n", landmarkIdx, pixel.x+0.5, pixel.y+0.5);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
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