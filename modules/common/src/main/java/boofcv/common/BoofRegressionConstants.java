package boofcv.common;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BoofRegressionConstants {
    public static final String CURRENT_DIRECTORY = "regression/current/";
    public static final String BASELINE_DIRECTORY = "regression/baseline/";

    public static File tempDir() {
        return new File("tmp");
    }

    public static void clearCurrentResults() throws IOException {
        FileUtils.deleteDirectory( new File(BoofRegressionConstants.CURRENT_DIRECTORY));
        if( !new File(BoofRegressionConstants.CURRENT_DIRECTORY).mkdir() )
            throw new RuntimeException("Can't create directory");
        if( !new File(BoofRegressionConstants.CURRENT_DIRECTORY+"other").mkdir() )
            throw new RuntimeException("Can't create directory");
        if( !new File(BoofRegressionConstants.CURRENT_DIRECTORY+"U8").mkdir() )
            throw new RuntimeException("Can't create directory");
        if( !new File(BoofRegressionConstants.CURRENT_DIRECTORY+"F32").mkdir() )
            throw new RuntimeException("Can't create directory");
    }

    public static List<File> listAndSort(File directory ) {


        File[] files = directory.listFiles();
        if( files == null )
            throw new RuntimeException("Directory has no children! "+directory.getName());

        List<File> out = Arrays.asList(files);
        Collections.sort(out);

        return out;
    }
}
