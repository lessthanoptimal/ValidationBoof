package validate.segmentation;

import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.*;

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

    List<String> names = new ArrayList<>();
    List<T> images = new ArrayList<>();

    public ComputeSuperPixelsMetrics(String path , ImageType<T> imageType ) {

        File dir = new File(path);

        for( File f : dir.listFiles() ) {
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

        out.println(algName);

        GrayS32 segmented = new GrayS32(1,1);

        for (int i = 0; i < images.size(); i++) {
            T image = images.get(i);
            segmented.reshape(image.width,image.height);
            alg.segment(image,segmented);

            int regions = alg.getTotalSuperpixels();
            double stdevGray = computeGrayStdev(image,segmented,regions);
            double stdevSize = computeRegionSizeStdev(segmented,regions);

            out.printf("  %20s %4d gray-stdev %6.1f size-stdev %6.2f\n",names.get(i),regions,stdevGray,stdevSize);

        }
    }

    private double computeGrayStdev(T image, GrayS32 segmented, int regions) {

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

        float aveStdev = 0;

        for (int i = 0; i < regions; i++) {
            aveStdev += stdev[i];
        }
        aveStdev /= totalValid;
        return aveStdev;
    }

    private double computeRegionSizeStdev( GrayS32 segmented, int regions) {
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
        return Math.sqrt(variance/regions);
    }
}
