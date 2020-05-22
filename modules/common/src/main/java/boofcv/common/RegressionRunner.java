package boofcv.common;

import boofcv.BoofVersion;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.ImageDataType;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static boofcv.common.BoofRegressionConstants.CURRENT_DIRECTORY;

/**
 * Runs a single regression class. Before the regression is run the work space is cleaned up. The output in
 * regression/current is assumed to be already set up.
 *
 * @author Peter Abeles
 */
public class RegressionRunner {

    public static void clearWorkDirectory() {
        File[] files = new File(".").listFiles();

        // sanity check the directory before it starts deleting shit
        if( !contains(files,"src"))
            throw new RuntimeException("Can't find boofcv in working directory");
        if( !contains(files,"lib"))
            throw new RuntimeException("Can't find lib in working directory");
        if( !contains(files,"regression"))
            throw new RuntimeException("Can't find regression in working directory");

        try {
            FileUtils.deleteDirectory(BoofRegressionConstants.tempDir());
        } catch (IOException ignore) {}

        for( File f : files ) {
            if( f.isDirectory() )
                continue;

            if( f.isHidden() )
                continue;

            if( f.getName().contains(".iml") )
                continue;

            if( f.getName().contains(".txt") ) {
                if( !f.getName().equals("email_login.txt") &&
                        !f.getName().equals("cronlog.txt") &&
                        !f.delete() )
                    throw new RuntimeException("Can't clean work directory: " + f.getName());
            }

        }
    }

    private static boolean contains( File[] files, String name ) {
        for( File f : files ) {
            if( f.getName().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {

        // Turn off threading since it can cause non-deterministic results
        BoofConcurrency.USE_CONCURRENT = false;

        clearWorkDirectory();
        SettingsLocal.loadExitIfFail();

        Class regressionClass = Class.forName(args[0]);
        Object o = regressionClass.newInstance();
        if( args.length == 2 ) {
            ImageDataType imageType = ImageDataType.valueOf(args[1]);

            ImageRegression regression;
            try {
                regression = (ImageRegression) o;
            } catch( ClassCastException e ) {
                System.err.println("Can't class regression "+o.getClass().getSimpleName()+" into ImageRegression");
                System.err.println("Maybe the suffix of the regression is wrong? *Regression for image " +
                        "and *FRegression for file");
                System.exit(1);
                return;
            }

            File pathToMetricImage = new File(SettingsLocal.getPathToCurrentMetricsDirectory(),imageType.toString());
            File pathToRuntime = new File(SettingsLocal.getPathToCurrentRuntimeDirectory(),imageType.toString());

            regression.setMetricsDirectory(pathToMetricImage.getPath());
            regression.setRuntimeDirectory(pathToRuntime.getPath());

            try {
                regression.process(imageType);
            } catch( ImageTypeNotSupportedException e ) {
                System.out.println("Image type not supported. Skipping. "+e.getMessage());
            } catch( RuntimeException e ) {
                e.printStackTrace(regression.getErrorStream());
            }
            regression.getErrorStream().close();
        } else if( args.length == 1 ) {
            FileRegression regression;
            try {
                regression = (FileRegression) o;
            } catch( ClassCastException e ) {
                System.err.println("Can't class regression "+o.getClass().getSimpleName()+" into FileRegression");
                System.err.println("Maybe the suffix of the regression is wrong? *Regression for image " +
                        "and *FRegression for file");
                System.exit(1);
                return;
            }
            regression.setMetricsDirectory(new File(CURRENT_DIRECTORY,"other").getPath());
            try {
                regression.process();
            } catch( RuntimeException e ) {
                e.printStackTrace(regression.getErrorStream());
            }
            regression.getErrorStream().close();
        } else {
            throw new RuntimeException("Unexpected number of arguments "+args.length);
        }
        System.out.println("\n\nDone. BOOFCV-SHA "+ BoofVersion.GIT_SHA+" at "+new Date().toString());
    }
}
