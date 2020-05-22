package boofcv.common;

import boofcv.misc.BoofMiscOps;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BoofRegressionConstants {

    public static final String TYPE_CALIBRATION = "calibration";
    public static final String TYPE_CLASSIFICATION = "classification";
    public static final String TYPE_FEATURE= "feature";
    public static final String TYPE_FIDCUIALS = "fiducials";
    public static final String TYPE_GEOMETRY = "geometry";
    public static final String TYPE_IMAGEPROCESSING = "imageprocessing";
    public static final String TYPE_TRACKING = "tracking";
    public static final String TYPE_SEGMENTATION = "segmentation";
    public static final String TYPE_SHAPE = "shape";

    public static final String CURRENT_DIRECTORY = "regression/current/";
    public static final String BASELINE_DIRECTORY = "regression/baseline/";

    public static void printGenerator(PrintStream output, Class which ) {
        output.println("# GENERATOR "+which.getSimpleName());
    }

    public static File tempDir() {
        return new File("tmp");
    }

    public static void clearCurrentResults() {
        BoofRegressionConstants.delete( new File(BoofRegressionConstants.CURRENT_DIRECTORY),null);
        if( !new File(BoofRegressionConstants.CURRENT_DIRECTORY).mkdir() )
            throw new RuntimeException("Can't create directory");
    }

    public static void delete( File f , PrintStream error ) {
        boolean deleted = false;
        for (int i = 0; i < 10 && !deleted; i++) {
            try {
                if( f.isDirectory() ) {
                    FileUtils.deleteDirectory(f);
                    deleted = true;
                } else if( f.delete() ) {
                    deleted = true;
                }
            } catch( IOException ignore) {
                System.out.println(ignore.getMessage());
            }

            if( !deleted ) {
                // Mother F*****. On windows it will often not delete the file for a long time. Plus Java won't
                // get rid of the file until the garbage collector is called. This hack allows still to get done
                System.gc();
                BoofMiscOps.sleep(250);
                System.gc();
            }
        }
        if( !deleted && error != null  ) {
            error.println("Can't delete "+f.getName());
            throw new RuntimeException("Can't delete "+f.getName());
        }
    }

    public static List<File> listAndSort(File directory )
    {
        File[] files = directory.listFiles();
        if( files == null )
            throw new RuntimeException("Directory has no children! "+directory.getName());

        List<File> out = Arrays.asList(files);
        Collections.sort(out);

        return out;
    }
}
