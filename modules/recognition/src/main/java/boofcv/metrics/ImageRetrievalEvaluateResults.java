package boofcv.metrics;

import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.DogArray_F64;

import java.io.*;
import java.util.List;

/**
 * Given previously saved results, compute Mean Average Precision (mAP).
 *
 * @author Peter Abeles
 **/
public class ImageRetrievalEvaluateResults {
    // Where errors are sent to
    public PrintStream err = System.err;

    // Where detailed results are sent to
    public PrintStream outDetailed = System.out;
    // Summary results are saved here
    public PrintStream outSummary = System.out;

    // A single number that summarizes all the results
    public double score;

    public void printSummaryHeader() {
        outSummary.println("# Performance based on average precision statistics.");
        outSummary.println("# mAP = mean average precision, xAP = x% percentile average precision");
        outSummary.println("# of queries considered. 100 = first 100 queries");
        outSummary.println("# file_name, count, mAP, 1AP, 2AP, 5AP, 50AP, 95AP");
    }

    public boolean evaluate(String testName, File resultsFile, ImageRetrievalEvaluationData sets) {
        outDetailed.println("# Image Retrieval Detailed Results for " + testName);
        outDetailed.println("# Average Precision is computed for each image and saved here");

        List<String> queryList = sets.getQuery();

        int totalResults = 0;
        double sumAveragePrecision = 0.0;
        DogArray_F64 allAveragePrecision = new DogArray_F64();

        try {
            InputStream input = new FileInputStream(resultsFile);
            StringBuilder buffer = new StringBuilder(1024);

            while (true) {
                UtilIO.readLine(input, buffer);
                if (buffer.length()==0)
                    break;
                String line = buffer.toString();
                if (line.startsWith("#"))
                    continue;
                String[] words = line.split(",");
                int queryIndex = Integer.parseInt(words[0]);
                String queryName = words[1];
                BoofMiscOps.checkTrue(queryName.equals(queryList.get(queryIndex)), "Index and name do not match");
                int numResults = Integer.parseInt(words[2]);
                BoofMiscOps.checkEq(numResults, words.length - 3, "Unexpected number of results");

                // Maximum number of possible matches
                int totalMatches = sets.getTotalMatches(queryIndex);
                // How the number of relevant entries have been seen by index 'i'
                int totalRelevant = 0;

                // Precision at different number of results
                double averagePrecision = 0.0;

                for (int i = 0; i < numResults; i++) {
                    int result = Integer.parseInt(words[i + 3]);
                    if (sets.isMatch(queryIndex, result)) {
                        totalRelevant++;
                        double precision = totalRelevant / (double)(i + 1);
                        averagePrecision += precision;
                    }
                }
                // Make sure there aren't more relevant matches than possible!
                BoofMiscOps.checkTrue(totalRelevant<=totalMatches);

                averagePrecision /= totalMatches;

                outDetailed.printf("%-30s %7.4f\n", queryName, averagePrecision);

                allAveragePrecision.add(averagePrecision);
                sumAveragePrecision += averagePrecision;
                totalResults++;
            }
        } catch (IOException e) {
            e.printStackTrace(err);
            e.printStackTrace();
            return false;
        }
        outDetailed.flush();

        allAveragePrecision.sort();

        // median Average Prevision
        outSummary.printf("%-30s %7d %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f\n",
                testName, totalResults,
                sumAveragePrecision/totalResults,
                allAveragePrecision.getFraction(0.01),
                allAveragePrecision.getFraction(0.02),
                allAveragePrecision.getFraction(0.05),
                allAveragePrecision.getFraction(0.50),
                allAveragePrecision.getFraction(0.95));
        outSummary.flush();

        score = sumAveragePrecision/totalResults;
        return true;
    }
}
