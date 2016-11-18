package regression;

import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.Tuple2;
import validate.stabilization.FactoryRegressionVideoStabilization;
import validate.stabilization.RuntimePerformanceVideoStabilization;

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
public class VideoStabilizeRegression extends BaseTextFileRegression {

    List<File> videos = new ArrayList<>();

    public VideoStabilizeRegression() {
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
        name = "VideoStabilization_Runtime_" + name;
        switch( imageType.getFamily() ) {
            case GRAY:name += "_gray";break;
            case PLANAR:name += "_planar";break;
            case INTERLEAVED:name += "_interleaved";break;
            default: throw new RuntimeException("Unknown image family");
        }

        RuntimePerformanceVideoStabilization benchmark = new RuntimePerformanceVideoStabilization(alg,imageType);

        PrintStream out = new PrintStream(new File(directory,name+".txt"));

        out.println("# Runtime Performance of "+name);

        benchmark.setOutputResults(out);
        benchmark.setErrorStream(errorLog);

        benchmark.evaluate(videos);
    }
}
