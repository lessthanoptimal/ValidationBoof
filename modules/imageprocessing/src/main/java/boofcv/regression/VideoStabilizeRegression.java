package boofcv.regression;

import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.common.RegressionRunner;
import boofcv.metrics.stabilization.FactoryRegressionVideoStabilization;
import boofcv.metrics.stabilization.RuntimePerformanceVideoStabilization;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.Tuple2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Peter Abeles
 */
public class VideoStabilizeRegression extends BaseRegression implements ImageRegression {

    List<File> videos = new ArrayList<>();

    public VideoStabilizeRegression() {
        super(BoofRegressionConstants.TYPE_IMAGEPROCESSING);
        videos.add( new File("data/video_stabilize/shake.mjpeg") );
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        process(ImageType.pl(3,type));
        process(ImageType.single(type));
    }

    public void process(ImageType imageType) throws IOException {
        List<Tuple2<String,StitchingFromMotion2D>> algorithms = new ArrayList<>();
        algorithms.add(FactoryRegressionVideoStabilization.createKlt(imageType));

        for( Tuple2<String,StitchingFromMotion2D> a : algorithms ) {
            performRuntime(a.data0,a.data1,imageType);
        }
    }


    private void performRuntime(String name , StitchingFromMotion2D alg , ImageType imageType )
            throws FileNotFoundException
    {
        switch( imageType.getFamily() ) {
            case GRAY:name += "_gray";break;
            case PLANAR:name += "_planar";break;
            case INTERLEAVED:name += "_interleaved";break;
            default: throw new RuntimeException("Unknown image family");
        }

        RuntimePerformanceVideoStabilization benchmark = new RuntimePerformanceVideoStabilization(alg,imageType);

        PrintStream outputAcc = new PrintStream(new File(directory,"ACC_VideoStabilization_"+name+".txt"));
        BoofRegressionConstants.printGenerator(outputAcc, getClass());
        outputAcc.println("# Runtime Performance of "+name);

        PrintStream outputRuntime = new PrintStream(new File(directory,"RUN_VideoStabilization_"+name+".txt"));
        BoofRegressionConstants.printGenerator(outputRuntime, getClass());
        outputRuntime.println("# Runtime Performance of "+name);

        benchmark.setOutputMetrics(outputAcc);
        benchmark.setOutputRuntime(outputRuntime);
        benchmark.setErrorStream(errorLog);
        benchmark.evaluate(videos);

        outputAcc.close();
        outputRuntime.close();
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{VideoStabilizeRegression.class.getName(),ImageDataType.F32.toString()});
    }
}
