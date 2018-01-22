package boofcv.regression;

import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.common.BaseImageRegression;
import boofcv.common.ValidationConstants;
import boofcv.factory.segmentation.ConfigSlic;
import boofcv.factory.segmentation.FactoryImageSegmentation;
import boofcv.metrics.ComputeSuperPixelsMetrics;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class SuperPixelRegression extends BaseImageRegression {

    public static String pathToData = ValidationConstants.PATH_DATA+"segmentation/";

    PrintStream out;
    ComputeSuperPixelsMetrics metrics;

    @Override
    public void process(ImageDataType type) throws IOException {
        List<ImageSuperpixels> algs = new ArrayList<>();

        ImageType imageType = ImageType.pl(3,type);

        out = new PrintStream(new File(directory,"SuperPixels.txt"));
        metrics = new ComputeSuperPixelsMetrics(pathToData,imageType);

        metrics.err = errorLog;
        metrics.out = out;

        out.println("Super Pixel Regression Metrics\n\n");

        process("FH04",FactoryImageSegmentation.fh04(null,imageType));
        process("MeanShift",FactoryImageSegmentation.meanShift(null,imageType));
        process("SLIC",FactoryImageSegmentation.slic(new ConfigSlic(200),imageType));
        process("Watershed",FactoryImageSegmentation.watershed(null,imageType));

        out.close();
    }

    private void process( String name , ImageSuperpixels alg ) {
        System.out.println("processing "+name);

        try {
            metrics.process(name, alg);
        } catch( RuntimeException e ) {
            e.printStackTrace(errorLog);
        }
    }

    public static void main(String[] args) throws IOException {
        SuperPixelRegression regression = new SuperPixelRegression();

        regression.setOutputDirectory(".");
        regression.process(ImageDataType.F32);
    }
}
