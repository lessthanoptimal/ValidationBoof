package boofcv.metrics;

import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;

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
        outSummary.println("# All performance is in Mean Average Precision (mAP) The numbers refer to the number");
        outSummary.println("# of queries considered. 100 = first 100 queries");
        outSummary.println("# file_name, count, mAP=ALL, 1, 5, 10, 20, 50, 100, 200, 500");
    }

    public boolean evaluate(String testName, File resultsFile, ImageRetrievalEvaluationData sets) {
        outDetailed.println("# Image Retrieval Detailed Results for " + testName);
        outDetailed.println("# Average Precision is computed for each image and saved here");

        List<String> queryList = sets.getQuery();

        int totalResults = 0;
        double mAP_1,mAP_5,mAP_10,mAP_20,mAP_50,mAP_100,mAP_200,mAP_500,mAP;
        mAP_1 = mAP_5 = mAP_10 = mAP_20 = mAP_50 = mAP_100 = mAP_200 = mAP_500 = mAP = 0.0;

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
                double P_1 = 0.0;
                double P_5 = 0.0;
                double P_10 = 0.0;
                double P_20 = 0.0;
                double P_50 = 0.0;
                double P_100 = 0.0;
                double P_200 = 0.0;
                double P_500 = 0.0;
                double averagePrecision = 0.0;

                for (int i = 0; i < numResults; i++) {
                    int result = Integer.parseInt(words[i + 3]);
                    if (sets.isMatch(queryIndex, result)) {
                        totalRelevant++;
                        double precision = totalRelevant / (double)(i + 1);
                        averagePrecision += precision;
                    }

                    if (i==0) P_1 = averagePrecision/Math.min(1,totalMatches);
                    else if(i==4) P_5 = averagePrecision/Math.min(5,totalMatches);
                    else if(i==9) P_10 = averagePrecision/Math.min(10,totalMatches);
                    else if(i==19) P_20 = averagePrecision/Math.min(20,totalMatches);
                    else if(i==49) P_50 = averagePrecision/Math.min(50,totalMatches);
                    else if(i==99) P_100 = averagePrecision/Math.min(100,totalMatches);
                    else if(i==199) P_200 = averagePrecision/Math.min(200,totalMatches);
                    else if(i==499) P_500 = averagePrecision/Math.min(500,totalMatches);
                }
                // Make sure there aren't more relevant matches than possible!
                BoofMiscOps.checkTrue(totalRelevant<=totalMatches);

                averagePrecision /= totalMatches;

                outDetailed.printf("%-30s %7.4f\n", queryName, averagePrecision);

                // Handle cases where there were even N results
                if (P_5==0.0) P_5 = averagePrecision;
                if (P_10==0.0) P_10 = averagePrecision;
                if (P_20==0.0) P_20 = averagePrecision;
                if (P_50==0.0) P_50 = averagePrecision;
                if (P_100==0.0) P_100 = averagePrecision;
                if (P_200==0.0) P_200 = averagePrecision;
                if (P_500==0.0) P_500 = averagePrecision;

                mAP += averagePrecision;
                mAP_1 += P_1;
                mAP_5 += P_5;
                mAP_10 += P_10;
                mAP_20 += P_20;
                mAP_50 += P_50;
                mAP_100 += P_100;
                mAP_200 += P_200;
                mAP_500 += P_500;

                totalResults++;
            }
        } catch (IOException e) {
            e.printStackTrace(err);
            e.printStackTrace();
            return false;
        }
        outDetailed.flush();

        // median Average Prevision
        outSummary.printf("%-30s %7d %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f\n",
                testName, totalResults,
                mAP/totalResults,
                mAP_1/totalResults,
                mAP_5/totalResults,
                mAP_10/totalResults,
                mAP_20/totalResults,
                mAP_50/totalResults,
                mAP_100/totalResults,
                mAP_200/totalResults,
                mAP_500/totalResults);
        outSummary.flush();

        score = mAP/totalResults;
        return true;
    }
}
