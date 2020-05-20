package boofcv.metrics;

import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.common.BoofRegressionConstants;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.*;
import org.ddogleg.struct.GrowQueue_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes metrics from each region in the super pixel. These metrics don't really say how good its doing but
 * provide some insight into the core algorithm changing
 *
 * @author Peter Abeles
 */
public class ComputeSuperPixelsMetrics<T extends ImageBase<T>> {

    public PrintStream out = System.out;
    public PrintStream err = System.err;
    public final GrowQueue_F64 timesMS = new GrowQueue_F64();

    List<String> names = new ArrayList<>();
    List<T> images = new ArrayList<>();

    public ComputeSuperPixelsMetrics(String path , ImageType<T> imageType ) {

        File dir = new File(path);

        List<File> files = BoofRegressionConstants.listAndSort(dir);

        for( File f : files ) {
            if( !f.isFile()) continue;

            BufferedImage b = UtilImageIO.loadImage(f.getAbsolutePath());
            if( b != null ) {
                T image = imageType.createImage(b.getWidth(),b.getHeight());
                ConvertBufferedImage.convertFrom(b,image,true);
                images.add(image);
                names.add(f.getName());
            }
        }
    }

    public void process( String algName , ImageSuperpixels<T> alg ) {

        timesMS.reset();
        out.println(algName);

        GrayS32 segmented = new GrayS32(1,1);

        for (int i = 0; i < images.size(); i++) {
            T image = images.get(i);
            segmented.reshape(image.width,image.height);
            long time0 = System.nanoTime();
            alg.segment(image,segmented);
            long time1 = System.nanoTime();
            timesMS.add((time1-time0)*1e-6);

            int regions = alg.getTotalSuperpixels();
            out.printf("  %20s regions=%4d",names.get(i),regions);

            computeColorMetrics(image,segmented,regions);
            computeShapeMetrics(segmented,regions);
            out.println();
            out.flush();
        }
    }

    private void computeColorMetrics(T image, GrayS32 segmented, int regions) {

        // convert int into a known gray image to make the math easier to compute
        ImageGray gray;
        if( image instanceof ImageMultiBand) {
            gray = GeneralizedImageOps.createSingleBand(image.imageType.getDataType(),image.width,image.height);
            GConvertImage.average((ImageMultiBand)image, gray);
        } else {
            gray = (ImageGray)image;
        }
        GrayF32 grayF32;

        if( gray instanceof GrayF32) {
            grayF32 = (GrayF32)gray;
        } else {
            grayF32 = new GrayF32(image.width,image.height);
            GConvertImage.convert(gray,grayF32);
        }

        // now compute statistics of each region
        int N = image.width*image.height;
        int count[] = new int[regions];
        float mean[] = new float[regions];
        int totalValid = 0;
        for (int i = 0; i < N; i++) {
            int r = segmented.data[i];
            count[r]++;
            mean[r] += grayF32.data[i];
        }

        for (int i = 0; i < regions; i++) {
            if( count[i] > 0 ) {
                mean[i] /= count[i];
                totalValid++;
            }
        }

        float stdev[] = new float[regions];
        for (int i = 0; i < N; i++) {
            int r = segmented.data[i];
            float diff = grayF32.data[i]-mean[r];
            stdev[r] += diff*diff;
        }
        for (int i = 0; i < regions; i++) {
            if( count[i] > 0 )
                stdev[i] = (float)Math.sqrt(stdev[i]/count[i]);
        }

        // standard deviation of the region values
        float meanAll = 0;
        for (int i = 0; i < regions; i++) {
            meanAll += mean[i]*count[i];
        }
        meanAll /= N;

        float stdevAll = 0;
        for (int i = 0; i < regions; i++) {
            float diff = mean[i]-meanAll;
            stdevAll += diff*diff*count[i];
        }
        // how difference each region's color is. Probably want this to be higher
        stdevAll = (float)Math.sqrt(stdevAll/N);

        // compute average inner region stdev
        float aveStdev = 0;

        for (int i = 0; i < regions; i++) {
            aveStdev += stdev[i];
        }
        // variance of color within a single region. Probably want this to be lower
        aveStdev /= totalValid;

        out.printf(" all-stdev=%6.2f ave-stdev=%6.2f",stdevAll,aveStdev);
    }

    private void computeShapeMetrics(GrayS32 segmented, int regions) {
        int counts[] = new int[regions];

        int N = segmented.width*segmented.height;
        for (int i = 0; i < N; i++) {
            counts[segmented.data[i]]++;
        }

        double meanCounts = N/regions;

        double variance = 0;
        for (int i = 0; i < regions; i++) {
            double diff = counts[i]-meanCounts;
            variance += diff*diff;
        }
        // provides a measure of how uniform the size of each region is
        out.printf(" size-stdev/mean=%7.4f",Math.sqrt(variance/regions)/meanCounts);
    }
}
