package boofcv.metrics.mvs;

import boofcv.abst.disparity.StereoDisparity;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.mvs.BundleToRectificationStereoParameters;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.PointCloudViewerPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.geo.MultiViewIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.*;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.stats.StatisticsDogArray;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

/**
 * Metrics used to see if when given a scan of a surface will the surface be reconstructed consistently
 * independent of small changes in view. This is done by computing disparity along the sequence and
 * overlaying parts that overlap and see if it constructs the same surface.
 *
 * @author Peter Abeles
 */
public class DisparitySequenceConsistency<Image extends ImageGray<Image>> {

    public PrintStream out = System.out;
    public PrintStream err = System.err;

    SceneStructureMetric scene;
    SceneObservations observations;

    BundleToRectificationStereoParameters rectParam = new BundleToRectificationStereoParameters();
    DisparityParameters dispParam = new DisparityParameters();

    StereoDisparity<Image, GrayF32> stereoDisparity;

    Image rectified1;
    Image rectified2;

    DogArray_I32 frames = new DogArray_I32();
    File directory;
    GrayU8 mask = new GrayU8(1, 1);

    Class<Image> imageType;

    DogArray_F32 errors = new DogArray_F32();
    DogArray<PixelError> perrors = new DogArray<>(PixelError::new);

    // How long it took to process each log in milliseconds
    DogArray_F64 timingMS = new DogArray_F64();

    // Storage for
    DogArray_F64 overallErrors95 = new DogArray_F64();

    boolean showVisuals = true;

    public DisparitySequenceConsistency(StereoDisparity<Image, GrayF32> stereoDisparity) {
        this.stereoDisparity = stereoDisparity;
        this.imageType = stereoDisparity.getInputType().getImageClass();
        rectified1 = GeneralizedImageOps.createImage(imageType, 1, 1, 1);
        rectified2 = GeneralizedImageOps.createImage(imageType, 1, 1, 1);

        dispParam.disparityMin = stereoDisparity.getDisparityMin();
        dispParam.disparityRange = stereoDisparity.getDisparityRange();
    }

    public boolean process(File directory) {
        this.directory = directory;
        this.timingMS.reset();

        // Load configurations
        scene = MultiViewIO.load(new File(directory, "structure.yaml").getPath(), (SceneStructureMetric) null);
        observations = MultiViewIO.load(new File(directory, "observations.yaml").getPath(), (SceneObservations) null);
        loadKeyFrames(new File(directory, "keyframes.txt"));

        // Extract images
        extractKeyFramesIfNeeded(directory);

        GrayF32 inverseDepth1 = new GrayF32(1, 1);
        GrayF32 inverseDepth2 = new GrayF32(1, 1);

        // contains visuals when the key frame is the same
        var panelSame = new ListDisplayPanel();
        // Contains visuals when frames are different be viewing the same region
        var panelDiff = new ListDisplayPanel();

        boolean first = true;

        for (int frameIdx = 12; frameIdx < frames.size - 3; frameIdx++) {
            // Same key frame but different 'other' frames
            // This will catch biases and other artifacts when creating a single disparity image
            computeInverseDepth(frameIdx, frameIdx + 1, inverseDepth1);
            computeInverseDepth(frameIdx, frameIdx + 2, inverseDepth2);
            out.printf("Frame: %3d same   ", frameIdx);
            errorMetricsSameFrame(inverseDepth1, inverseDepth2);

            if (first && showVisuals) {
                first = false;
                SwingUtilities.invokeLater(() -> {
                    panelSame.setPreferredSize(new Dimension(inverseDepth1.width, inverseDepth1.height));
                    panelDiff.setPreferredSize(panelSame.getPreferredSize());
                    ShowImages.showWindow(panelSame, "Same Error", true);
                    ShowImages.showWindow(panelDiff, "Mutual Error", true);
                });
            }

            String frameName = "" + frameIdx;

            if (showVisuals)
                SwingUtilities.invokeLater(() -> panelSame.addImage(visualizeError(inverseDepth1.width, inverseDepth1.height), frameName));

            // Two key frames that observe overlapping regions
            // If there's a persistent artifact when the left frame is the same it will show up here.
            // This was inpired by the surface curving
            computeInverseDepth(frameIdx + 1, frameIdx + 3, inverseDepth1);
            out.printf("Frame: %3d mutual ", frameIdx);
            errorMetricsReprojected(frameIdx, inverseDepth2, frameIdx + 1, inverseDepth1);
//            errorMetricsMutualProj(frameIdx + 1, inverseDepth1, frameIdx, inverseDepth2);

            if (showVisuals)
                SwingUtilities.invokeLater(() -> panelDiff.addImage(visualizeError(inverseDepth1.width, inverseDepth1.height), frameName));

//            displayCloud(frameIdx, inverseDepth2);
        }

        out.println();
        double average = StatisticsDogArray.mean(overallErrors95);
        out.printf("Overall: N=%d average_P95=%.4f\n", overallErrors95.size, average);

        return true;
    }

