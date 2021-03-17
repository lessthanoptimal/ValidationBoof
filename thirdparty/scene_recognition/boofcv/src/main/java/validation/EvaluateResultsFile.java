package validation;

import boofcv.io.UtilIO;
import boofcv.metrics.ImageRetrievalEvaluateResults;
import boofcv.metrics.ImageRetrievalEvaluationData;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray_I32;

import java.io.File;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateResultsFile {
    public static ImageRetrievalEvaluationData holidaysDONOTCOPYCODE(List<String> query) {
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
            @Override public List<String> getTraining() {throw new IllegalArgumentException("Bad");}
            @Override public List<String> getDataBase() {throw new IllegalArgumentException("Bad");}
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
    }
    public static void main(String[] args) {
        System.out.println("args.length="+args.length);
        String directoryData = args.length > 0 ? args[0] : "data";
        System.out.println("input data path: "+directoryData);

        List<String> queryList = UtilIO.listSmart(directoryData, true, (f) -> true);
        if (queryList.isEmpty()) {
            System.err.println("No images found");
            System.exit(1);
        }

        ImageRetrievalEvaluateResults evaluate = new ImageRetrievalEvaluateResults();
//        evaluate.err = utils.err;
//        evaluate.outSummary = new PrintStream(new MirrorStream(System.out, resultsSummary));
//        evaluate.outDetailed = resultsDetailed;
        evaluate.evaluate("IPOL",new File("/home/pja/projects/ValidationBoof/thirdparty/scene_recognition/ipol/ipol_results.csv"), holidaysDONOTCOPYCODE(queryList));
    }
}
