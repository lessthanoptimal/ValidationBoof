package boofcv.regression;

import boofcv.common.*;
import boofcv.io.UtilIO;
import boofcv.metrics.mvs.ThreeViewStereoPerformance;
import boofcv.struct.image.ImageDataType;
import org.ddogleg.stats.UtilStatisticsQueue;
import org.ddogleg.struct.GrowQueue_F64;

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

    RuntimeSummary outputRuntime;

    ThreeViewStereoPerformance evaluator = new ThreeViewStereoPerformance();

    public ThreeViewReconstructionRegression() {
        super(BoofRegressionConstants.TYPE_GEOMETRY);
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        outputRuntime = new RuntimeSummary();
        outputRuntime.out = new PrintStream(new File(directoryRuntime, "RUN_ThreeViewReconstruction.txt"));
        BoofRegressionConstants.printGenerator(outputRuntime.out, getClass());
        outputRuntime.out.println("# All times are in milliseconds");
        outputRuntime.out.println();

        PrintStream out = new PrintStream(new File(directoryMetrics,"ACC_ThreeViewReconstruction.txt"));
        BoofRegressionConstants.printGenerator(out, getClass());
        out.println("# Uncalibrated Three View Reconstruction Performance. Stereo association percentage");
        out.println("# (name) (score: higher better) (rectified area: higher better)");
        out.println();

        File inputDir = new File("data/multiview/triple");
        List<String> images = findUniquePrefixes(inputDir);

        if( images.size() == 0 ) {
            errorLog.println("Can't find any images!");
            return;
        }

        GrowQueue_F64 scores = new GrowQueue_F64();
        GrowQueue_F64 areas = new GrowQueue_F64();
        GrowQueue_F64 runtimes = new GrowQueue_F64();
        int totalFailed = 0;
        for( String image : images ) {
            System.out.println("Evaluating "+image);
            try {
                if (evaluator.process(new File(inputDir, image).getPath(), "jpg")) {
                    scores.add( evaluator.getScore() );
                    areas.add( evaluator.getAreaFraction() );
                    runtimes.add( evaluator.getElapsedTime() );
                    out.printf("%30s %6.2f %6.2f\n", image, evaluator.getScore() * 100,100*evaluator.getAreaFraction());
                    outputRuntime.out.printf("%30s %d\n", image, evaluator.getElapsedTime());
                } else {
                    totalFailed++;
                    out.printf("%30s failed!\n", image);
                    outputRuntime.out.printf("%30s failed!\n", image);
                }
            } catch( Exception e ) {
                errorLog.println(e);
            }
        }

        out.println();
        out.println("Summary:");
        out.println("total = "+images.size()+"  failed = "+totalFailed);
        out.printf("%10s %7s %7s %7s %7s\n","metric","mean","P03","P50","P97");
        printSummary(out,"%7.5f","score",scores);
        printSummary(out,"%7.5f","area",areas);

        outputRuntime.out.println();
        outputRuntime.printHeader(true);
        outputRuntime.printStats("summary",runtimes);
        outputRuntime.out.close();
    }

    private void printSummary( PrintStream out, String format, String metric , GrowQueue_F64 values ) {
        values.sort();
        double mean = UtilStatisticsQueue.mean(values);
        double p03 = values.getFraction(0.03);
        double p50 = values.getFraction(0.5);
        double p97 = values.getFraction(0.97);
        out.printf("%10s "+format+" "+format+" "+format+" "+format+"\n",metric,mean,p03,p50,p97);
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

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        BoofRegressionConstants.clearCurrentResults();
//        RegressionRunner.main(new String[]{ThreeViewReconstructionRegression.class.getName(),ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{ThreeViewReconstructionRegression.class.getName(),ImageDataType.U8.toString()});
//        ThreeViewReconstructionRegression regression = new ThreeViewReconstructionRegression();
//        regression.process(ImageDataType.U8);
    }
}