    /**
     * Compare two inverse depth images and see how similar they are. Error will be in distance units.
     */
    private void errorMetricsSameFrame(GrayF32 inverse1, GrayF32 inverse2) {
        errors.reset();
        perrors.reset();
        for (int y = 0; y < inverse1.height; y++) {
            for (int x = 0; x < inverse1.width; x++) {
                float v1 = inverse1.unsafe_get(x, y);
                float v2 = inverse2.unsafe_get(x, y);

                // Skip invalid points and points at infinity
                if (Double.isNaN(v1) || Double.isNaN(v2) || v1 == 0.0 || v2 == 0.0) {
                    continue;
                }
                float error = (1.0f / v2) - (1.0f / v1);
                errors.add(error);
                perrors.grow().setTo(x, y, error);
            }
        }
        printMetrics(inverse1);
    }

    private void printMetrics(GrayF32 inverse1) {
        double overlap = 100.0 * ((double) errors.size / (inverse1.width * inverse1.height));

        float mean = 0.0f;
        for (int i = 0; i < errors.size; i++) {
            mean += errors.get(i);
            errors.set(i, Math.abs(errors.get(i)));
        }
        mean /= errors.size;

        errors.sort();
        out.printf("overlap: %3d%% mean=%6.3f p50=%.3f p95=%.3f p995=%.3f max=%.3f\n",
                (int) overlap, mean,
                errors.getFraction(0.50), errors.getFraction(0.95),
                errors.getFraction(0.995), errors.getFraction(1.00));

        overallErrors95.add(errors.getFraction(0.95));
    }

    private void errorMetricsReprojected(int idx1, GrayF32 inverse1, int idx2, GrayF32 inverse2) {
        var intrinsic = new CameraPinholeBrown();
        BundleAdjustmentOps.convert(scene.getViewCamera(idx1).model, 0, 0, intrinsic);
        var pixelToNorm = new LensDistortionBrown(intrinsic).undistort_F64(true, false);
        var normToPixel = new LensDistortionBrown(intrinsic).distort_F64(false, true);

        Se3_F64 a_to_b = scene.getViewToView(idx1, idx2);

        var norm = new Point2D_F64();
        var point1 = new Point4D_F64();
        var point2 = new Point4D_F64();
        var pixel2 = new Point2D_F64();

        errors.reset();
        for (int y = 0; y < inverse1.height; y++) {
            for (int x = 0; x < inverse1.width; x++) {
                float v1 = inverse1.unsafe_get(x, y);
                if (v1 == 0.0 || Float.isNaN(v1)) {
                    continue;
                }

                // Get point in view-1's reference frame
                pixelToNorm.compute(x, y, norm);
                point1.setTo(norm.x, norm.y, 1.0, v1);

                // Put it into view-2's reference frame
                SePointOps_F64.transform(a_to_b, point1, point2);

                if (PerspectiveOps.isBehindCamera(point2))
                    continue;

                // See which pixel it's on
                normToPixel.compute(point2.x / point2.z, point2.y / point2.z, pixel2);

                int x2 = (int) (pixel2.x + 0.5);
                int y2 = (int) (pixel2.y + 0.5);
                if (!inverse2.isInBounds(x2, y2))
                    continue;

                float v2 = inverse1.unsafe_get(x2, y2);
                if (v2 == 0.0f || Float.isNaN(v2))
                    continue;

                float error = (float) (point2.z / point2.w) - (1.0f / v2);
                errors.add(error);
                perrors.grow().setTo(x, y, error);
            }
        }

        printMetrics(inverse1);
    }

