package boofcv.metrics.mvs;

import boofcv.io.UtilIO;
import gnu.trove.impl.Constants;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.*;

/**
 * @author Peter Abeles
 */
public class MultiViewRegressionIO {
    public static void saveMapTIntString(TIntObjectMap<String> map, File file) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(file));
            out.println("# map from int to string");
            map.forEachEntry((key, value) -> {
                out.printf("%d,\"%s\"\n", key, value);
                return true;
            });
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveMapTStringInt(TObjectIntMap<String> map, File file) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(file));
            out.println("# map from string to int");
            map.forEachEntry((key, value) -> {
                out.printf("\"%s\",%d\n", key, value);
                return true;
            });
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TIntObjectMap<String> loadMapTIntString(File file) {
        TIntObjectMap<String> map =
                new TIntObjectHashMap<>(Constants.DEFAULT_CAPACITY,Constants.DEFAULT_LOAD_FACTOR,-1);
        try {
            StringBuilder builder = new StringBuilder();
            InputStream in = new FileInputStream(file);
            while (true) {
                String line = UtilIO.readLine(in, builder);
                if (line.isEmpty())
                    break;
                if (line.charAt(0)=='#')
                    continue;
                String[] words = line.split(",");
                int key;
                String value;
                if (words[0].startsWith("\"")) {
                    key = Integer.parseInt(words[1]);
                    value = words[0].substring(1, words[0].length() - 1);
                } else {
                    key = Integer.parseInt(words[0]);
                    value = words[1].substring(1, words[1].length() - 1);
                }
                map.put(key,value);
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }
}
