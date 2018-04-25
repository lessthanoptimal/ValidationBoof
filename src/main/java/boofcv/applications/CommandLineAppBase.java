package boofcv.applications;

import org.kohsuke.args4j.CmdLineParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class CommandLineAppBase {

    protected boolean readImage = true;

    protected abstract void processFile( BufferedImage image , File inputFile ,  File outputDirectory );

    public void processRootDirectory( String rootInput, String rootOutput ) throws IOException {
        File outputDir = new File(rootOutput);

        for( File f : new File(rootInput).listFiles() ) {
            if( f.isDirectory() ) {
                processDirectory(f,new File(outputDir,f.getName()));
            }
        }

        processDirectory(new File(rootInput),outputDir);
    }

    protected void processDirectory( File inputDir, File outputDir ) throws IOException {
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

        for( File f : files ) {
            if( !f.isFile() || f.isHidden() )
                continue;

            boolean type_image = f.getName().toLowerCase().endsWith("jpg") || f.getName().toLowerCase().endsWith("png");

            if( !type_image )
                continue;

            if( readImage ) {
                BufferedImage image = ImageIO.read(f);
                if (image == null) {
                    failExit("Can't load image " + f.getName());
                }
                processFile(image, f, outputDir);
            } else {
                processFile(null,f,outputDir);
            }
        }
    }

    public static void printHelpExit( CmdLineParser parser ) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    public static void failExit( String message ) {
        System.err.println(message);
        System.exit(1);
    }
}
