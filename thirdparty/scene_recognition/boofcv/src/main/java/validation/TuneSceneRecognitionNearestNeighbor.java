package validation;

import boofcv.abst.scene.ConfigFeatureToSceneRecognition;
import boofcv.alg.scene.bow.BowDistanceTypes;
import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigGeneratorGrid;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;

/**
 * Tool for finding the optimal set of parameters for Nister2006.
 *
 * @author Peter Abeles
 */
public class TuneSceneRecognitionNearestNeighbor extends TuneSceneRecognition {
    @Option(name = "--Task", usage = "Which task should it run. DICTIONARY, SURF_RND, SIFT_RND, SURF_VEC, SIFT_VEC, MINIMUM_DEPTH")
    String taskName = Task.DICTIONARY.name();

    public void searchDictionaryGrid() {
        ConfigGeneratorGrid<ConfigFeatureToSceneRecognition> generator =
                new ConfigGeneratorGrid<>(0xDEADBEEF, ConfigFeatureToSceneRecognition.class);

        generator.setRangeDiscretization(20);
        generator.rangeOfIntegers("recognizeNeighbor.numberOfWords", 1000, 100_000);
        generator.setOfEnums("recognizeNeighbor.distanceNorm", BowDistanceTypes.values());

        generator.initialize();
        generator.getConfigurationBase().typeRecognize = ConfigFeatureToSceneRecognition.Type.NEAREST_NEIGHBOR;


        // See if the user wants to override the default base config
        if (!pathToConfig.isEmpty()) {
            ConfigFeatureToSceneRecognition canonical = UtilIO.loadConfig(new File(pathToConfig));
            generator.getConfigurationBase().setTo(canonical);
        }

        performParameterSearch(generator, true);
    }

    @Override
    protected String argumentsToString() {
        String text = super.argumentsToString();
        text += "taskName,"+taskName+"\n";
        return text;
    }

    /**
     * Tune which parameters
     */
    enum Task {
        DICTIONARY,
        MINIMUM_DEPTH,
        SURF_RND,
        SIFT_RND,
        SURF_VEC,
        SIFT_VEC,
    }

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);
        System.exit(1);
    }

    public static void main(String[] args) {
        TuneSceneRecognitionNearestNeighbor generator = new TuneSceneRecognitionNearestNeighbor();
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
                case DICTIONARY: generator.searchDictionaryGrid(); break;
                default: throw new RuntimeException("Not yet implemented. " + task);
            }
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        }
    }
}
