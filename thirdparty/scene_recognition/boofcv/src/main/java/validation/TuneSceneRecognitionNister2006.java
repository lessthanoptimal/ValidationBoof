package validation;

import boofcv.abst.scene.nister2006.ConfigImageRecognitionNister2006;
import boofcv.io.MirrorStream;
import boofcv.io.UtilIO;
import boofcv.metrics.ImageRetrievalEvaluateResults;
import boofcv.metrics.ImageRetrievalEvaluationData;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigGeneratorGrid;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray_I32;
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
 * Tool for finding the optimal set of parameters.
 *
 * @author Peter Abeles
 */
public class TuneSceneRecognitionNister2006 {
    @Option(name = "-t", aliases = {"--Training"}, usage = "Path to training dataset")
    String pathToTraining = "";
    @Option(name = "-q", aliases = {"--Query"}, usage = "Path to query dataset. If blank, assumed to be query.")
    String pathToQuery = "";
    @Option(name = "-d", aliases = {"--Distract"}, usage = "Path to images to use as distractors. 'glob:' and 'regex:' allowed")
    String pathToDistractors = "";
    @Option(name = "-o", aliases = {"--Output"}, usage = "Output directory where results should be saved to")
    String pathToResults = ".";
    @Option(name = "--ConfigPath", usage = "Which config should it use as the baseline")
    String pathToConfig = "";
    @Option(name = "--QueryFormat", usage = "Specify if 'holidays' or 'ukbench' images are being querried")
    String queryFormat = "holidays";

    ConfigGeneratorGrid<ConfigImageRecognitionNister2006> generator;

