package boofcv;

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.abst.filter.binary.BinaryContourFinderChang2004;
import boofcv.alg.color.ColorRgb;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.applications.CommandLineAppBase;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FindContoursBoofCVApp extends CommandLineAppBase {
    @Option(name="-i",aliases = {"--Input"}, usage="Input directory.")
    String inputDir;
    @Option(name="-o",aliases = {"--Output"}, usage="Output directory.")
    String outputDir=".";
    @Option(name="-t",aliases = {"--Threshold"}, usage="Binarization threshold.")
    int threshold=0;

    BinaryContourFinder contourFinder = new BinaryContourFinderChang2004();

    Planar<GrayU8> rgb = new Planar(GrayU8.class,1,1,3);
    GrayU8 gray = new GrayU8(1,1);
    GrayU8 binary = new GrayU8(1,1);
    GrayS32 labeled = new GrayS32(1,1);


    public FindContoursBoofCVApp() {
        contourFinder.setConnectRule(ConnectRule.EIGHT);

        contourFinder.setSaveInnerContour(false);

    }

    @Override
    protected void processFile(BufferedImage image, File inputFile, File outputDirectory) {
        ConvertBufferedImage.convertFrom(image,rgb,true);

        ColorRgb.rgbToGray_Weighted(rgb,gray);
        binary.reshape(gray.width, gray.height);

        ThresholdImageOps.threshold(gray,binary,threshold,true);

        labeled.reshape(binary.width,binary.height);

        long before = System.currentTimeMillis();
        contourFinder.process(binary,labeled);
        long after = System.currentTimeMillis();

        int N = contourFinder.getContours().size();

        System.out.printf("%4dx%4d time = %4d count %4d file %s\n",binary.width,binary.height,after-before,N,inputFile.getPath());

//            String name = FilenameUtils.removeExtension(f.getName())+".txt";
//            save(detector.getDetections(),outputDir,name);
//            if( ++count%80 == 0 )
//                System.out.println();
    }

    public static void main(String[] args) {
        FindContoursBoofCVApp generator = new FindContoursBoofCVApp();
        CmdLineParser parser = new CmdLineParser(generator);
        try {
            parser.parseArgument(args);
            if( generator.inputDir == null )
                printHelpExit(parser);
            if( generator.outputDir == null )
                printHelpExit(parser);
            if( generator.threshold == 0 )
                printHelpExit(parser);

            generator.processRootDirectory(generator.inputDir,generator.outputDir);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
