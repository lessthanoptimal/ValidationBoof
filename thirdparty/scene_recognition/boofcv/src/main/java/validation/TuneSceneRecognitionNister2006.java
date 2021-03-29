package validation;

import boofcv.abst.scene.ConfigFeatureToSceneRecognition;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.io.MirrorStream;
import boofcv.io.UtilIO;
import boofcv.metrics.ImageRetrievalEvaluateResults;
import boofcv.metrics.ImageRetrievalEvaluationData;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigGenerator;
import boofcv.struct.ConfigGeneratorGrid;
import boofcv.struct.ConfigGeneratorRandom;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool for finding the optimal set of parameters for Nister2006.
 *
 * @author Peter Abeles
 */
public class TuneSceneRecognitionNister2006 {
    @Option(name = "-t", aliases = {"--Training"}, usage = "Path to training dataset. 'glob:' and 'regex:' allowed")
    String pathToTraining = "";
    @Option(name = "-q", aliases = {"--Query"},
            usage = "Path to query dataset. If blank, assumed to be training. 'glob:' and 'regex:' allowed")
    String pathToQuery = "";
    @Option(name = "-d", aliases = {"--Distract"},
            usage = "Path to images to use as distractors. 'glob:' and 'regex:' allowed")
    String pathToDistractors = "";
    @Option(name = "-o", aliases = {"--Output"}, usage = "Output directory where results should be saved to")
    String pathToResults = ".";
    @Option(name = "--ConfigPath", usage = "Which config should it use as the baseline")
    String pathToConfig = "";
    @Option(name = "--QueryFormat", usage = "Specify if 'holidays' or 'ukbench' images are being queried")
    String queryFormat = "holidays";
    @Option(name = "--TrainingFraction", usage = "Use a fraction of the 'training' dataset to train")
    double trainingFraction = 1.0;
    @Option(name = "--Trials", usage = "Number of random trials to perform, if applicable")
    int numRandomTrials = 400;
    @Option(name = "--Task", usage = "Which task should it run. TREE_STRUCTURE, SURF, SIFT, MINIMUM_DEPTH")
    String taskName = Task.TREE_STRUCTURE.name();

    public void searchGridTreeParams() {
        var generator = new ConfigGeneratorGrid<>(0xDEADBEEF, ConfigFeatureToSceneRecognition.class);

        generator.rangeOfIntegers("recognizeNister2006.tree.branchFactor", 8, 32);
        generator.rangeOfIntegers("recognizeNister2006.tree.maximumLevel", 2, 7);
        generator.setDiscretizationRule("recognizeNister2006.tree.branchFactor", ConfigGeneratorGrid.Discretization.INTEGER_VALUES);
        generator.setDiscretizationRule("recognizeNister2006.tree.maximumLevel", ConfigGeneratorGrid.Discretization.INTEGER_VALUES);

        generator.initialize();

        // See if the user wants to override the default base config
        if (!pathToConfig.isEmpty()) {
            ConfigFeatureToSceneRecognition canonical = UtilIO.loadConfig(new File(pathToConfig));
            generator.getConfigurationBase().setTo(canonical);
        }

        // Forcing this to be zero to avoid biasing it towards trees with more depth
        generator.getConfigurationBase().recognizeNister2006.minimumDepthFromRoot = 0;
        // This is intended to make queries with large number of images run MUCH faster but can degrade
        // performance potentially. Without it the speed is untenable for a large study.
        generator.getConfigurationBase().recognizeNister2006.queryMaximumImagesInNode.setRelative(0.01, 10_000);

        performParameterSearch(generator, false);
    }

