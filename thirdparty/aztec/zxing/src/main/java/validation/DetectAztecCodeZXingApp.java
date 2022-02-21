package validation;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.aztec.AztecReader;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Saves a list of detected Aztec Codes
 *
 * @author Peter Abeles
 */
public class DetectAztecCodeZXingApp {
    @Option(name = "-i", aliases = {"--Input"}, usage = "Input directory.")
    String inputDir;
    @Option(name = "-o", aliases = {"--Output"}, usage = "Output directory.")
    String outputDir = ".";
    @Option(name = "-s", aliases = {"--Suffix"}, usage = "File suffix. Case insensitive.")
    String suffix = "jpg";

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    private static void failExit(String message) {
        System.err.println(message);
        System.exit(1);
    }

    public static void save(Result[] results, double milliseconds, File outputDir, String outputName) throws FileNotFoundException {
        PrintStream out = new PrintStream(new File(outputDir, outputName));

        out.println("# ZXing Aztec Code Detections " + outputName);
        out.printf("milliseconds = %.4f\n", milliseconds);

        for (int i = 0; results != null && i < results.length; i++) {
            Result r = results[i];
            if (r.getBarcodeFormat() != BarcodeFormat.AZTEC)
                continue;
            ResultPoint[] pts = r.getResultPoints();

            String message = r.getText().replaceAll("\\p{C}", "?");
            out.println("message = " + message);

            for (int j = 0; j < pts.length; j++) {
                out.printf("%f %f", pts[j].getX(), pts[j].getY());
                if (j == pts.length-1)
                    out.println();
                else
                    out.print(" ");
            }
        }
        int numResults = results == null ? 0 : results.length;
        if (numResults < 10)
            System.out.print(numResults);
        else
            System.out.print("*");
        out.close();
    }

    public static void main(String[] args) {
        // Hints are ignored by the reader
        Map<DecodeHintType, ?> hints = null;

        AztecReader reader = new AztecReader();

        DetectAztecCodeZXingApp generator = new DetectAztecCodeZXingApp();
        CmdLineParser parser = new CmdLineParser(generator);
        try {
            parser.parseArgument(args);
            if (generator.inputDir == null)
                printHelpExit(parser);

            File outputDir = new File(generator.outputDir);

            List<File> children = Arrays.asList(new File(generator.inputDir).listFiles());
            Collections.sort(children);
            for (File f : children) {
                if (f.isDirectory()) {
                    processDirectory(hints, reader, f, new File(outputDir, f.getName()), generator.suffix);
                }
            }

            processDirectory(hints, reader, new File(generator.inputDir), outputDir, generator.suffix);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processDirectory(Map<DecodeHintType, ?> hints, AztecReader reader,
                                         File inputDir, File outputDir, String suffix) throws IOException {
        if (!inputDir.isDirectory())
            failExit("Directory is not a directory. " + inputDir);

        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                failExit("Can't create directory " + outputDir.getPath());
            }
        }

        File[] _files = inputDir.listFiles();
        if (_files == null) {
            failExit("Files is null");
            return;
        }

        List<File> files = Arrays.asList(_files);
        Collections.sort(files);

        int count = 0;
        for (File f : files) {
            if (!f.isFile() || f.isHidden())
                continue;

            boolean type_image = f.getName().toLowerCase().endsWith(suffix) || f.getName().toLowerCase().endsWith("png");

            if (!type_image)
                continue;

            BufferedImage image = ImageIO.read(f);
            if (image == null) {
                failExit("Can't load image " + f.getName());
                continue;
            }

            LuminanceSource source = new BufferedImageLuminanceSource(image);

            // Don't include converting to gray scale because opencv code does that
            // when loading and can't be separated
            long time0 = System.nanoTime();
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result[] results = null;
            try {
                // When inspecting the source code it appears to only be able to detect a single marker
                Result result = reader.decode(bitmap, hints);
                results = new Result[]{result};
            } catch (FormatException e) {

            } catch (NotFoundException e) {
                // fall thru, it means there is no Marker in the image
            }
            long time1 = System.nanoTime();
            double milliseconds = (time1 - time0) * 1e-6;

            String name = FilenameUtils.removeExtension(f.getName()) + ".txt";
            save(results, milliseconds, outputDir, name);
            if (++count % 80 == 0)
                System.out.println();
        }
    }
}