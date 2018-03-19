package boofcv.regression;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.metrics.background.BackgroundModelMetrics;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BackgroundModelRegression extends BaseRegression implements ImageRegression {

    File dataPath = new File("data/background");

    public BackgroundModelRegression() {
        super(BoofRegressionConstants.TYPE_SEGMENTATION);
    }

    @Override
    public void process( ImageDataType type ) throws IOException {
        List<Info> stationary = new ArrayList<>();

        ImageType grayType = ImageType.single(type);
        ImageType colorType = ImageType.il(3,type);

        ConfigBackgroundBasic configBasic = new ConfigBackgroundBasic(35, 0.005f);
        ConfigBackgroundGaussian configGaussian = new ConfigBackgroundGaussian(12,0.001f);

        stationary.add(new Info("basic-gray",FactoryBackgroundModel.stationaryBasic(configBasic,grayType)));
        stationary.add(new Info("gaussian-gray",FactoryBackgroundModel.stationaryGaussian(configGaussian,grayType)));
        stationary.add(new Info("gmm-gray",FactoryBackgroundModel.stationaryGmm(null,grayType)));

        stationary.add(new Info("basic-rgb",FactoryBackgroundModel.stationaryBasic(configBasic,colorType)));
        stationary.add(new Info("gaussian-rgb",FactoryBackgroundModel.stationaryGaussian(configGaussian,colorType)));
        stationary.add(new Info("gmm-rgb",FactoryBackgroundModel.stationaryGmm(null,colorType)));

        File[] files = dataPath.listFiles();
        if( files == null )
            throw new IOException("No datasets exist");

        PrintStream out = new PrintStream(new FileOutputStream(new File(directory,"ACC_background_stationary.txt")));
        BoofRegressionConstants.printGenerator(out, getClass());
        out.println("# Stationary Background Model Detection Metrics");
        out.println("# <data set> <# truth> <mean F> <mean precision> <mean recall>");
        out.println();

        PrintStream outputRuntime = new PrintStream(new File(directory,"RUN_background_stationary.txt"));
        BoofRegressionConstants.printGenerator(outputRuntime, getClass());
        outputRuntime.println("# Stationary Background Model Runtime Metrics");
        outputRuntime.println("# algorithm, average time (ms)");
        outputRuntime.println();

        for( Info info : stationary ) {
            BackgroundModelMetrics metrics = new BackgroundModelMetrics();
            metrics.out = out;
            out.println("# "+info.name);
            outputRuntime.println("# "+info.name);

            for( File f : files ) {
                if( !new File(f,"motion").isDirectory() )
                    continue;

                System.out.println(info.name+" "+f.getName());

                metrics.evaluate(f,info.algorithm);
                outputRuntime.printf("%20s %7.4f (ms)\n",f.getName(),metrics.averageTimeMS);
            }
            out.println();
            outputRuntime.println();
        }

        out.close();
        outputRuntime.close();
    }


    public static class Info {
        String name;
        BackgroundModelStationary algorithm;

        public Info(String name, BackgroundModelStationary algorithm) {
            this.name = name;
            this.algorithm = algorithm;
        }
    }

    public static void main(String[] args) throws IOException {
        BackgroundModelRegression regression = new BackgroundModelRegression();
        regression.setOutputDirectory("tmp");

        regression.process(ImageDataType.U8);
    }
}
