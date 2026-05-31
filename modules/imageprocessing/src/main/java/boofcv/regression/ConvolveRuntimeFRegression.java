package boofcv.regression;

import boofcv.alg.filter.convolve.ConvolveImageNormalized;
import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard_IL;
import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard_SB;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FileRegression;
import boofcv.common.RegressionRunner;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

/**
 * Runtime regression for convolution across supported image types, families, kernel shapes, and kernel widths.
 *
 * @author Samik
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ConvolveRuntimeFRegression extends BaseRegression implements FileRegression {
    static final int WIDTH = 320;
    static final int HEIGHT = 240;
    static final long MIN_RUNTIME_NS = 20_000_000L;

    final ImageDataType[] imageTypes = new ImageDataType[]{
            ImageDataType.U8, ImageDataType.U16, ImageDataType.S16,
            ImageDataType.S32, ImageDataType.F32, ImageDataType.F64};
    final ImageType.Family[] families = new ImageType.Family[]{
            ImageType.Family.GRAY, ImageType.Family.INTERLEAVED, ImageType.Family.PLANAR};
    final int[] kernelWidths = new int[]{3, 5, 7, 21};

    PrintStream out;

    public ConvolveRuntimeFRegression() {
        super(BoofRegressionConstants.TYPE_IMAGEPROCESSING);
    }

    @Override
    public void process() throws IOException {
        out = new PrintStream(new File(directoryRuntime, "RUN_ConvolveRuntime.txt"));
        BoofRegressionConstants.printGenerator(out, getClass());
        out.println("# Ops/sec for convolution runtime regression. Higher is better.");
        out.println();
        out.println("ConvolveRuntime");
        out.println("OpsPerSec");

        for (boolean normalized : new boolean[]{false, true}) {
            for (ImageType.Family family : families) {
                for (ImageDataType imageType : imageTypes) {
                    for (Shape shape : Shape.values()) {
                        for (int kernelWidth : kernelWidths) {
                            process(normalized, family, imageType, shape, kernelWidth);
                        }
                    }
                }
            }
        }

        out.close();
    }

    private void process( boolean normalized, ImageType.Family family, ImageDataType imageType,
                          Shape shape, int kernelWidth ) {
        String name = String.format("%s_%s_%s_%s_w%02d",
                normalized ? "normalized" : "standard", familyName(family), imageType, shape.name, kernelWidth);
        System.out.println("processing " + name);

        try {
            ImageBase input = createInput(family, imageType);
            ImageBase output = createOutput(family, imageType, normalized);
            GImageMiscOps.fillUniform(input, new Random(234), 0, 20);
            Object kernel = createKernel(imageType, shape, kernelWidth);
            double opsPerSecond = measure(new Operation() {
                @Override
                public void run() {
                    apply(normalized, family, imageType, shape, input, output, kernel);
                }
            });
            out.printf("%s %.6f%n", name, opsPerSecond);
            out.flush();
        } catch (RuntimeException e) {
            errorLog.println("Failed " + name);
            e.printStackTrace(errorLog);
            throw e;
        }
    }

    private static ImageBase createInput( ImageType.Family family, ImageDataType imageType ) {
        return createImage(family, imageType);
    }

    private static ImageBase createOutput( ImageType.Family family, ImageDataType inputType, boolean normalized ) {
        if (normalized)
            return createImage(family, inputType);

        ImageDataType outputType;
        switch (inputType) {
            case U8:
            case U16:
            case S16:
                outputType = ImageDataType.S16;
                break;

            default:
                outputType = inputType;
                break;
        }
        return createImage(family, outputType);
    }

    private static ImageBase createImage( ImageType.Family family, ImageDataType imageType ) {
        switch (family) {
            case GRAY:
                return ImageType.single(imageType).createImage(WIDTH, HEIGHT);

            case INTERLEAVED:
                return ImageType.il(3, imageType).createImage(WIDTH, HEIGHT);

            case PLANAR:
                return ImageType.pl(3, imageType).createImage(WIDTH, HEIGHT);
        }
        throw new IllegalArgumentException("Unknown image family " + family);
    }

    private static Object createKernel( ImageDataType imageType, Shape shape, int width ) {
        if (shape == Shape.CONVOLVE_2D) {
            if (imageType == ImageDataType.F32) {
                Kernel2D_F32 kernel = new Kernel2D_F32(width);
                Arrays.fill(kernel.data, 1.0f);
                return kernel;
            } else if (imageType == ImageDataType.F64) {
                Kernel2D_F64 kernel = new Kernel2D_F64(width);
                Arrays.fill(kernel.data, 1.0);
                return kernel;
            } else {
                Kernel2D_S32 kernel = new Kernel2D_S32(width);
                Arrays.fill(kernel.data, 1);
                return kernel;
            }
        } else {
            if (imageType == ImageDataType.F32) {
                Kernel1D_F32 kernel = new Kernel1D_F32(width);
                Arrays.fill(kernel.data, 1.0f);
                return kernel;
            } else if (imageType == ImageDataType.F64) {
                Kernel1D_F64 kernel = new Kernel1D_F64(width);
                Arrays.fill(kernel.data, 1.0);
                return kernel;
            } else {
                Kernel1D_S32 kernel = new Kernel1D_S32(width);
                Arrays.fill(kernel.data, 1);
                return kernel;
            }
        }
    }

    private static double measure( Operation operation ) {
        operation.run();

        int N = 1;
        while (true) {
            long start = System.nanoTime();
            for (int i = 0; i < N; i++) {
                operation.run();
            }
            long elapsed = System.nanoTime() - start;
            if (elapsed >= MIN_RUNTIME_NS)
                return N*1e9/elapsed;
            N *= 2;
        }
    }

    private static void apply( boolean normalized, ImageType.Family family, ImageDataType imageType,
                               Shape shape, ImageBase input, ImageBase output, Object kernel ) {
        if (normalized) {
            switch (family) {
                case GRAY:
                    applyNormalizedGray(imageType, shape, (ImageGray)input, (ImageGray)output, kernel);
                    break;

                case INTERLEAVED:
                    applyNormalizedInterleaved(imageType, shape, (ImageInterleaved)input, (ImageInterleaved)output, kernel);
                    break;

                case PLANAR:
                    applyNormalizedPlanar(imageType, shape, (Planar)input, (Planar)output, kernel);
                    break;
            }
        } else {
            switch (family) {
                case GRAY:
                    applyStandardGray(imageType, shape, (ImageGray)input, (ImageGray)output, kernel);
                    break;

                case INTERLEAVED:
                    applyStandardInterleaved(imageType, shape, (ImageInterleaved)input, (ImageInterleaved)output, kernel);
                    break;

                case PLANAR:
                    applyStandardPlanar(imageType, shape, (Planar)input, (Planar)output, kernel);
                    break;
            }
        }
    }

    private static void applyStandardPlanar( ImageDataType imageType, Shape shape, Planar input, Planar output, Object kernel ) {
        for (int band = 0; band < input.getNumBands(); band++) {
            applyStandardGray(imageType, shape, input.getBand(band), output.getBand(band), kernel);
        }
    }

    private static void applyNormalizedPlanar( ImageDataType imageType, Shape shape, Planar input, Planar output, Object kernel ) {
        for (int band = 0; band < input.getNumBands(); band++) {
            applyNormalizedGray(imageType, shape, input.getBand(band), output.getBand(band), kernel);
        }
    }

    private static void applyStandardGray( ImageDataType imageType, Shape shape,
                                           ImageGray input, ImageGray output, Object kernel ) {
        switch (imageType) {
            case U8: {
                GrayU8 in = (GrayU8)input; GrayI16 out = (GrayI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_SB.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_SB.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageStandard_SB.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case U16: {
                GrayU16 in = (GrayU16)input; GrayI16 out = (GrayI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_SB.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_SB.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageStandard_SB.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case S16: {
                GrayS16 in = (GrayS16)input; GrayI16 out = (GrayI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_SB.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_SB.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageStandard_SB.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case S32: {
                GrayS32 in = (GrayS32)input; GrayS32 out = (GrayS32)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_SB.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_SB.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageStandard_SB.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case F32: {
                GrayF32 in = (GrayF32)input; GrayF32 out = (GrayF32)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_SB.horizontal((Kernel1D_F32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_SB.vertical((Kernel1D_F32)kernel, in, out);
                else ConvolveImageStandard_SB.convolve((Kernel2D_F32)kernel, in, out);
                break;
            }
            case F64: {
                GrayF64 in = (GrayF64)input; GrayF64 out = (GrayF64)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_SB.horizontal((Kernel1D_F64)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_SB.vertical((Kernel1D_F64)kernel, in, out);
                else ConvolveImageStandard_SB.convolve((Kernel2D_F64)kernel, in, out);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported image type " + imageType);
        }
    }

    private static void applyStandardInterleaved( ImageDataType imageType, Shape shape,
                                                  ImageInterleaved input, ImageInterleaved output, Object kernel ) {
        switch (imageType) {
            case U8: {
                InterleavedU8 in = (InterleavedU8)input; InterleavedI16 out = (InterleavedI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_IL.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_IL.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageStandard_IL.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case U16: {
                InterleavedU16 in = (InterleavedU16)input; InterleavedI16 out = (InterleavedI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_IL.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_IL.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageStandard_IL.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case S16: {
                InterleavedS16 in = (InterleavedS16)input; InterleavedI16 out = (InterleavedI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_IL.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_IL.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageStandard_IL.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case S32: {
                InterleavedS32 in = (InterleavedS32)input; InterleavedS32 out = (InterleavedS32)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_IL.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_IL.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageStandard_IL.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case F32: {
                InterleavedF32 in = (InterleavedF32)input; InterleavedF32 out = (InterleavedF32)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_IL.horizontal((Kernel1D_F32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_IL.vertical((Kernel1D_F32)kernel, in, out);
                else ConvolveImageStandard_IL.convolve((Kernel2D_F32)kernel, in, out);
                break;
            }
            case F64: {
                InterleavedF64 in = (InterleavedF64)input; InterleavedF64 out = (InterleavedF64)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageStandard_IL.horizontal((Kernel1D_F64)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageStandard_IL.vertical((Kernel1D_F64)kernel, in, out);
                else ConvolveImageStandard_IL.convolve((Kernel2D_F64)kernel, in, out);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported image type " + imageType);
        }
    }

    private static void applyNormalizedGray( ImageDataType imageType, Shape shape,
                                             ImageGray input, ImageGray output, Object kernel ) {
        switch (imageType) {
            case U8: {
                GrayU8 in = (GrayU8)input; GrayI8 out = (GrayI8)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case U16: {
                GrayU16 in = (GrayU16)input; GrayI16 out = (GrayI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case S16: {
                GrayS16 in = (GrayS16)input; GrayI16 out = (GrayI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case S32: {
                GrayS32 in = (GrayS32)input; GrayS32 out = (GrayS32)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case F32: {
                GrayF32 in = (GrayF32)input; GrayF32 out = (GrayF32)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_F32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_F32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_F32)kernel, in, out);
                break;
            }
            case F64: {
                GrayF64 in = (GrayF64)input; GrayF64 out = (GrayF64)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_F64)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_F64)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_F64)kernel, in, out);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported image type " + imageType);
        }
    }

    private static void applyNormalizedInterleaved( ImageDataType imageType, Shape shape,
                                                    ImageInterleaved input, ImageInterleaved output, Object kernel ) {
        switch (imageType) {
            case U8: {
                InterleavedU8 in = (InterleavedU8)input; InterleavedI8 out = (InterleavedI8)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case U16: {
                InterleavedU16 in = (InterleavedU16)input; InterleavedI16 out = (InterleavedI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case S16: {
                InterleavedS16 in = (InterleavedS16)input; InterleavedI16 out = (InterleavedI16)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case S32: {
                InterleavedS32 in = (InterleavedS32)input; InterleavedS32 out = (InterleavedS32)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_S32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_S32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_S32)kernel, in, out);
                break;
            }
            case F32: {
                InterleavedF32 in = (InterleavedF32)input; InterleavedF32 out = (InterleavedF32)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_F32)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_F32)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_F32)kernel, in, out);
                break;
            }
            case F64: {
                InterleavedF64 in = (InterleavedF64)input; InterleavedF64 out = (InterleavedF64)output;
                if (shape == Shape.HORIZONTAL) ConvolveImageNormalized.horizontal((Kernel1D_F64)kernel, in, out);
                else if (shape == Shape.VERTICAL) ConvolveImageNormalized.vertical((Kernel1D_F64)kernel, in, out);
                else ConvolveImageNormalized.convolve((Kernel2D_F64)kernel, in, out);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported image type " + imageType);
        }
    }

    private static String familyName( ImageType.Family family ) {
        if (family == ImageType.Family.GRAY)
            return "SB";
        if (family == ImageType.Family.INTERLEAVED)
            return "IL";
        return "PL";
    }

    public static void main( String[] args ) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{ConvolveRuntimeFRegression.class.getName()});
    }

    interface Operation {
        void run();
    }

    enum Shape {
        HORIZONTAL("horizontal"),
        VERTICAL("vertical"),
        CONVOLVE_2D("convolve2D");

        final String name;

        Shape( String name ) {
            this.name = name;
        }
    }
}
