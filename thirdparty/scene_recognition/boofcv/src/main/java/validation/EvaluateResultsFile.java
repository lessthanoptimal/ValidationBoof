package validation;

import boofcv.io.UtilIO;
import boofcv.metrics.ImageRetrievalEvaluateResults;
import boofcv.metrics.ImageRetrievalEvaluationData;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.List;

/**
 * Commandline application for computing performance metrics from a results file
 *
 * @author Peter Abeles
 */
public class EvaluateResultsFile {
    @Option(name = "-q", aliases = {"--Query"}, usage = "Path to query dataset. Used to sanity check results.")
    String pathToQuery = "";

    @Option(name = "-r", aliases = {"--Results"}, usage = "Path to results file")
    String pathToResults = ".";

    @Option(name = "--QueryFormat", usage = "Specify if 'holidays' or 'ukbench' images are being querried")
    String queryFormat = "holidays";


    public void evaluate() {
        List<String> queryList = UtilIO.listSmart(pathToQuery, true, (f) -> true);
        if (queryList.isEmpty()) {
            System.err.println("No images found");
            System.exit(1);
        }

        ImageRetrievalEvaluationData set = SceneRecognitionUtils.evaluateByFormat(queryFormat,null,null,queryList);
        ImageRetrievalEvaluateResults evaluate = new ImageRetrievalEvaluateResults();
        evaluate.evaluate("HmmName", new File(pathToResults), set);
    }

    private static void printHelpExit(CmdLineParser parser) {
        parser.getProperties().withUsageWidth(120);
        parser.printUsage(System.out);

        System.exit(1);
    }

    public static void main(String[] args) {
        EvaluateResultsFile generator = new EvaluateResultsFile();
        CmdLineParser parser = new CmdLineParser(generator);

        if (args.length == 0) {
            printHelpExit(parser);
        }

        try {
            parser.parseArgument(args);
            generator.evaluate();
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printHelpExit(parser);
        }
    }
}
