package boofcv.common.misc;

import georegression.struct.point.Point2D_F64;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static boofcv.common.misc.ParseHelper.skipComments;

/**
 * For reading and writing simple list of points
 *
 * @author Peter Abeles
 */
public class PointFileCodec {
    public static void save(String path, String header, List<Point2D_F64> points) {
        File f = new File(path);

        try {
            PrintStream out = new PrintStream(f);

            if (header != null)
                out.println("# " + header);

            for (int i = 0; i < points.size(); i++) {
                Point2D_F64 p = points.get(i);
                out.printf("%.20f %.20f\n", p.x, p.y);
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveSets(String path, String header, List<List<Point2D_F64>> points) {
        File f = new File(path);

        try {
            PrintStream out = new PrintStream(f);

            if (header != null)
                out.println("# " + header);

            out.println("SETS");
            for (List<Point2D_F64> set : points) {
                if (set.isEmpty())
                    continue;
                for (int i = 0; i < set.size(); i++) {
                    Point2D_F64 p = set.get(i);
                    out.printf("%.20f %.20f", p.x, p.y);
                    if (i != set.size() - 1) {
                        out.print(" ");
                    }
                }
                out.println();
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPointSet(File path) {
        try {
            FileReader reader = new FileReader(path);
            BufferedReader input = new BufferedReader(reader);
            String line = skipComments(input);
            reader.close();
            return line.compareTo("SETS") == 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Point2D_F64> load(File path) {
        return load(path.getAbsolutePath());
    }

    public static List<Point2D_F64> load(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            String line = skipComments(reader);
            if (line.compareTo("SETS") == 0) {
                System.err.println("This is a point sets file!");
                return null;
            }

            List<Point2D_F64> out = new ArrayList<>();
            while (line != null) {

                String[] words = line.split(" ");
                if (words.length != 2) {
                    throw new RuntimeException("expected two words");
                }

                Point2D_F64 p = new Point2D_F64();
                p.x = Double.parseDouble(words[0]);
                p.y = Double.parseDouble(words[1]);

                out.add(p);
                line = reader.readLine();
            }

            return out;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<List<Point2D_F64>> loadSets(File path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            List<List<Point2D_F64>> sets = new ArrayList<>();
            String line = skipComments(reader);
            if (line.compareTo("SETS") == 0)
                line = reader.readLine();
            else {
                // not a point sets file, assume everything is one set
                sets.add(load(path));
                return sets;
            }

            while (line != null) {
                List<Point2D_F64> set = new ArrayList<>();
                String[] words = line.split(" ");

                if (words.length % 2 == 1)
                    throw new RuntimeException("Odd number of words found on a line");

                for (int i = 0; i < words.length; i += 2) {
                    Point2D_F64 p = new Point2D_F64();
                    p.x = Double.parseDouble(words[i]);
                    p.y = Double.parseDouble(words[i + 1]);
                    set.add(p);
                }
                sets.add(set);
                line = reader.readLine();
            }

            return sets;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
