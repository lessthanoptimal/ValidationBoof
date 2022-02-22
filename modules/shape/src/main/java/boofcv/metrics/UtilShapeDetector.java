package boofcv.metrics;

import boofcv.common.misc.ParseHelper;
import boofcv.common.misc.PointFileCodec;
import boofcv.factory.shape.ConfigEllipseDetector;
import boofcv.factory.shape.ConfigPolygonDetector;
import georegression.struct.ConvertFloatType;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class UtilShapeDetector {
    public static PolylineSettings loadPolylineSettings(File file) {
        var settings = new PolylineSettings();

        if (!file.exists())
            return settings;

        try {
            BufferedReader reader = Files.newBufferedReader(file.toPath());
            String line = ParseHelper.skipComments(reader);

            while (line != null) {
                String[] words = line.split(" ");
                if (words.length != 2)
                    throw new RuntimeException("Unexpected number of words on line");

                if (words[0].equalsIgnoreCase("convex")) {
                    settings.convex = Boolean.parseBoolean(words[1]);
                } else if (words[0].equalsIgnoreCase("min_sides")) {
                    settings.minSides = Integer.parseInt(words[1]);
                } else if (words[0].equalsIgnoreCase("max_sides")) {
                    settings.maxSides = Integer.parseInt(words[1]);
                } else if (words[0].equalsIgnoreCase("loop")) {
                    settings.looping = Boolean.parseBoolean(words[1]);
                }

                line = reader.readLine();
            }
        } catch (FileNotFoundException ignore) {
            // just go with the defaults
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return settings;
    }

    public static ConfigPolygonDetector configurePolygon(File file) {

        int minSides = 3, maxSides = 6;
        boolean convex = true;
        boolean border = false;

        try {
            var reader = new BufferedReader(new FileReader(file));

            String line = ParseHelper.skipComments(reader);

            while (line != null) {

                String[] words = line.split(" ");
                if (words.length != 2)
                    throw new RuntimeException("Unexpected number of words on line");

                if (words[0].equalsIgnoreCase("convex")) {
                    convex = Boolean.parseBoolean(words[1]);
                } else if (words[0].equalsIgnoreCase("min_sides")) {
                    minSides = Integer.parseInt(words[1]);
                } else if (words[0].equalsIgnoreCase("max_sides")) {
                    maxSides = Integer.parseInt(words[1]);
                } else if (words[0].equalsIgnoreCase("image_border")) {
                    border = Boolean.parseBoolean(words[1]);
                }

                line = reader.readLine();
            }
        } catch (FileNotFoundException ignore) {
            // just go with the defaults
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var config = new ConfigPolygonDetector(minSides, maxSides);

        config.detector.contourToPoly.convex = convex;
        config.detector.canTouchBorder = border;

        return config;
    }

    public static ConfigEllipseDetector configureEllipse(boolean subpixel) {
        var config = new ConfigEllipseDetector();

        if (!subpixel)
            config.maxIterations = 0;

        return config;
    }

    public static void saveResults(List<Polygon2D_F64> polygons, File file) {
        try {
            var out = new PrintStream(file);

            out.println("# Detected polygons in an image");
            out.println("# (Number of corners) (corner X) (corner Y) ...");
            for (int i = 0; i < polygons.size(); i++) {
                Polygon2D_F64 p = polygons.get(i);

                out.print(p.size());
                for (int j = 0; j < p.size(); j++) {
                    Point2D_F64 c = p.get(j);
                    out.printf(" %f %f", c.x, c.y);
                }
                out.println();
            }

            out.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveResultsPolyline(List<List<Point2D_I32>> polygons, File file) {
        try {
            var out = new PrintStream(file);

            out.println("# Detected polylines in an image");
            out.println("# (Number of corners) (corner X) (corner Y) ...");
            for (int i = 0; i < polygons.size(); i++) {
                List<Point2D_I32> p = polygons.get(i);

                out.print(p.size());
                for (int j = 0; j < p.size(); j++) {
                    Point2D_I32 c = p.get(j);
                    out.printf(" %d %d", c.x, c.y);
                }
                out.println();
            }

            out.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<List<Point2D_I32>> loadResultsPolyline(File file) {
        try {
            var reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();
            while (line != null && line.length() >= 1) {
                if (line.charAt(0) != '#')
                    break;
                line = reader.readLine();
            }

            List<List<Point2D_I32>> out = new ArrayList<>();
            while (line != null) {

                String[] words = line.split(" ");
                int size = Integer.parseInt(words[0]);

                List<Point2D_I32> poly = new ArrayList<>();

                for (int i = 0; i < size; i++) {
                    Point2D_I32 p = new Point2D_I32();
                    p.x = Integer.parseInt(words[1 + i * 2]);
                    p.y = Integer.parseInt(words[2 + i * 2]);
                    poly.add(p);
                }
                out.add(poly);
                line = reader.readLine();
            }

            return out;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<PolygonTruthIndexes> loadDescription(File file) {
        try {
            var reader = new BufferedReader(new FileReader(file));

            String line = ParseHelper.skipComments(reader);

            var out = new ArrayList<PolygonTruthIndexes>();
            int index = 0;
            while (line != null) {

                String[] words = line.split(" ");
                if (words.length != 2)
                    throw new RuntimeException("Unexpected number of words on line");

                PolygonTruthIndexes polygon = new PolygonTruthIndexes();
                polygon.name = words[0];

                int numCorners = Integer.parseInt(words[1]);

                polygon.indexes = new int[numCorners];
                for (int i = 0; i < numCorners; i++) {
                    polygon.indexes[i] = index++;
                }

                out.add(polygon);
                line = reader.readLine();
            }

            return out;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<LineSegment2D_F32> loadTruthLineSegments(File fileTruth) {
        List<List<Point2D_F64>> sets = PointFileCodec.loadSets(fileTruth);

        var lines = new ArrayList<LineSegment2D_F32>();

        var line64 = new LineSegment2D_F64();

        for (int i = 0; i < sets.size(); i++) {
            List<Point2D_F64> set = sets.get(i);

            if (set.size() == 2) {
                line64.setTo(set.get(0), set.get(1));

                lines.add(ConvertFloatType.convert(line64, null));
            } else {
                for (int j = 0, k = set.size() - 1; j < set.size(); j++) {
                    line64.setTo(set.get(j), set.get(k));
                    lines.add(ConvertFloatType.convert(line64, null));
                }
            }
        }

        return lines;
    }

    public static void saveResultsLines(List<LineParametric2D_F32> lines, File file) {
        try {
            var out = new PrintStream(file);

            out.println("# Detected lines in an image. Parametric equation");
            out.println("# x0 y0 slopeX slopeY");
            for (int i = 0; i < lines.size(); i++) {
                LineParametric2D_F32 p = lines.get(i);
                out.printf("%f %f %f %f\n", p.p.x, p.p.y, p.slope.x, p.slope.y);
            }

            out.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<LineParametric2D_F32> loadResultsLines(File file) {
        try {
            var reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();
            while (line != null && line.length() >= 1) {
                if (line.charAt(0) != '#')
                    break;
                line = reader.readLine();
            }

            var out = new ArrayList<LineParametric2D_F32>();
            while (line != null) {
                var l = new LineParametric2D_F32();

                String[] words = line.split(" ");
                if (words.length != 4)
                    throw new RuntimeException("Unexpected number of works. " + words.length);

                l.p.x = Float.parseFloat(words[0]);
                l.p.y = Float.parseFloat(words[1]);
                l.slope.x = Float.parseFloat(words[2]);
                l.slope.y = Float.parseFloat(words[3]);

                out.add(l);
                line = reader.readLine();
            }

            return out;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Polygon2D_F64> loadResults(File file) {
        try {
            var reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();
            while (line != null && line.length() >= 1) {
                if (line.charAt(0) != '#')
                    break;
                line = reader.readLine();
            }

            var out = new ArrayList<Polygon2D_F64>();
            while (line != null) {

                String[] words = line.split(" ");
                int size = Integer.parseInt(words[0]);

                var poly = new Polygon2D_F64(size);

                for (int i = 0; i < size; i++) {
                    Point2D_F64 p = poly.get(i);
                    p.x = Double.parseDouble(words[1 + i * 2]);
                    p.y = Double.parseDouble(words[2 + i * 2]);
                }
                out.add(poly);
                line = reader.readLine();
            }

            return out;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String imageToDetectedName(String name) {
        return name.substring(0, name.length() - 4) + "_detected.txt";
    }
}