    private BufferedImage visualizeError(int width, int height) {
        GrayF32 errorImage = new GrayF32(width, height);

        float maxAllowedError = 2.0f;

        GImageMiscOps.fill(errorImage, Float.NaN);
        perrors.forEach(e -> {
            // Ensure the error scale is within an allowed value for visualization
            float error = e.error;
            if (Math.abs(error) > maxAllowedError) {
                error *= maxAllowedError / Math.abs(error);
            }
            errorImage.unsafe_set(e.x, e.y, error);
        });

        return VisualizeImageData.colorizeSign(errorImage, null, maxAllowedError);
    }

    private void displayCloud(int imageIdx, GrayF32 inverseDepth) {
        BufferedImage image = loadBufferedImage(imageIdx, directory);
        var intrinsic = new CameraPinholeBrown();
        BundleAdjustmentOps.convert(scene.getViewCamera(imageIdx).model,
                image.getWidth(), image.getHeight(), intrinsic);

        var pixelToNorm = new LensDistortionBrown(intrinsic).undistort_F64(true, false);
        var norm = new Point2D_F64();

        var cloud = new DogArray<>(Point3D_F64::new);
        var colors = new DogArray_I32();

        for (int y = 0; y < inverseDepth.height; y++) {
            for (int x = 0; x < inverseDepth.width; x++) {
                float v = inverseDepth.unsafe_get(x, y);
                // Skip over points at infinity or very far away
                if (Float.isNaN(v) || v <= 1e-8) {
                    continue;
                }

                pixelToNorm.compute(x, y, norm);

                cloud.grow().setTo(norm.x / v, norm.y / v, 1.0 / v);
                colors.add(image.getRGB(x, y));
            }
        }

        SwingUtilities.invokeLater(() -> {
            var viewer = new PointCloudViewerPanel();
            viewer.addCloud64(cloud.toList(), colors);
            viewer.getViewer().setCameraHFov(PerspectiveOps.computeVFov(intrinsic));
            viewer.getViewer().setTranslationStep(0.2);
            viewer.setPreferredSize(new Dimension(intrinsic.width / 2, intrinsic.height / 2));

            ShowImages.showWindow(viewer, "Cloud " + imageIdx);
        });
        BoofMiscOps.sleep(60_000);
    }

    private void computeInverseDepth(int idx1, int idx2, GrayF32 inverseDepth) {
        Image imgA = loadImage(idx1, directory);
        Image imgB = loadImage(idx2, directory);

        long time0 = System.currentTimeMillis();

        Se3_F64 a_to_b = scene.getViewToView(idx1, idx2);

        rectParam.setView1(scene.getViewCamera(idx1).model, imgA.width, imgA.height);
        rectParam.processView2(scene.getViewCamera(idx2).model, imgB.width, imgB.height, a_to_b);

        ImageDistort<Image, Image> distortLeft =
                RectifyDistortImageOps.rectifyImage(rectParam.intrinsic1,
                        rectParam.undist_to_rect1_F32, BorderType.EXTENDED, imgA.getImageType());
        ImageDistort<Image, Image> distortRight =
                RectifyDistortImageOps.rectifyImage(rectParam.intrinsic2,
                        rectParam.undist_to_rect2_F32, BorderType.EXTENDED, imgB.getImageType());

        ImageDimension rectifiedShape = rectParam.rectifiedShape;

        rectified1.reshape(rectifiedShape.width, rectifiedShape.height);
        rectified2.reshape(rectifiedShape.width, rectifiedShape.height);

        distortLeft.apply(imgA, rectified1, mask);
        distortRight.apply(imgB, rectified2);

        stereoDisparity.process(rectified1, rectified2);

        dispParam.baseline = a_to_b.T.norm();
        PerspectiveOps.matrixToPinhole(rectParam.rectifiedK, rectifiedShape.width, rectifiedShape.height, dispParam.pinhole);

        transferDepthToOriginal(stereoDisparity.getDisparity(), inverseDepth);

        timingMS.add(System.currentTimeMillis() - time0);
    }

