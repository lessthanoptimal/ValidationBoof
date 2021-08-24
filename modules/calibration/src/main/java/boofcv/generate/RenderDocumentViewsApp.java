package boofcv.generate;

import boofcv.alg.distort.motion.MotionBlurOps;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.convolve.GConvolveImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.app.PaperSize;
import boofcv.common.parsing.MarkerDocumentLandmarks;
import boofcv.common.parsing.ParseCalibrationConfigFiles;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.convolve.Kernel2D_F32;
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
    // TODO bright and dark spots

    @Option(name = "-i", aliases = {"--Input"}, usage = "PDF of marker")
    public String inputFile;
    @Option(name = "-o", aliases = {"--Output"}, usage = "Output directory for rendered images.")
    public String destinationDir = ".";
    @Option(name = "-l", aliases = {"--Landmarks"}, usage = "Landmarks file")
    public String landmarksFile;
    @Option(name = "-s", aliases = {"--ScaleCounts"}, usage = "Used to scale up the number of trials in each scenario")
    public int trialCountFactor = 1;
    @Option(name = "--Seed", usage = "Seed for random number generator")
    public long randomSeed = 234;
    @Option(name = "--PixelNoise", usage = "Amount of gaussian noise added to each pixel's intensity")
    double pixelNoiseSigma = 5.0;

    // Units the simulator uses
    Unit units = Unit.METER;
    // Size of the document read in. Determined from the PDF itself
    PaperSize paper = null;

    // nominal distance of the marker used in several scenarios
    double markerZ = 3.0;

    // Amount of blur applied to each image
    double blurSigma = 0.0;

    // storage for blurred image
    GrayF32 blurred = new GrayF32(1, 1);

    PostRenderProcess postRender = (img) -> img;

    Random rand;

    MarkerDocumentLandmarks landmarks;
    Point2D_F64 pixel = new Point2D_F64();

    public void process() {
        System.out.println("Input PDF: " + inputFile);
        System.out.println("Output:    " + destinationDir);

        if (landmarksFile != null) {
            landmarks = ParseCalibrationConfigFiles.parseDocumentLandmarks(new File(landmarksFile));
        }

        rand = new Random(randomSeed);
        GrayF32 markerImage = loadMarkerImage();

        SimulatePlanarWorld simulator = new SimulatePlanarWorld();
        simulator.setBackground(10);
        simulator.enableHighAccuracy();

        renderBrownScenarios(markerImage, simulator);
        renderFisheyeScenarios(markerImage, simulator);

        System.out.println("Done!");
    }

    private void renderBrownScenarios(GrayF32 markerImage, SimulatePlanarWorld simulator) {
        var intrinsic = new CameraPinholeBrown().fsetK(1000, 1000, 0, 800, 600, 1600, 1200);
        simulator.setCamera(intrinsic);
        Se3_F64 markerToWorld = SpecialEuclideanOps_F64.eulerXyz(0, 0, markerZ, 0.02, Math.PI, 0, null);
        simulator.addSurface(markerToWorld, paper.convertWidth(units), markerImage);

        renderLinearMotionBlur(simulator, markerImage);
        renderFadeToBlack(simulator, markerImage);
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

            File outputDir = setupScenarioOutput("fisheye_blur" + blurCount);

            double fisheyeMarkerZ = 0.20;
            double span = Math.PI * 0.7;
            int numTrials = 30 * trialCountFactor;
            for (int indexYaw = 0; indexYaw < numTrials; indexYaw++) {
                simulator.resetScene();

                double yaw = -Math.PI / 2 + (Math.PI - span) / 2.0 + indexYaw * span / (numTrials - 1);
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

    private void renderLinearMotionBlur(SimulatePlanarWorld simulator, GrayF32 markerImage) {
        File outputDir = setupScenarioOutput("linear_blur");

        int numAngles = 6 * trialCountFactor;
        int numMagnitudes = 7 * trialCountFactor;

        GrayF32 workImage = new GrayF32(1, 1);
        ImageBorder<GrayF32> border = FactoryImageBorder.single(BorderType.EXTENDED, GrayF32.class);

        int trial = 0;
        for (int angleidx = 0; angleidx < numAngles; angleidx++) {
            double theta = Math.PI * angleidx / numAngles;
            for (int magIdx = 0; magIdx < numMagnitudes; magIdx++) {
                double magnitude = (1 + magIdx) * 3;

                // Shuffle the target around a little-bit to avoid special due to rendering from dominating
                double tx = rand.nextGaussian() * 0.005;
                double ty = rand.nextGaussian() * 0.005;
                double tz = rand.nextGaussian() * 0.01;

                Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(tx, ty, tz + -markerZ * 0.8, 0, 0, 0, null);
                simulator.setWorldToCamera(worldToCamera);

                Kernel2D_F32 psf = MotionBlurOps.linearMotionPsf(magnitude, theta);

                postRender = (image) -> {
                    workImage.reshape(image.width, image.height);
                    GConvolveImageOps.convolve(psf, image, workImage, border);
                    return workImage;
                };

                saveSimulatedImage(simulator, outputDir, trial++);
            }
        }

        // remove the post process filter
        postRender = (img)-> img;
    }

    private void renderFadeToBlack(SimulatePlanarWorld simulator, GrayF32 markerImage) {
        File outputDir = setupScenarioOutput("fade");

        GrayF32 texture = simulator.getImageRect(0).texture.clone();

        int numTrials = 12 * trialCountFactor;
        for (int i = 0; i < numTrials; i++) {
            // Shuffle the target around a little-bit to avoid special due to rendering from dominating
            double rotX = rand.nextGaussian() * 1e-2;
            double rotY = rand.nextGaussian() * 1e-2;
            double rotZ = rand.nextGaussian() * 1e-1;

            Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0, 0, -markerZ * 0.8, rotX, rotY, rotZ, null);
            simulator.setWorldToCamera(worldToCamera);

            double brightness = (numTrials - i - 1) / (double) (numTrials - 1) + 0.02;
            PixelMath.multiply(texture, (float) brightness, simulator.getImageRect(0).texture);
            saveSimulatedImage(simulator, outputDir, i);
        }

        // Undo the changes so that other scenarios are not messed up by it
        simulator.getImageRect(0).texture.setTo(texture);
    }

    private void renderMovingAway(SimulatePlanarWorld simulator, GrayF32 markerImage) {
        File outputDir = setupScenarioOutput("move_away");

        int N = 40 * trialCountFactor;
        double maxDistanceAway = markerZ * 1.5;
        for (int i = 0; i < N; i++) {
            // Shuffle the target around a little-bit to avoid special due to rendering from dominating
            double rotX = rand.nextGaussian() * 1e-2;
            double rotY = rand.nextGaussian() * 1e-2;
            double rotZ = rand.nextGaussian() * 1e-1;

            double distance = markerZ - 0.3 - maxDistanceAway * i / (N - 1);
            Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0, 0, -distance, rotX, rotY, rotZ, null);
            simulator.setWorldToCamera(worldToCamera);
            saveSimulatedImage(simulator, outputDir, i);
        }
    }

    private void renderRotatingZ(SimulatePlanarWorld simulator, GrayF32 markerImage) {
        File outputDir = setupScenarioOutput("rotate_z");

        int N = 40 * trialCountFactor;
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

        int N = 20 * trialCountFactor;
        double sweep = Math.PI * 0.95;

        for (int blurCount = 0; blurCount < 5; blurCount++) {
            blurSigma = blurCount;

            File outputDir = setupScenarioOutput("axis_blur" + blurCount);


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

        GrayF32 rendered = noise(postRender.process(simulator.render()), pixelNoiseSigma);
        UtilImageIO.saveImage(rendered, new File(outputDir, String.format("image%03d.png", i)).getPath());

        if (landmarks == null)
            return;

        double documentWidth = landmarks.paper.convertWidth(landmarks.units);
        double documentHeight = landmarks.paper.convertHeight(landmarks.units);

        try (PrintStream out = new PrintStream(new File(outputDir, String.format("landmarks_image%03d.txt", i)))) {
            out.println("# True marker landmark locations");
            out.println("image.shape=" + rendered.width + "x" + rendered.height);

            int total = 0;
            for (int landmarkIdx = 0; landmarkIdx < landmarks.landmarks.size; landmarkIdx++) {
                Point2D_F64 p = landmarks.landmarks.get(landmarkIdx);
                double convert = landmarks.units.convert(1.0, units);
                simulator.computePixel(0, convert * (p.x - documentWidth / 2.0), convert * (p.y - documentHeight / 2.0), pixel);
                if (!rendered.isInBounds((int) pixel.x, (int) pixel.y))
                    continue;
                total++;
            }
            // If nothing is visible there are no markers
            if (total == 0) {
                out.println("markers.size=0");
                return;
            }
            out.println("markers.size=1"); // hard coded for 1 marker being visible
            out.println("marker=0");
            out.println("landmarks.size=" + total);
            for (int landmarkIdx = 0; landmarkIdx < landmarks.landmarks.size; landmarkIdx++) {
                Point2D_F64 p = landmarks.landmarks.get(landmarkIdx);
                double convert = landmarks.units.convert(1.0, units);
                simulator.computePixel(0, convert * (p.x - documentWidth / 2.0), convert * (p.y - documentHeight / 2.0), pixel);
                if (!rendered.isInBounds((int) pixel.x, (int) pixel.y))
                    continue;

                out.printf("%d %.8f %.8f\n", landmarkIdx, pixel.x, pixel.y);
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

    private File setupScenarioOutput(String move_away) {
        File outputDir = new File(destinationDir, move_away);

        try {
            // Delete old files just in case the last time it was run there were more generated
            if (outputDir.exists())
                UtilIO.delete(outputDir, f -> true);

            BoofMiscOps.checkTrue(outputDir.mkdirs());
        } catch (IOException ignore) {
        }

        System.out.println("Rendering " + outputDir.getName());
        return outputDir;
    }

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    @FunctionalInterface
    public interface PostRenderProcess {
        GrayF32 process(GrayF32 image);
    }

    public static void main(String[] args) {
        var generator = new RenderDocumentViewsApp();
        var parser = new CmdLineParser(generator);
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
