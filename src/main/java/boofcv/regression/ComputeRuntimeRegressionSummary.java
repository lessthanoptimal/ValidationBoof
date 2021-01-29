package boofcv.regression;

import boofcv.common.SettingsLocal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares the baseline to current results and flags situations were there is a large change. If there is no
 * direct comparison that will be noted too.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("StringConcatenationInLoop")
public class ComputeRuntimeRegressionSummary {

    // Trigger a warning if it changed by this amount
    double threshold = 0.4;


    // The number of results successfully compared against each other
    int resultsCompared;
    final List<Flagged> flagged = new ArrayList<>();
    final List<String> missMatches = new ArrayList<>();
    final List<String> exceptions = new ArrayList<>();

    final ParseRuntimeResults rawBaseline = new ParseRuntimeResults();
    final ParseRuntimeResults rawCurrent = new ParseRuntimeResults();

    public String computeSummary(PrintStream err) {
        try {
            SettingsLocal.load();
            resultsCompared = 0;
            flagged.clear();
            missMatches.clear();
            exceptions.clear();

            processAllResults();

            return resultsToString();
        } catch (Exception e) {
            e.printStackTrace(err);
            return "Exception computing runtime summary.\n" +
                    "  Type=" + e.getClass().getName() + "\n" +
                    "  Message=" + e.getMessage() + "\n";
        }
    }

    private String resultsToString() {
        String message = "Runtime Summary: Significant=" + threshold + " Metric=" + ParseRuntimeResults.DEFAULT_METRIC + "\n\n";
        message += "compared    flagged   miss_match  exceptions\n";
        message += String.format("%5d       %5d      %5d      %5d\n",
                resultsCompared, flagged.size(), missMatches.size(), exceptions.size());
        if (!flagged.isEmpty()) {
            message += String.format("\nflagged.size=%d\n", flagged.size());
            for (Flagged f : flagged) {
                message += String.format("  %5.1f%% %s\n", 100.0 * (f.currentOverBaseline), f.file + ":" + f.group + ":" + f.field);
            }
        }
        if (!missMatches.isEmpty()) {
            message += "\nMissMatches.size=" + missMatches.size() + "\n";
            for (String s : missMatches) {
                message += "  " + s + "\n";
            }
        }
        if (!exceptions.isEmpty()) {
            message += "\nExceptions.size=" + exceptions.size() + "\n";
            for (String s : exceptions) {
                message += "  " + s + "\n";
            }
        }
        return message;
    }

    /**
     * Load and compare all the runtime results
     */
    private void processAllResults() {
        List<String> baselinePaths = new ArrayList<>();
        List<String> currentPaths = new ArrayList<>();

        findAllRuntimeResults(SettingsLocal.getPathToBaselineRuntimeDirectory(), baselinePaths);
        findAllRuntimeResults(SettingsLocal.getPathToCurrentRuntimeDirectory(), currentPaths);

        for (String baselinePath : baselinePaths) {
            String currentPath = findCorrespondingPath(true, baselinePath, currentPaths);
            String shortBaselinePath = truncateBaselinePath(true, baselinePath);

            // Check to see if the current has these results from the baseline
            if (currentPath == null) {
                missMatches.add("File not in current: " + shortBaselinePath);
                continue;
            }

            // Loads results for the specific metric from current and baseline
            if (loadResults(baselinePath, shortBaselinePath, currentPath))
                continue;

            // See if the file override this metric
            String targetMetric = rawBaseline.targetMetric;

            // indicates that the target metric was found at least once
            boolean foundTargetMetric = false;
            for (ParseRuntimeResults.Group groupBaseline : rawBaseline.groups) {
                ParseRuntimeResults.Group groupCurrent = rawCurrent.findGroup(groupBaseline.name);
                if (groupCurrent == null) {
                    exceptions.add("No corresponding group: " + groupBaseline.name + " in " + shortBaselinePath);
                    continue;
                }

                if (groupBaseline.results.size() != groupCurrent.results.size())
                    exceptions.add("Result size miss match: " + shortBaselinePath + " " + groupBaseline.name);

                // find the ID of the metric being evaluated
                int targetBaseline = groupBaseline.indexOfMetric(targetMetric);
                int targetCurrent = groupCurrent.indexOfMetric(targetMetric);

                // The targeted metric is not in this group so skip it
                if (targetBaseline == -1)
                    continue;
                foundTargetMetric = true;

                compareMetricsForBenchmark(shortBaselinePath, groupBaseline, groupCurrent, targetBaseline, targetCurrent);
            }

            // Print an error if it couldn't perform any runtime regression on this file
            if (!foundTargetMetric) {
                exceptions.add("Metric not found. " + shortBaselinePath);
            }
        }

        findResultsNotInBaseline(baselinePaths, currentPaths);
    }