    /**
     * Take a disparity image and compute a depth image in the original image coordinates.
     */
    private void transferDepthToOriginal(GrayF32 disparity, GrayF32 inverseDepth) {
        final int width = rectParam.intrinsic1.width;
        final int height = rectParam.intrinsic1.height;

        inverseDepth.reshape(width, height);

        var undist1 = new Point2D_F64();
        var rect1 = new Point2D_F64();
        var leftPoint = new Point4D_F64();

        int outsideRect = 0;
        int outsideMask = 0;
        int failedProjection = 0;

        for (int pixelY = 0; pixelY < height; pixelY++) {
            for (int pixelX = 0; pixelX < width; pixelX++) {
                // Remove lens distortion
                rectParam.view1_dist_to_undist.compute(pixelX, pixelY, undist1);

                // Find pixel on rectified image-1
                GeometryMath_F64.mult(rectParam.undist_to_rect1, undist1, rect1);

                // Disparity value at this pixel
                float value = Float.NaN;
                try {
                    int rectPixX = (int) (rect1.x + 0.5);
                    int rectPixY = (int) (rect1.y + 0.5);

                    if (!BoofMiscOps.isInside(disparity, rectPixX, rectPixY)) {
                        outsideRect++;
                        continue;
                    }

                    if (mask.unsafe_get(rectPixX, rectPixY) == 0) {
                        outsideMask++;
                        continue;
                    }

                    // Compute 3D point in view-1 coordinate system
                    float d = disparity.unsafe_get(rectPixX, rectPixY);
                    if (!dispParam.pixelToLeft4D(rect1.x, rect1.y, d, leftPoint)) {
                        failedProjection++;
                        continue;
                    }

                    value = (float) (leftPoint.w / leftPoint.z);
                } finally {
                    inverseDepth.unsafe_set(pixelX, pixelY, value);
                }
            }
        }

//        System.out.printf("rejected: rect=%d mask=%d proj=%d\n", outsideRect, outsideMask, failedProjection);
    }

    private void extractKeyFramesIfNeeded(File directory) {
        var imageDir = new File(directory, "images");
        if (imageDir.exists())
            return;

        UtilIO.mkdirs(imageDir);
        var pathVideo = new File(directory, "video.mp4").getPath();
        SimpleImageSequence<GrayU8> sequence = DefaultMediaManager.INSTANCE.openVideo(pathVideo, ImageType.SB_U8);
        Objects.requireNonNull(sequence);

        int frameIdx = 0;
        while (sequence.hasNext() && frameIdx < frames.size) {
            sequence.next();
            if (sequence.getFrameNumber() != frames.get(frameIdx) + 1)
                continue;

            String pathOutput = new File(imageDir, String.format("frame%04d.png", frameIdx)).getPath();
            UtilImageIO.saveImage((BufferedImage) sequence.getGuiImage(), pathOutput);

            frameIdx++;
        }
    }

    private void loadKeyFrames(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            frames.reset().reserve(lines.size());
            for (String line : lines) {
                if (line.isBlank())
                    continue;
                frames.add(Integer.parseInt(line));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Image loadImage(int frameIdx, File directory) {
        String name = String.format("images/frame%04d.png", frameIdx);
        Image image = UtilImageIO.loadImage(directory.getPath(), name, imageType);
        return Objects.requireNonNull(image, "Image not found");
    }

    private BufferedImage loadBufferedImage(int frameIdx, File directory) {
        String name = String.format("images/frame%04d.png", frameIdx);
        return UtilImageIO.loadImage(new File(directory, name).getPath());
    }

    static class PixelError {
        int x;
        int y;
        float error;

        public PixelError setTo(int x, int y, float error) {
            this.x = x;
            this.y = y;
            this.error = error;

            return this;
        }
    }

    public static void main(String[] args) {
        var config = new ConfigDisparityBMBest5();
        config.disparityRange = 250;
        config.regionRadiusX = config.regionRadiusY = 12;

        StereoDisparity<GrayF32, GrayF32> stereoDisparity = FactoryStereoDisparity.blockMatchBest5(config, GrayF32.class, GrayF32.class);

        String path = "data/mvs/smooth_cement_qr";

        var app = new DisparitySequenceConsistency<>(stereoDisparity);
        app.process(new File(path));
    }
}
