package boofcv.metrics;

import boofcv.abst.scene.ImageRecognition;
import boofcv.abst.scene.nister2006.ConfigImageRecognitionNister2006;
import boofcv.abst.scene.nister2006.ImageRecognitionNister2006;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.factory.feature.describe.ConfigConvertTupleDesc;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs implementations of {@link ImageRecognition} through various benchmarks and prints out the results.
 *
 * @author Peter Abeles
 **/
public class EvaluateImageRetrieval<T extends ImageBase<T>> {
    public static final String DATA_DIR = "data/cbir";

    // Where temp results are stored
    public File workingDirectory = new File("tmp");

    public PrintStream err = System.err;
    public PrintStream outAccuracy = System.out;
    public PrintStream outRuntime = System.out;

    List<Dataset> listOfSets = new ArrayList<>();
    ImageType<T> imageType;

    ImageRetrievalEvaluateResults results = new ImageRetrievalEvaluateResults();

    public EvaluateImageRetrieval(ImageType<T> imageType) {
        this.imageType = imageType;
        listOfSets.add(datasetUkBench80());
        listOfSets.add(inriaHolidays());
    }

    public void printHeaders() {
        outRuntime.printf("%-20s %s %s %s, units=seconds\n", "name", "training", "adding", "lookup");
        results.err = err;
        results.outSummary = outAccuracy;
        results.printSummaryHeader();
    }

    public void evaluate(ImageRecognition<T> target) {
        // Crate working directory if it doesn't exist
        if (!workingDirectory.exists())
            BoofMiscOps.checkTrue(workingDirectory.mkdirs());

        // Configure the generators
        ImageRetrievalGenerateResults<T> generate = new ImageRetrievalGenerateResults<>(imageType);
        generate.err = err;

        // Dump debugging info since this can be slow
        target.setVerbose(System.out, null);

        for (Dataset dataset : listOfSets) {
            File resultsFile = new File(workingDirectory, dataset.name);
            System.out.println("Processing: " + dataset.name);
            generate.process(target, dataset.sets, resultsFile);
            outRuntime.printf("%-20s %7.1f %7.1f %7.1f\n",
                    dataset.name, generate.timeTrainingMS/1000.0, generate.timeAddingMS/1000.0, generate.timeLookUpMS /1000.0);

            // Save the detailed output for future debugging
            try {
                results.outDetailed = new PrintStream(new FileOutputStream(
                        new File(workingDirectory, dataset.name+"_detailed.txt")));
            } catch (FileNotFoundException e) {
                e.printStackTrace(err);
                System.out.println("Exception "+e.getMessage());
                continue;
            }
            results.evaluate(dataset.name, resultsFile, dataset.sets);
        }
        System.out.println("Done!");
    }

    private Dataset datasetUkBench80() {
        Dataset ds = new Dataset();
        ds.name = "ukbench_80";
        ds.sets = new ImageFilesInFixedSets(0, 4, new File(DATA_DIR + "/ukbench_80"));
        return ds;
    }

    private Dataset inriaHolidays() {
        Dataset ds = new Dataset();
        ds.name = "inria_holidays";
        ds.sets = new InriaSets(100, new File(DATA_DIR + "/inria_holidays"));
        return ds;
    }

    private static class Dataset {
        String name;
        ImageRetrievalEvaluationData sets;
    }

    public static void main(String[] args) throws FileNotFoundException {
        EvaluateImageRetrieval<GrayU8> evaluator = new EvaluateImageRetrieval<>(ImageType.SB_U8);
        evaluator.outRuntime = new PrintStream("cbir_runtime.txt");
        evaluator.outAccuracy = new PrintStream("cbir_accuracy.txt");

        ConfigImageRecognitionNister2006 config = new ConfigImageRecognitionNister2006();
//        config.features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SIFT;
//        config.features.typeDetector = ConfigDetectInterestPoint.DetectorType.SIFT;
        config.features.convertDescriptor.outputData = ConfigConvertTupleDesc.DataType.F32;
        config.features.detectFastHessian.maxFeaturesAll = 2000;
        config.features.detectFastHessian.extract.radius = 4;
        config.distanceNorm = RecognitionVocabularyTreeNister2006.DistanceTypes.L2;
        config.maxMatches = 1000;

        evaluator.printHeaders();
        evaluator.evaluate(new ImageRecognitionNister2006<>(config, ImageType.SB_U8));
    }
}
