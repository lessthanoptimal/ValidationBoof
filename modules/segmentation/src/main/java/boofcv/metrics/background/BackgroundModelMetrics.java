package boofcv.metrics.background;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.GrowQueue_F64;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Peter Abeles
 */
public class BackgroundModelMetrics<T extends ImageBase<T>> {

    public PrintStream out = System.out;

    public GrowQueue_F64 periodMS = new GrowQueue_F64();

    public void evaluate(File directory , BackgroundModelStationary<T> model ) {
        ImageType<T> imageType = model.getImageType();
        SimpleImageSequence<T> sequence;

        if( new File(directory,"movie.mp4").exists() ) {
            sequence = DefaultMediaManager.INSTANCE.openVideo(
                    new File(directory,"movie.mp4").getAbsolutePath(),imageType);
        } else {
            throw new IllegalArgumentException("Can't find input movie");
        }

        Map<Integer,String> truthPaths = loadTruth(new File(directory,"motion"));

        GrayU8 segment = new GrayU8(sequence.getWidth(),sequence.getHeight());

        List<ImageMetrics> results = new ArrayList<>();

        model.reset();
        int frame = 0;
        while( sequence.hasNext() ) {
            T input = sequence.next();
            long before = System.nanoTime();
            model.updateBackground(input,segment);
            long after = System.nanoTime();

            periodMS.add((after-before)*1e-6);

            if( truthPaths.containsKey(frame)) {
                results.add( evalute(segment,truthPaths.get(frame)));
            }

            frame++;
        }

        double meanF=0,meanRecall=0,meanPrecision=0;
        int totalWithPositive=0;
        int totalF = 0;
        for( ImageMetrics m : results ) {
            if( !Double.isNaN(m.computeF())) {
                meanF += m.computeF();
                totalF++;
            }
            if( m.tp+m.fn > 0 ) {
                meanRecall += m.recall();
                meanPrecision += m.precision();
                totalWithPositive++;
            }
        }
        meanF /= totalF;
        meanRecall /= totalWithPositive;
        meanPrecision /= totalWithPositive;

        out.printf("%20s %4d %5.2f %5.2f %5.2f\n",directory.getName(),truthPaths.size(),meanF,meanRecall,meanPrecision);
    }

    private static ImageMetrics evalute( GrayU8 found , String pathTruth ) {
        ImageMetrics metrics = new ImageMetrics();
        GrayU8 imageTruth = UtilImageIO.loadImage(pathTruth,GrayU8.class);

        if( found.width != imageTruth.width || found.height != imageTruth.height )
            throw new IllegalArgumentException("Inconsistent image shape");

        for (int i = 0; i < found.height; i++) {
            for (int j = 0; j < found.width; j++) {
                int expectedValue = imageTruth.unsafe_get(j,i) >= 125 ? 1 : 0;
                int foundValue = found.unsafe_get(j,i);

                if( expectedValue == 1 ) {
                    if( foundValue == 1 ) {
                        metrics.tp++;
                    } else {
                        metrics.fn++;
                    }
                } else {
                    if( foundValue == 1 ) {
                        metrics.fp++;
                    } else {
                        metrics.tn++;
                    }
                }
            }
        }

        return metrics;
    }

    private static class ImageMetrics {
        int tp,fp,tn,fn;

        public double computeF() {
            return 2.0*tp/(2.0*tp + fp + fn);
        }
        public double precision() {
            return tp/(double)(tp+fp);
        }
        public double recall() {
            return tp/(double)(tp+fn);
        }
    }

    public static Map<Integer,String> loadTruth( File directory ) {

        Map<Integer,String> output = new HashMap<>();

        String prefix = "motion";
        List<String> files = UtilIO.listByPrefix(directory.getPath(),prefix,null);

        for( String path : files ) {
            File f = new File(path);

            String inner = FilenameUtils.removeExtension(f.getName()).substring(prefix.length());

            output.put( Integer.parseInt(inner), f.getPath() );
        }
        return output;
    }
}
