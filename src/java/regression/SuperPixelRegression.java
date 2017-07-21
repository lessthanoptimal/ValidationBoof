package regression;

import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.factory.segmentation.ConfigSlic;
import boofcv.factory.segmentation.FactoryImageSegmentation;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import validate.ValidationConstants;
import validate.segmentation.ComputeSuperPixelsMetrics;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class SuperPixelRegression extends BaseTextFileRegression {

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

        process("FH04",FactoryImageSegmentation.fh04(null,imageType),imageType);
        process("MeanShift",FactoryImageSegmentation.meanShift(null,imageType),imageType);
        process("SLIC",FactoryImageSegmentation.slic(new ConfigSlic(200),imageType),imageType);
        process("Watershed",FactoryImageSegmentation.watershed(null,imageType),imageType);

        out.close();
    }

    private void process( String name , ImageSuperpixels alg , ImageType imageType ) {
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
