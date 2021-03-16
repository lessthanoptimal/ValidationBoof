package validation;

import boofcv.io.UtilIO;
import boofcv.metrics.ImageRetrievalEvaluateResults;
import boofcv.metrics.ImageRetrievalEvaluationData;

import java.io.File;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateResultsForIPOL2018 {
    public static ImageRetrievalEvaluationData foo(File queryImages) {
        List<String> all = UtilIO.listImages(queryImages.getPath(), true);

        return new ImageRetrievalEvaluationData() {
            @Override
            public List<String> getTraining() {throw new RuntimeException("Bad");}

            @Override
            public List<String> getDataBase() {throw new RuntimeException("Bad");}

            @Override
            public List<String> getQuery() {return all;}

            @Override
            public boolean isMatch(int query, int dataset) {
                return (query/4)==(dataset/4);
            }

            @Override
            public int getTotalMatches(int query) {
                return 4;
            }
        };
    }

    public static void main(String[] args) {
        System.out.println("args.length="+args.length);
        String resultsFile = args.length > 0 ? args[0] : "results.csv";
        System.out.println("input data path: "+resultsFile);

        ImageRetrievalEvaluateResults evaluate = new ImageRetrievalEvaluateResults();
        evaluate.evaluate("IPOL2018",new File(resultsFile),foo(new File("/home/pja/projects/datasets/data/full/")));
    }
}
