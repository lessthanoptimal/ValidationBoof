package boofcv.regression;

import boofcv.common.ValidationConstants;
import boofcv.io.UtilIO;
import org.ddogleg.struct.DogArray_F64;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Parses runtime results
 *
 * @author  Peter Abeles
 */
public class ParseRuntimeResults {
    public static final String DEFAULT_METRIC = "P50";

    List<Group> groups = new ArrayList<>();

    StringBuilder buffer = new StringBuilder(1025);
    InputStream input;

    // The file can contain an override for the metric which is to be evaluated
    String targetMetric = DEFAULT_METRIC;

    // line number in the file
    int linesRead;

    public void parse( InputStream input ) throws IOException {
        this.input = input;
        this.linesRead = 0;
        this.targetMetric = DEFAULT_METRIC;

        groups = new ArrayList<>();

        State state = State.HOME;
        Group group = null;

        while (true) {
            String line = readLine();
            if (line == null)
                break;

            switch (state) {
                case HOME -> {
                    line = line.strip();
                    if (line.isEmpty())
                        continue;

                    // See if the file has provided an override for the target metric
                    if (line.startsWith(ValidationConstants.TARGET_OVERRIDE)) {
                        targetMetric = line.substring(ValidationConstants.TARGET_OVERRIDE.length());
                        continue;
                    }
                    group = new Group();
                    group.name = line;
                    state = State.METRICS;
                }
                case METRICS -> {
                    String[] words = line.strip().split("\\s+");
                    Objects.requireNonNull(group);

                    // If there are just two words then it's individual results
                    if (words.length==2) {
                        group.metrics.add("Time");
                        addResultsToGroup(group, words);
                    } else {
                        group.metrics.addAll(Arrays.asList(words));
                    }
                    state = State.RESULTS;
                }
                case RESULTS -> {
                    if (line.isEmpty()) {
                        state = State.HOME;
                        groups.add(group);
                        group = null;
                        break;
                    }
                    String[] words = line.strip().split("\\s+");
                    addResultsToGroup(group, words);
                }
            }
        }

        if (group!=null) {
            groups.add(group);
        }
    }

    private void addResultsToGroup(Group group, String[] words) throws IOException {
        Result r = new Result();
        r.name = words[0];
        try {
            for (int i = 1; i < words.length; i++) {
                r.results.add(Double.parseDouble(words[i]));
            }
        } catch( NumberFormatException e) {
            throw new IOException("parseDouble: "+e.getMessage());
        }
        Objects.requireNonNull(group).results.add(r);
        if (words.length-1 != group.metrics.size())
            throw new IOException("Number of results and metrics do not match");
    }

    private String readLine() {
        try {
            while (input.available()>0) {
                linesRead++;
                String line = UtilIO.readLine(input, buffer);
                // skip over comment lines
                if (line.isEmpty() || line.charAt(0)!='#')
                    return line;
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Group findGroup(String name) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).name.equals(name))
                return groups.get(i);
        }
        return null;
    }

    public static class Group {
        String name;
        final List<String> metrics = new ArrayList<>();
        final List<Result> results = new ArrayList<>();

        public int indexOfMetric( String metric ) {
            for (int i = 0; i < metrics.size(); i++) {
                if (metrics.get(i).equals(metric))
                    return i;
            }
            return -1;
        }

        public Result lookup(String name) {
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).name.equals(name))
                    return results.get(i);
            }
            return null;
        }
    }

    public static class Result {
        String name;
        DogArray_F64 results = new DogArray_F64();
    }

    enum State {
        HOME,
        METRICS,
        RESULTS
    }
}
