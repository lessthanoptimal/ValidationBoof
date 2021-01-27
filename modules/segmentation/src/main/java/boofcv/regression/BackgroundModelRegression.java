package boofcv.regression;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.common.*;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.metrics.background.BackgroundModelMetrics;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_F64;

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

        List<File> files = BoofRegressionConstants.listAndSort(dataPath);

        PrintStream out = new PrintStream(new FileOutputStream(new File(directoryMetrics,"ACC_background_stationary.txt")));
        BoofRegressionConstants.printGenerator(out, getClass());
        out.println("# Stationary Background Model Detection Metrics");
        out.println("# <data set> <# truth> <mean F> <mean precision> <mean recall>");
        out.println();

        RuntimeSummary runtime = new RuntimeSummary();
        runtime.initializeLog(directoryRuntime, getClass(),"RUN_background_stationary.txt");

        for( Info info : stationary ) {
            BackgroundModelMetrics metrics = new BackgroundModelMetrics();
            metrics.out = out;
            out.println("# "+info.name);

            DogArray_F64 summaryTimeMS = new DogArray_F64();
            runtime.out.println(info.name);
            runtime.printUnitsRow(false);

            for( File f : files ) {
                if( !new File(f,"motion").isDirectory() )
                    continue;

                System.out.println(info.name+" "+f.getName());

                try {
                    metrics.evaluate(f, info.algorithm);

                    summaryTimeMS.addAll(metrics.periodMS);
                    runtime.printStatsRow(f.getName(), metrics.periodMS);
                } catch (Exception e) {
                    errorLog.println("------------------------------------------------------------------");
                    errorLog.println("Exception in "+f.getPath());
                    e.printStackTrace(errorLog);
                    e.printStackTrace(System.err);
                }
            }
            out.println();
            runtime.out.println();
            runtime.saveSummary(info.name,summaryTimeMS);
        }

        runtime.printSummaryResults();
        runtime.out.close();

        out.close();
    }


    public static class Info {
        String name;
        BackgroundModelStationary algorithm;

        public Info(String name, BackgroundModelStationary algorithm) {
            this.name = name;
            this.algorithm = algorithm;
        }
    }

    public static void main(String[] args)
            throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{BackgroundModelRegression.class.getName(),ImageDataType.U8.toString()});
    }
}