    public void searchGridTreeParams() {
        generator = new ConfigGeneratorGrid<>(0xDEADBEEF, ConfigImageRecognitionNister2006.class);

        generator.rangeOfIntegers("tree.branchFactor", 8, 32);
        generator.rangeOfIntegers("tree.maximumLevel", 2, 7);
        generator.setDiscretizationRule("tree.branchFactor", ConfigGeneratorGrid.Discretization.INTEGER_VALUES);
        generator.setDiscretizationRule("tree.maximumLevel", ConfigGeneratorGrid.Discretization.INTEGER_VALUES);

        generator.initialize();

        // See if the user wants to override the default base config
        if (!pathToConfig.isEmpty()) {
            ConfigImageRecognitionNister2006 canonical = UtilIO.loadConfig(new File(pathToConfig));
            generator.getConfigurationBase().setTo(canonical);
        }

        // Evaluate on these two smaller datasets
        ImageRetrievalEvaluationData dataset = createDataset();

        File directoryBase = new File(pathToResults);
        BoofMiscOps.checkTrue(directoryBase.mkdirs(),"Output already exists: "+directoryBase.getAbsolutePath());
        try {
            FileUtils.write(new File(directoryBase, "grid_settings.txt"), generator.toStringSettings(), StandardCharsets.UTF_8);

            int digits = BoofMiscOps.numDigits(generator.getNumTrials());
            while (generator.hasNext()) {
                ConfigImageRecognitionNister2006 config = generator.next();
                System.out.println("Grid trial " + generator.getTrial());

                evaluate(directoryBase, String.format("%0" + digits + "d", generator.getTrial()), dataset, config);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void evaluate(File outputDir, String trialDir,
                          ImageRetrievalEvaluationData dataset,
                          ConfigImageRecognitionNister2006 config) throws IOException {
        BoofMiscOps.checkTrue(new File(outputDir, trialDir).mkdirs());

        // Save grid parameter values
        FileUtils.write(new File(outputDir, trialDir + "/grid_state.txt"), generator.toStringState(), StandardCharsets.UTF_8);

        ImageRecognitionUtils.ModelInfo model = new ImageRecognitionUtils.ModelInfo(trialDir, config);

        try (PrintStream printErr = new PrintStream(new File(outputDir, trialDir + "/log_stderr.txt"));
             PrintStream printOut = new PrintStream(new File(outputDir, trialDir + "/log_stdout.txt"))) {
            long time0 = System.currentTimeMillis();
            ImageRecognitionUtils<GrayU8> utils = new ImageRecognitionUtils<>(ImageType.SB_U8);
            utils.pathHome = outputDir;
            utils.err = new PrintStream(new MirrorStream(System.err, printErr));
            utils.out = new PrintStream(new MirrorStream(System.out, printOut));

            System.out.println("Creating model...");
            if (!utils.createAndSave(model, dataset.getTraining()))
                return;
            System.out.println("Adding images...");
            if (!utils.addImagesToDataBase(model.name, dataset.getDataBase()))
                return;
            System.out.println("Classifying images...");
            if (!utils.classify(model.name, dataset.getQuery()))
                return;

            // Generic performance metrics and save to disk
            System.out.println("Evaluating classifications...");
            ImageRetrievalEvaluateResults evaluate = new ImageRetrievalEvaluateResults();
            evaluate.outSummary = new PrintStream(new File(outputDir, trialDir + "/summary.txt"));
            evaluate.outDetailed = new PrintStream(new File(outputDir, trialDir + "/detailed.txt"));
            if (!evaluate.evaluate("images", new File(outputDir, trialDir + "/results.csv"), dataset))
                return;

            long time1 = System.currentTimeMillis();

            // Make a very easy to read file with a single number that can be used to compare results
            FileUtils.write(new File(outputDir, trialDir + "/score.txt"), "" + evaluate.score, StandardCharsets.UTF_8);

            // Save how long it took to process and the start/stoptime
            FileUtils.write(new File(outputDir, trialDir + "/elapsed_time.txt"),
                    "" + (time1 - time0) + " (ms) " + BoofMiscOps.milliToHuman(time1 - time0) + "\n" +
                            BoofMiscOps.timeStr(time0) + "\n" + BoofMiscOps.timeStr(time1) + "\n", StandardCharsets.UTF_8);

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
        List<String> distractors = UtilIO.listSmart(pathToDistractors, true, (f) -> true);
        List<String> all = new ArrayList<>(query.size() + distractors.size());
        all.addAll(query);
        all.addAll(distractors);

        System.out.println("training.size=" + training.size());
        System.out.println("query.size=" + query.size());
        System.out.println("distractors.size=" + distractors.size());

        if (queryFormat.equalsIgnoreCase("ukbench")) {
            // @formatter:off
            return new ImageRetrievalEvaluationData() {
                @Override public List<String> getTraining() {return training;}
                @Override public List<String> getDataBase() {return all;}
                @Override public List<String> getQuery() {return query;}
                @Override public boolean isMatch(int query, int dataset) {return (query/4)==(dataset/4);}
                @Override public int getTotalMatches(int query) {return 4;}
            };
            // @formatter:on
        } else if (queryFormat.equalsIgnoreCase("holidays")) {
            // Inria files indicate which images are related based on the file name, which is a number.
            final int divisor = 100;
            int lastNumber = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(query.size() - 1)).getName()));
            int totalSets = lastNumber / divisor + 1;

            // Number of matches each query has in the database
            DogArray_I32 setCounts = new DogArray_I32();
            setCounts.resize(totalSets);

            // precompute number of membership in each set
            for (int i = 0; i < query.size(); i++) {
                int number = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(i)).getName()));
                setCounts.data[number / divisor]++;
            }

            // @formatter:off
            return new ImageRetrievalEvaluationData() {
                @Override public List<String> getTraining() {return training;}
                @Override public List<String> getDataBase() {return all;}
                @Override public List<String> getQuery() {return query;}
                @Override public boolean isMatch(int queryID, int datasetID) {
                    // query images are first in the list
                    if (datasetID>=query.size())
                        return false;
                    int numberQuery = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(queryID)).getName()));
                    int numberDataset = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(datasetID)).getName()));
                    return numberQuery/divisor == numberDataset/divisor;
                }
                @Override public int getTotalMatches(int queryID) {
                    int number = Integer.parseInt(FilenameUtils.getBaseName(new File(query.get(queryID)).getName()));
                    return setCounts.get(number/divisor);
                }
            };
            // @formatter:on
        } else {
            throw new IllegalArgumentException("Unknown query format: " + queryFormat);
        }
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
            generator.searchGridTreeParams();
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        }
    }
}
