package validation;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
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
 * Saves a list of detected qr codes
 *
 * @author Peter Abeles
 */
public class DetectQrCodeZXingApp {
    @Option(name="-i",aliases = {"--Input"}, usage="Input directory.")
    String inputDir;
    @Option(name="-o",aliases = {"--Output"}, usage="Output directory.")
    String outputDir=".";
    @Option(name="-s",aliases = {"--Suffix"}, usage="File suffix. Case insensitive.")
    String suffix="jpg";

    private static void printHelpExit( CmdLineParser parser ) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    private static void failExit( String message ) {
        System.err.println(message);
        System.exit(1);
    }

    public static void save(Result[] results , File outputDir , String outputName ) throws FileNotFoundException {
        PrintStream out = new PrintStream(new File(outputDir,outputName));

        out.println("# ZXing QR-Code Detections "+outputName);
        for (int i = 0; results != null && i < results.length; i++) {
            Result r = results[i];
            if( r.getBarcodeFormat() != BarcodeFormat.QR_CODE )
                continue;
            ResultPoint[] pts = r.getResultPoints();

            String message = r.getText().replaceAll("\\p{C}", "?");
            out.println("message = "+message);
            // ZXing doesn't provide the corners of each position pattern
            // let's provide an approximate bounding box instead

            float x3 = pts[2].getX() + (pts[0].getX()-pts[1].getX());
            float y3 = pts[2].getY() + (pts[0].getY()-pts[1].getY());

            for (int j = 0; j < 3; j++) {
                out.printf("%f %f ",pts[j].getX(),pts[j].getY());
            }
            out.printf("%f %f\n",x3,y3);
        }
        int numResults = results == null ? 0 : results.length;
        if( numResults < 10 )
            System.out.print(numResults);
        else
            System.out.print("*");
        out.close();
    }

    public static void main(String[] args) {

        Map<DecodeHintType,?> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER,null);

        QRCodeMultiReader reader = new QRCodeMultiReader();

        DetectQrCodeZXingApp generator = new DetectQrCodeZXingApp();
        CmdLineParser parser = new CmdLineParser(generator);
        try {
            parser.parseArgument(args);
            if( generator.inputDir == null )
                printHelpExit(parser);

            File outputDir = new File(generator.outputDir);

            for( File f : new File(generator.inputDir).listFiles() ) {
                if( f.isDirectory() ) {
                    processDirectory(hints,reader,f,new File(outputDir,f.getName()),generator.suffix);
                }
            }

            processDirectory(hints, reader,new File(generator.inputDir),outputDir,generator.suffix);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processDirectory(Map<DecodeHintType, ?> hints, QRCodeMultiReader reader,
                                         File inputDir, File outputDir , String suffix ) throws IOException {
        if( !inputDir.isDirectory())
            failExit("Directory is not a directory. "+inputDir);

        if( !outputDir.exists() ) {
            if( !outputDir.mkdirs() ) {
                failExit("Can't create directory "+outputDir.getPath());
            }
        }

        File[] _files = inputDir.listFiles();
        if( _files == null ) {
            failExit("Files is null");
            return;
        }

        List<File> files = Arrays.asList(_files);
        Collections.sort(files);

        int count = 0;
        for( File f : files ) {
            if( !f.isFile() || f.isHidden() )
                continue;

            boolean type_image = f.getName().toLowerCase().endsWith(suffix) || f.getName().toLowerCase().endsWith("png");

            if( !type_image )
                continue;

            BufferedImage image = ImageIO.read(f);
            if( image == null ) {
                failExit("Can't load image "+f.getName());
            }

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result[] results = null;
            try {
                results = reader.decodeMultiple(bitmap,hints);
            } catch (NotFoundException e) {
                // fall thru, it means there is no QR code in image
            }

            String name = FilenameUtils.removeExtension(f.getName())+".txt";
            save(results,outputDir,name);
            if( ++count%80 == 0 )
                System.out.println();
        }
    }
}