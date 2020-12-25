package boofcv.regression;

import boofcv.common.SettingsLocal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    String targetMetric = "P50";

    // Trigger a warning if it changed by this amount
    double threshold = 0.4;


    // The number of results successfully compared against each other
    int resultsCompared;
    final List<Flagged> flagged = new ArrayList<>();
    final List<String> missMatches = new ArrayList<>();
    final List<String> exceptions = new ArrayList<>();

    final ParseRuntimeResults rawBaseline = new ParseRuntimeResults();
    final ParseRuntimeResults rawCurrent = new ParseRuntimeResults();


    public String computeSummary() {
        try {
            SettingsLocal.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        resultsCompared = 0;
        flagged.clear();
        missMatches.clear();
        exceptions.clear();

        processAllResults();

        return resultsToString();
    }

    private String resultsToString() {
        String message = "Runtime Summary: Significant="+threshold+" Metric="+targetMetric+"\n\n";
        message += "compared    flagged   miss_match  exceptions\n";
        message += String.format("%5d       %5d      %5d      %5d\n",
                resultsCompared,flagged.size(),missMatches.size(),exceptions.size());
        if (!flagged.isEmpty()) {
            message += String.format("\nflagged.size=%d\n", flagged.size());
            Flagged prev = new Flagged("", "", "");
            for (Flagged f : flagged) {
                boolean match = true;
                String spaces = "    ";
                if (!f.file.equals(prev.file)) {
                    message += spaces + f.file + "\n";
                } else {
                    match = false;
                }
                spaces += "    ";
                if (match && !f.group.equals(prev.group)) {
                    message += spaces + f.group + "\n";
                }
                spaces += "    ";
                message += spaces + f.field + "\n";
            }
        }
        if (!missMatches.isEmpty()) {
            message += "\nMissMatches.size=" + missMatches.size() + "\n";
            for (String s : missMatches) {
                message += "    " + s + "\n";
            }
        }
        if (!exceptions.isEmpty()) {
            message += "\nExceptions.size=" + exceptions.size() + "\n";
            for (String s : exceptions) {
                message += "    " + s + "\n";
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

        for ( String baselinePath : baselinePaths ) {
            String shortBaselinePath = truncateBaselinePath(baselinePath);
            String currentPath = findCorrespondingPath(baselinePath, currentPaths);

            // Check to see if the current has these results from the baseline
            if (currentPath==null) {
                missMatches.add("No matching base: "+shortBaselinePath);
                continue;
            }

            // Try parsing the results and if there are exceptions log them
            try {
                rawBaseline.parse(new FileInputStream(baselinePath));
            } catch (IOException e) {
                exceptions.add(e.getMessage()+" "+shortBaselinePath);
                continue;
            }
            try {
                rawCurrent.parse(new FileInputStream(currentPath));
            } catch (IOException e) {
                exceptions.add(e.getMessage()+" "+shortBaselinePath);
                continue;
            }

            for( ParseRuntimeResults.Group groupBaseline : rawBaseline.groups ) {
                ParseRuntimeResults.Group groupCurrent = rawCurrent.findGroup(groupBaseline.name);
                if (groupCurrent==null) {
                    exceptions.add("No corresponding group: "+groupBaseline.name+" in "+shortBaselinePath);
                    continue;
                }

                if (groupBaseline.results.size() != groupCurrent.results.size())
                    exceptions.add("Result size miss match: "+shortBaselinePath+" "+groupBaseline.name);

                // find the ID of the metric being evaluated
                int targetBaseline = groupBaseline.indexOfMetric(targetMetric);
                int targetCurrent = groupCurrent.indexOfMetric(targetMetric);

                // The targeted metric is not in this group so skip it
                if (targetBaseline==-1)
                    continue;

                int unmatched = 0;
                for (int idxResults = 0; idxResults < groupBaseline.results.size(); idxResults++) {
                    ParseRuntimeResults.Result resultBaseline = groupBaseline.results.get(idxResults);
                    ParseRuntimeResults.Result resultCurrent = groupCurrent.lookup(resultBaseline.name);
                    if (resultCurrent==null) {
                        unmatched++;
                        continue;
                    }

                    // Look up the found metric values
                    double b = resultBaseline.results.get(targetBaseline);
                    double c = resultCurrent.results.get(targetCurrent);

                    if (b==0.0) {
                        exceptions.add("Base took zero seconds: "+shortBaselinePath+" "+groupBaseline.name+" "+resultBaseline.name);
                        continue;
                    }
                    resultsCompared++;

                    double fraction = b/c;
                    if (fraction-1.0>threshold||1.0-fraction>threshold) {
                        flagged.add(new Flagged(baselinePath,groupBaseline.name,resultBaseline.name));
                    }
                }

                // if the result count doesn't match this error has already been reported
                if (unmatched==0 || (groupBaseline.results.size() != groupCurrent.results.size()))
                    continue;

                missMatches.add("Unmatched results: "+shortBaselinePath+" "+groupBaseline.name+
                        " "+unmatched+"/"+groupBaseline.results.size());
            }
        }
    }

    /**
     * Shorten the path to make it more readable
     */
    private String truncateBaselinePath( String path ) {
        String baselinePref = SettingsLocal.getPathToBaselineRuntimeDirectory().getPath();
        return path.substring(baselinePref.length()+1);
    }

    /**
     * Finds the corresponding directory in current results to the results in baseline
     */
    private String findCorrespondingPath(String baselinePath, List<String> currentPaths) {
        String baselinePref = SettingsLocal.getPathToBaselineRuntimeDirectory().getPath();

        String ending = baselinePath.substring(baselinePref.length());

        for (int i = 0; i < currentPaths.size(); i++) {
            if (currentPaths.get(i).endsWith(ending))
                return currentPaths.get(i);
        }

        return null;
    }

    /**
     * Recursively searches for all runtime results in the path
     */
    public void findAllRuntimeResults( File root, List<String> found ) {
        File[] files = root.listFiles();
        if (files==null)
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

        public Flagged(String file, String group, String field) {
            this.file = file;
            this.group = group;
            this.field = field;
        }
    }

    public static void main(String[] args) {
        var runtime = new ComputeRuntimeRegressionSummary();
        System.out.println(runtime.computeSummary());
    }
}