    private void compareMetricsForBenchmark(String shortBaselinePath,
                                            ParseRuntimeResults.Group groupBaseline,
                                            ParseRuntimeResults.Group groupCurrent,
                                            int targetBaseline, int targetCurrent) {
        int unmatched = 0;
        for (int idxResults = 0; idxResults < groupBaseline.results.size(); idxResults++) {
            ParseRuntimeResults.Result resultBaseline = groupBaseline.results.get(idxResults);
            ParseRuntimeResults.Result resultCurrent = groupCurrent.lookup(resultBaseline.name);
            if (resultCurrent == null) {
                unmatched++;
                continue;
            }

            // Look up the found metric values
            double b = resultBaseline.results.get(targetBaseline);
            double c = resultCurrent.results.get(targetCurrent);

            if (b == 0.0) {
                exceptions.add("Base took zero seconds: " + shortBaselinePath + " " + groupBaseline.name + " " + resultBaseline.name);
                continue;
            }
            resultsCompared++;

            if (threshold < (c > b ? c / b : b / c) - 1.0) {
                flagged.add(new Flagged(shortBaselinePath, groupBaseline.name, resultBaseline.name, c / b));
            }
        }

        // if the result count doesn't match this error has already been reported
        if (unmatched == 0 || (groupBaseline.results.size() != groupCurrent.results.size()))
            return;

        missMatches.add("Unmatched results: " + shortBaselinePath + " " + groupBaseline.name +
                " " + unmatched + "/" + groupBaseline.results.size());
    }

    private void findResultsNotInBaseline(List<String> baselinePaths, List<String> currentPaths) {
        for (String currentPath : currentPaths) {
            String baselinePath = findCorrespondingPath(false, currentPath, baselinePaths);

            if (baselinePath != null)
                continue;

            String shortCurrentPath = truncateBaselinePath(false, currentPath);
            missMatches.add("File not in baseline: " + shortCurrentPath);
        }
    }

    private boolean loadResults(String baselinePath, String shortBaselinePath, String currentPath) {
        // Try parsing the results and if there are any exceptions log them
        try {
            rawBaseline.parse(new FileInputStream(baselinePath));
        } catch (IOException e) {
            exceptions.add(e.getMessage() + " " + shortBaselinePath);
            return true;
        }
        try {
            rawCurrent.parse(new FileInputStream(currentPath));
        } catch (IOException e) {
            exceptions.add(e.getMessage() + " " + shortBaselinePath);
            return true;
        }
        return false;
    }

    /**
     * Shorten the path to make it more readable
     */
    private String truncateBaselinePath(boolean baseline, String path) {
        String baselinePref = baseline ?
                SettingsLocal.getPathToBaselineRuntimeDirectory().getPath() :
                SettingsLocal.getPathToCurrentRuntimeDirectory().getPath();
        return path.substring(baselinePref.length() + 1);
    }

    /**
     * Finds the corresponding directory in current results to the results in baseline
     */
    private String findCorrespondingPath(boolean baseline, String baselinePath, List<String> currentPaths) {
        String baselinePref = baseline ?
                SettingsLocal.getPathToBaselineRuntimeDirectory().getPath() :
                SettingsLocal.getPathToCurrentRuntimeDirectory().getPath();

        String ending = baselinePath.substring(baselinePref.length() + 1);

        for (int i = 0; i < currentPaths.size(); i++) {
            if (currentPaths.get(i).endsWith(ending))
                return currentPaths.get(i);
        }

        return null;
    }

    /**
     * Recursively searches for all runtime results in the path
     */
    public void findAllRuntimeResults(File root, List<String> found) {
        File[] files = root.listFiles();
        if (files == null)
            return;

        // Find all the runtime files
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (!f.isFile() || !f.getName().startsWith("RUN_"))
                continue;
            found.add(f.getPath());
        }

        // Go into child directories and see if there are any results there
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (!f.isDirectory())
                continue;

            findAllRuntimeResults(f, found);
        }
    }

    public static class Flagged {
        String file;
        String group;
        String field;
        // value of metric
        double currentOverBaseline;

        public Flagged(String file, String group, String field, double currentOverBaseline) {
            this.file = file;
            this.group = group;
            this.field = field;
            this.currentOverBaseline = currentOverBaseline;
        }
    }

    public static void main(String[] args) {
        System.out.println(new ComputeRuntimeRegressionSummary().computeSummary(System.err));
    }
}
