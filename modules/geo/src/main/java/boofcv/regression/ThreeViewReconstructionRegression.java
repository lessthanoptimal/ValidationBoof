package boofcv.regression;

import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.io.UtilIO;
import boofcv.metrics.mvs.ThreeViewStereoPerformance;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evaluates three view reconstruction from uncalibrated images.
 *
 * @author Peter Abeles
 */
public class ThreeViewReconstructionRegression extends BaseRegression implements ImageRegression {

    PrintStream outputRuntime;

    ThreeViewStereoPerformance evaluator = new ThreeViewStereoPerformance();

    public ThreeViewReconstructionRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        outputRuntime = new PrintStream(new File(directory,"RUN_ThreeViewReconstruction.txt"));
        BoofRegressionConstants.printGenerator(outputRuntime, getClass());
        outputRuntime.println("# Runtime for each triplet of images in milliseconds\n");

        PrintStream out = new PrintStream(new File(directory,"ACC_ThreeViewReconstruction.txt"));
        BoofRegressionConstants.printGenerator(out, getClass());
        out.println("# Uncalibrated Three View Reconstruction Performance. Stereo association percentage");
        out.println();

        File inputDir = new File("data/multiview/triple");
        List<String> images = findUniquePrefixes(inputDir);

        if( images.size() == 0 ) {
            errorLog.println("Can't find any images!");
            return;
        }

        for( String image : images ) {
            System.out.println("Evaluating "+image);
            try {
                if (evaluator.process(new File(inputDir, image).getPath(), "jpg")) {
                    out.printf("%30s %6.2f\n", image, evaluator.getScore() * 100);
                    outputRuntime.printf("%30s %d\n", image, evaluator.getElapsedTime());
                } else {
                    out.printf("%30s failed!\n", image);
                    outputRuntime.printf("%30s failed!\n", image);
                }
            } catch( Exception e ) {
                errorLog.println(e);
            }
        }

        outputRuntime.close();
    }

    private List<String> findUniquePrefixes(File directory ) {
        File[] all = UtilIO.findMatches(directory,"\\w*.jpg");
        List<String> prefixes = new ArrayList<>();

        for( File f : all ) {
            String name = f.getName();
            name = name.substring(0,name.length()-6);
            if( prefixes.contains(name))
                continue;
            prefixes.add(name);
        }
        Collections.sort(prefixes);

        return prefixes;
    }

    public static void main(String[] args) throws IOException {
        ThreeViewReconstructionRegression regression = new ThreeViewReconstructionRegression();
        regression.process(ImageDataType.U8);
    }
}