    private void performParameterSearch(ConfigGenerator<ConfigFeatureToSceneRecognition> generator, boolean runBaseConfig) {
        // Evaluate on these two smaller datasets
        ImageRetrievalEvaluationData dataset = createDataset();

        File directoryBase = new File(pathToResults);
        BoofMiscOps.checkTrue(directoryBase.mkdirs(), "Output already exists: " + directoryBase.getAbsolutePath());
        try {
            FileUtils.write(new File(directoryBase, "tuning_settings.txt"), generator.toStringSettings(), StandardCharsets.UTF_8);
            FileUtils.write(new File(directoryBase, "application_arguments.txt"), argumentsToString(), StandardCharsets.UTF_8);

            // If requested, run the base config first so we can see if things are actually better
            if (runBaseConfig) {
                String trialDir = "base";
                BoofMiscOps.checkTrue(new File(directoryBase, trialDir).mkdirs());
                evaluate(directoryBase, trialDir, dataset, generator.getConfigurationBase());
            }

            // Go through all the generated configurations and save the results
            int digits = BoofMiscOps.numDigits(generator.getNumTrials());
            while (generator.hasNext()) {
                ConfigFeatureToSceneRecognition config = generator.next();
                System.out.println("Grid trial " + generator.getTrial());

                String trialDir = String.format("%0" + digits + "d", generator.getTrial());
                BoofMiscOps.checkTrue(new File(directoryBase, trialDir).mkdirs());
                FileUtils.write(new File(directoryBase, trialDir + "/generator_state.txt"), generator.toStringState(), StandardCharsets.UTF_8);

                evaluate(directoryBase, trialDir, dataset, config);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void searchMinimumDepth() {
        var generator = new ConfigGeneratorGrid<>(0xDEADBEEF, ConfigFeatureToSceneRecognition.class);

        // This really should be a dynamic distribution based on maximumLevel
        generator.rangeOfIntegers("minimumDepthFromRoot", 0, 4);

        generator.initialize();

        // See if the user wants to override the default base config
        if (!pathToConfig.isEmpty()) {
            ConfigFeatureToSceneRecognition canonical = UtilIO.loadConfig(new File(pathToConfig));
            generator.getConfigurationBase().setTo(canonical);
        }

        // This is intended to make queries with large number of images run MUCH faster but can degrade
        // performance potentially. Without it the speed is untenable for a large study.
        generator.getConfigurationBase().recognizeNister2006.queryMaximumImagesInNode.setRelative(0.01, 10_000);

        performParameterSearch(generator, false);
    }

    public void searchSurfParams() {
        var generator = new ConfigGeneratorRandom<>(numRandomTrials, 0xDEADBEEF, ConfigFeatureToSceneRecognition.class);
        generator.rangeOfIntegers("features.detectFastHessian.maxFeaturesPerScale", 100, 2000);
        generator.rangeOfFloats("features.detectFastHessian.extract.threshold", 0.0, 2.0);
        generator.rangeOfIntegers("features.detectFastHessian.extract.radius", 1, 10);
        generator.rangeOfIntegers("features.detectFastHessian.numberOfOctaves", 1, 6);
        generator.rangeOfFloats("features.describeSurfStability.widthSample", 1, 6);

        generator.initialize();

        // See if the user wants to override the default base config
        if (!pathToConfig.isEmpty()) {
            ConfigFeatureToSceneRecognition canonical = UtilIO.loadConfig(new File(pathToConfig));
            generator.getConfigurationBase().setTo(canonical);
        }

        // make sure it's configured for SURF
        generator.getConfigurationBase().features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
        generator.getConfigurationBase().features.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
        // Forcing this to be zero to avoid biasing it towards trees with more depth
        generator.getConfigurationBase().recognizeNister2006.minimumDepthFromRoot = 0;
        // This is intended to make queries with large number of images run MUCH faster but can degrade
        // performance potentially. Without it the speed is untenable for a large study.
        generator.getConfigurationBase().recognizeNister2006.queryMaximumImagesInNode.setRelative(0.01, 10_000);

        performParameterSearch(generator, true);
    }

    public void searchSiftParams() {
        var generator = new ConfigGeneratorRandom<>(numRandomTrials, 0xDEADBEEF, ConfigFeatureToSceneRecognition.class);
        generator.rangeOfIntegers("features.detectSift.maxFeaturesPerScale", 200, 2000);
        generator.rangeOfFloats("features.detectSift.extract.threshold", 0.0, 2.0);
        generator.rangeOfFloats("features.detectSift.edgeR", 2.0, 20.0);
        generator.rangeOfIntegers("features.detectSift.extract.radius", 1, 10);
        generator.rangeOfFloats("features.scaleSpaceSift.sigma0", 0.5, 5.0);
        generator.rangeOfIntegers("features.scaleSpaceSift.lastOctave", 2, 7);
        generator.rangeOfFloats("features.describeSift.sigmaToPixels", 0.25, 5.0);

        generator.initialize();

        // See if the user wants to override the default base config
        if (!pathToConfig.isEmpty()) {
            ConfigFeatureToSceneRecognition canonical = UtilIO.loadConfig(new File(pathToConfig));
            generator.getConfigurationBase().setTo(canonical);
        }

        // make sure it's configured for SIFT
        generator.getConfigurationBase().features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SIFT;
        generator.getConfigurationBase().features.typeDetector = ConfigDetectInterestPoint.DetectorType.SIFT;
        // Forcing this to be zero to avoid biasing it towards trees with more depth
        generator.getConfigurationBase().recognizeNister2006.minimumDepthFromRoot = 0;
        // This is intended to make queries with large number of images run MUCH faster but can degrade
        // performance potentially. Without it the speed is untenable for a large study.
        generator.getConfigurationBase().recognizeNister2006.queryMaximumImagesInNode.setRelative(0.01, 10_000);

        performParameterSearch(generator, true);
    }

    private void evaluate(File outputDir, String trialDir,
                          ImageRetrievalEvaluationData dataset,
                          ConfigFeatureToSceneRecognition config) throws IOException {
        SceneRecognitionUtils.ModelInfo model = new SceneRecognitionUtils.ModelInfo(trialDir, config);

        try (PrintStream printErr = new PrintStream(new File(outputDir, trialDir + "/log_stderr.txt"));
             PrintStream printOut = new PrintStream(new File(outputDir, trialDir + "/log_stdout.txt"))) {
            long time0 = System.currentTimeMillis();
            SceneRecognitionUtils<GrayU8> utils = new SceneRecognitionUtils<>(ImageType.SB_U8);
            utils.pathHome = outputDir;
            utils.err = new PrintStream(new MirrorStream(System.err, printErr));
            utils.out = new PrintStream(new MirrorStream(System.out, printOut));

            utils.out.println("training.size="+dataset.getTraining().size());
            utils.out.println("database.size="+dataset.getDataBase().size());
            utils.out.println("query.size="+dataset.getQuery().size());
            utils.out.println();
            utils.out.println("Creating model...");
            if (!utils.createAndSave(model, dataset.getTraining()))
                return;
            long elapsedTraining = utils.elapsedTimeMS;
            utils.out.println("Adding images...");
            if (!utils.addImagesToDataBase(model.name, dataset.getDataBase()))
                return;
            long elapsedDB = utils.elapsedTimeMS;
            utils.out.println("Classifying images...");
            if (!utils.classify(model.name, dataset.getQuery()))
                return;
            long elapsedClassifying = utils.elapsedTimeMS;

            // Generic performance metrics and save to disk
            utils.out.println("Evaluating classifications...");
            ImageRetrievalEvaluateResults evaluate = new ImageRetrievalEvaluateResults();
            evaluate.outSummary = new PrintStream(new File(outputDir, trialDir + "/summary.txt"));
            evaluate.outDetailed = new PrintStream(new File(outputDir, trialDir + "/detailed.txt"));
            evaluate.printSummaryHeader();
            if (!evaluate.evaluate("images", new File(outputDir, trialDir + "/results.csv"), dataset))
                return;

            long time1 = System.currentTimeMillis();

            // Make a very easy to read file with a single number that can be used to compare results
            FileUtils.write(new File(outputDir, trialDir + "/score.txt"), "" + evaluate.score + "\n", StandardCharsets.UTF_8);

            // Save how long it took to process and the start/stop time
            FileUtils.write(new File(outputDir, trialDir + "/time.txt"),
                    "" + (time1 - time0) + " (ms) " + BoofMiscOps.milliToHuman(time1 - time0) + "\n" +
                            BoofMiscOps.timeStr(time0) + "\n" + BoofMiscOps.timeStr(time1) + "\n" +
                            String.format("training %.1f (s)\n", elapsedTraining * 1e-3) +
                            String.format("database %.1f (s)\n", elapsedDB * 1e-3) +
                            String.format("classifying %.1f (s)\n", elapsedClassifying * 1e-3)
                    , StandardCharsets.UTF_8);

            evaluate.outSummary.close();
            evaluate.outDetailed.close();

            // Free up disk space
            utils.deleteModels(model.name);
        }
    }

    /**
     * Creates an evaluation dataset given the command line arguments
     */
    private ImageRetrievalEvaluationData createDataset() {
        List<String> training = UtilIO.listSmart(pathToTraining, true, (f) -> true);
        BoofMiscOps.checkTrue(!training.isEmpty(), "Training is empty. Check path");
        List<String> query;
        if (pathToQuery.isEmpty())
            query = training;
        else
            query = UtilIO.listSmart(pathToQuery, true, (f) -> true);
        BoofMiscOps.checkTrue(!query.isEmpty(), "query is empty. Check path");

        // Prune some of the training dataset, if requested
        if (trainingFraction < 1.0) {
            training = training.subList(0, (int) (trainingFraction * training.size()));
        }

        List<String> distractors = UtilIO.listSmart(pathToDistractors, true, (f) -> true);
        List<String> all = new ArrayList<>(query.size() + distractors.size());
        all.addAll(query);
        all.addAll(distractors);

        System.out.println("training.size=" + training.size());
        System.out.println("query.size=" + query.size());
        System.out.println("distractors.size=" + distractors.size());
        // pause for a second so you can read these numbers
        BoofMiscOps.sleep(10_000);

        return SceneRecognitionUtils.evaluateByFormat(queryFormat, training, all, query);
    }

    private String argumentsToString() {
        String text = "";
        text += "pathToTraining,"+pathToTraining+"\n";
        text += "pathToQuery,"+pathToQuery+"\n";
        text += "pathToDistractors,"+pathToDistractors+"\n";
        text += "pathToResults,"+pathToResults+"\n";
        text += "pathToConfig,"+pathToConfig+"\n";
        text += "queryFormat,"+queryFormat+"\n";
        text += "trainingFraction,"+trainingFraction+"\n";
        text += "numRandomTrials,"+numRandomTrials+"\n";
        text += "taskName,"+taskName+"\n";
        return text;
    }

    /**
     * Tune which parameters
     */
    enum Task {
        TREE_STRUCTURE,
        MINIMUM_DEPTH,
        SURF,
        SIFT,
    }

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    public static void main(String[] args) {
        TuneSceneRecognitionNister2006 generator = new TuneSceneRecognitionNister2006();
        CmdLineParser parser = new CmdLineParser(generator);

        if (args.length == 0) {
            printHelpExit(parser);
        }

        try {
            parser.parseArgument(args);

            // Fuzzy logic for selecting the task
            Task task = null;
            double bestScore = 0;
            for (Task t : Task.values()) {
                double score = BoofMiscOps.similarity(t.name(), generator.taskName);
                if (score > bestScore) {
                    bestScore = score;
                    task = t;
                }
            }
            if (task == null)
                throw new RuntimeException("No task selected?");
            System.out.println("Task: "+task);
            switch (task) {
                case TREE_STRUCTURE: generator.searchGridTreeParams(); break;
                case SURF: generator.searchSurfParams(); break;
                case SIFT: generator.searchSiftParams(); break;
                case MINIMUM_DEPTH: generator.searchMinimumDepth(); break;
                default: throw new RuntimeException("Not yet implemented. " + task);
            }
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        }
    }
}
