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
import java.util.List;

/**
 * @author Peter Abeles
 */
public class TuneImageRecognitionNister2006 {

    @Option(name = "-i", aliases = {"--Input"}, usage = "Input directory with all the input image datasets")
    String pathToData = "";
    @Option(name = "-o", aliases = {"--Output"}, usage = "Output directory where results should be saved to")
    String pathToResults = ".";

    ConfigGeneratorGrid<ConfigImageRecognitionNister2006> generator;

    public void searchGridTreeParams() {
        generator = new ConfigGeneratorGrid<>(0xDEADBEEF, ConfigImageRecognitionNister2006.class);

        generator.rangeOfIntegers("tree.branchFactor", 8, 32);
        generator.rangeOfIntegers("tree.maximumLevel", 2, 7);
        generator.setDiscretizationRule("tree.branchFactor", ConfigGeneratorGrid.Discretization.INTEGER_VALUES);
        generator.setDiscretizationRule("tree.maximumLevel", ConfigGeneratorGrid.Discretization.INTEGER_VALUES);

        generator.initialize();
        generator.getConfigBase().features.detectFastHessian.maxFeaturesAll = 500;
        generator.getConfigBase().features.detectFastHessian.maxFeaturesPerScale = 0;

        // Evaluate on these two smaller datasets
        ImageRetrievalEvaluationData ukbench = datasetUkBench1000();
        ImageRetrievalEvaluationData inria = datasetInria();

        File directoryBase = new File(pathToResults, "grid_tree");
        BoofMiscOps.checkTrue(directoryBase.mkdirs());
        try {
            FileUtils.write(new File(directoryBase, "grid_settings.txt"), generator.toStringSettings(), StandardCharsets.UTF_8);

            File dirUkbench = new File(directoryBase, "ukbench1000");
            File dirInria = new File(directoryBase, "inria");

            int digits = BoofMiscOps.numDigits(generator.getNumTrials());
            while (generator.hasNext()) {
                ConfigImageRecognitionNister2006 config = generator.next();
                System.out.println("Grid trial " + generator.getTrial());

                evaluate(dirUkbench, String.format("%0" + digits + "d", generator.getTrial()), ukbench, config);
                evaluate(dirInria, String.format("%0" + digits + "d", generator.getTrial()), inria, config);
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
            if (!utils.addImagesToDataBase(model.name, "images", dataset.getDataBase()))
                return;
            System.out.println("Classifying images...");
            if (!utils.classify(model.name, "images", dataset.getQuery()))
                return;

            // Generic performance metrics and save to disk
            System.out.println("Evaluating classifications...");
            ImageRetrievalEvaluateResults evaluate = new ImageRetrievalEvaluateResults();
            evaluate.outSummary = new PrintStream(new File(outputDir, trialDir + "/summary.txt"));
            evaluate.outDetailed = new PrintStream(new File(outputDir, trialDir + "/detailed.txt"));
            if (!evaluate.evaluate("images", new File(outputDir, trialDir + "/images_results.csv"), dataset))
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
        }
    }


    /**
     * First 1000 images from ukbench dataset.
     * <p>
     * TODO link to source
     */
    private ImageRetrievalEvaluationData datasetUkBench1000() {
        List<String> all = UtilIO.listSmart(
                new File("glob:" + pathToData, "ukbench1000/ukbench*jpg").getPath(), true, (f) -> true);

        if (all.isEmpty())
            throw new RuntimeException("ukbench1000 is empty. Is path correct?");

        // @formatter:off
        return new ImageRetrievalEvaluationData() {
            @Override public List<String> getTraining() {return all;}
            @Override public List<String> getDataBase() {return getTraining();}
            @Override public List<String> getQuery() {return getTraining();}
            @Override public boolean isMatch(int query, int dataset) {return (query/4)==(dataset/4);}
            @Override public int getTotalMatches(int query) {return 4;}
        };
        // @formatter:on
    }

    /**
     * The two INRIA datasets combined. Should be 813+680=1494 images
     * <p>
     * TODO link to source
     */
    private ImageRetrievalEvaluationData datasetInria() {
        List<String> all = UtilIO.listSmart(
                new File("glob:" + pathToData, "inria/*jpg").getPath(), true, (f) -> true);

        if (all.isEmpty())
            throw new RuntimeException("inria is empty. Is path correct?");

        // Inria files indicate which images are related based on the file name, which is a number.
        final int divisor = 100;
        int lastNumber = Integer.parseInt(FilenameUtils.getBaseName(new File(all.get(all.size() - 1)).getName()));
        int totalSets = lastNumber / divisor + 1;

        // Number of matches each query has in the database
        DogArray_I32 setCounts = new DogArray_I32();
        setCounts.resize(totalSets);

        // precompute number of membership in each set
        for (int i = 0; i < all.size(); i++) {
            int number = Integer.parseInt(FilenameUtils.getBaseName(new File(all.get(i)).getName()));
            setCounts.data[number / divisor]++;
        }

        // @formatter:off
        return new ImageRetrievalEvaluationData() {
            @Override public List<String> getTraining() {return all;}
            @Override public List<String> getDataBase() {return getTraining();}
            @Override public List<String> getQuery() {return getTraining();}
            @Override public boolean isMatch(int query, int dataset) {
                int numberQuery = Integer.parseInt(FilenameUtils.getBaseName(new File(all.get(query)).getName()));
                int numberDataset = Integer.parseInt(FilenameUtils.getBaseName(new File(all.get(dataset)).getName()));
                return numberQuery/divisor == numberDataset/divisor;
            }
            @Override public int getTotalMatches(int query) {
                int number = Integer.parseInt(FilenameUtils.getBaseName(new File(all.get(query)).getName()));
                return setCounts.get(number/divisor);
            }
        };
        // @formatter:on
    }

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);

        System.exit(1);
    }

    public static void main(String[] args) {
        TuneImageRecognitionNister2006 generator = new TuneImageRecognitionNister2006();
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
