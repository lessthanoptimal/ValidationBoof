package validation;

import boofcv.BoofVersion;
import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Saves a list of detected qr codes
 *
 * @author Peter Abeles
 */
public class DetectQrCodeBoofCVApp {
    @Option(name="-i",aliases = {"--Input"}, usage="Input directory.")
    String inputDir;
    @Option(name="-o",aliases = {"--Output"}, usage="Output directory.")
    String outputDir=".";

    private static void printHelpExit( CmdLineParser parser ) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    private static void failExit( String message ) {
        System.err.println(message);
        System.exit(1);
    }

    public static void save(List<QrCode> results , File outputDir , String outputName ) throws FileNotFoundException {
        PrintStream out = new PrintStream(new File(outputDir,outputName));

        out.println("# BoofCV "+ BoofVersion.VERSION+" QR-Code Detections "+outputName);
        for (int i = 0; i < results.size(); i++) {
            QrCode qr = results.get(i);

            out.println("message = "+qr.message.replaceAll("\\p{C}", "?"));

            for (int j = 0; j < 4; j++) {
                Point2D_F64 p = qr.bounds.get(j);
                out.printf("%f %f ",p.x,p.y);
            }
            out.println();
        }
        if( results.size() < 10 )
            System.out.print(results.size());
        else
            System.out.print("*");
        out.close();
    }

    public static void main(String[] args) {

        QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null,GrayU8.class);

        DetectQrCodeBoofCVApp generator = new DetectQrCodeBoofCVApp();
        CmdLineParser parser = new CmdLineParser(generator);
        try {
            parser.parseArgument(args);
            if( generator.inputDir == null )
                printHelpExit(parser);

            File outputDir = new File(generator.outputDir);

            for( File f : new File(generator.inputDir).listFiles() ) {
                if( f.isDirectory() ) {
                    processDirectory(detector,f,new File(outputDir,f.getName()));
                }
            }

            processDirectory(detector,new File(generator.inputDir),outputDir);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processDirectory(QrCodeDetector<GrayU8> detector,
                                         File inputDir, File outputDir ) throws IOException {
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

        GrayU8 gray = new GrayU8(1,1);

        List<File> files = Arrays.asList(_files);
        Collections.sort(files);

        int count = 0;
        for( File f : files ) {
            if( !f.isFile() || f.isHidden() )
                continue;

            boolean type_image = f.getName().toLowerCase().endsWith("jpg") || f.getName().toLowerCase().endsWith("png");

            if( !type_image )
                continue;

            BufferedImage image = ImageIO.read(f);
            if( image == null ) {
                failExit("Can't load image "+f.getName());
            }
            ConvertBufferedImage.convertFrom(image,gray);

            detector.process(gray);

            String name = FilenameUtils.removeExtension(f.getName())+".txt";
            save(detector.getDetections(),outputDir,name);
            if( ++count%80 == 0 )
                System.out.println();
        }
    }
}