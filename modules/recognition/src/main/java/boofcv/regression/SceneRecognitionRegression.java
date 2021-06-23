package boofcv.regression;

import boofcv.abst.scene.ConfigFeatureToSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.common.*;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.metrics.EvaluateImageRetrieval;
import boofcv.metrics.ImageFilesInFixedSets;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates scene recognition
 *
 * @author Peter Abeles
 */
public class SceneRecognitionRegression<T extends ImageBase<T>> extends BaseRegression implements ImageRegression {

    public File dataPathRoot = new File("data/recognition/scene");

    RuntimeSummary runtime;
    DogArray_F64 summaryPeriodMS = new DogArray_F64();

    EvaluateImageRetrieval evaluate;

    public SceneRecognitionRegression() {
        super(BoofRegressionConstants.TYPE_RECOGNITION);
    }

    @Override
    public void process(ImageDataType type) throws IOException {
        PrintStream metricsOut = new PrintStream(new File(directoryMetrics, "ACC_SceneRecognition.txt"));
        BoofRegressionConstants.printGenerator(metricsOut, getClass());

        runtime = new RuntimeSummary("seconds");
        runtime.initializeLog(directoryRuntime, getClass(), "RUN_SceneRecognition.txt");

        final ImageType<T> imageType = (ImageType<T>) ImageType.single(type);

        evaluate = new EvaluateImageRetrieval<>(imageType);
        evaluate.outAccuracy = metricsOut;
        evaluate.outRuntime = runtime.out;
        evaluate.err = errorLog;
        evaluate.results.outSummary = metricsOut;
        evaluate.results.printSummaryHeader();

        List<Info> testSubjects = new ArrayList<>();
        {
            ConfigFeatureToSceneRecognition config = new ConfigFeatureToSceneRecognition();
            config.typeRecognize = ConfigFeatureToSceneRecognition.Type.NISTER_2006;
            testSubjects.add(new Info("Nister2006", config));
        }
        {
            ConfigFeatureToSceneRecognition config = new ConfigFeatureToSceneRecognition();
            config.typeRecognize = ConfigFeatureToSceneRecognition.Type.NEAREST_NEIGHBOR;
            config.recognizeNeighbor.kmeans.maxIterations = 1; // need to speed this up. Way too slow with default
            testSubjects.add(new Info("NearestNeighbor", config));
        }

        for (Info info : testSubjects) {
            evaluate.outAccuracy.println("\n" + info.name);
            SceneRecognition<T> recognizer = FactorySceneRecognition.createFeatureToScene(info.config, imageType);
            evaluate(info.name, recognizer);
        }

        runtime.out.println();
        runtime.printSummaryResults();

        metricsOut.close();
        runtime.out.close();
    }

    void evaluate(String name, SceneRecognition<T> recognizer) {
        evaluate.listOfSets.clear();
        evaluate.addDataset("ukbench500", new ImageFilesInFixedSets(0, 4, new File(dataPathRoot, "ukbench500")));
        evaluate.evaluate(name, recognizer);
        runtime.saveSummary(name, DogArray_F64.array(evaluate.totalTimeMS));
        runtime.out.println();
    }

    public class Info {
        public String name;
        public ConfigFeatureToSceneRecognition config;

        public Info(String name, ConfigFeatureToSceneRecognition config) {
            this.name = name;
            this.config = config;
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        BoofRegressionConstants.clearCurrentResults();
//        RegressionRunner.main(new String[]{SceneRecognitionRegression.class.getName(), ImageDataType.F32.toString()});
        RegressionRunner.main(new String[]{SceneRecognitionRegression.class.getName(), ImageDataType.U8.toString()});
    }
}
