package boofcv.regression;

import boofcv.io.UtilIO;
import org.ddogleg.struct.DogArray_F64;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ParseRuntimeResults {
    List<Group> groups = new ArrayList<>();

    StringBuilder buffer = new StringBuilder(1025);
    InputStream input;

    public void parse( InputStream input ) throws IOException {
        this.input = input;

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
                    group = new Group();
                    group.name = line;
                    state = State.METRICS;
                }
                case METRICS -> {
                    String[] words = line.strip().split("\\s+");
                    Objects.requireNonNull(group).metrics.addAll(Arrays.asList(words));
                    state = State.RESULTS;
                }
                case RESULTS -> {
                    if (!line.startsWith("  ")) {
                        if (!line.strip().isEmpty())
                            throw new IOException("Expected empty line after results");
                        state = State.HOME;
                        groups.add(group);
                        group = null;
                        break;
                    }
                    String[] words = line.strip().split("\\s+");
                    if (words.length==0)
                        throw new IOException("Line with no results");
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
            }
        }

        if (group!=null) {
            groups.add(group);
        }
    }

    private String readLine() {
        try {
            while (input.available()>0) {
                String line = UtilIO.readLine(input, buffer);
                if (line.length()==0)
                    continue;
                // skip over comment lines
                if (line.charAt(0)!='#')
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
