package validate.fiducial.qrcode;

import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static validate.misc.ParseHelper.skipComments;

/**
 * @author Peter Abeles
 */
public class QrCodeFileCodec {
    public static List<Polygon2D_F64> loadLocations(String path ) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            String line = skipComments(reader);
            List<Polygon2D_F64> sets = new ArrayList<>();

            boolean pointsLine = true;
            while( line != null ) {
                if( pointsLine ) {
                    String words[] = line.split(" ");
                    if( words.length != 8 )
                        throw new RuntimeException("Expected 8 words not "+words.length);
                    Polygon2D_F64 q = new Polygon2D_F64(4);
                    for (int i = 0; i < words.length; i += 2 ) {
                        Point2D_F64 p = new Point2D_F64();
                        p.x = Double.parseDouble(words[i]);
                        p.y = Double.parseDouble(words[i+1]);
                        q.get(i/2).set(p);
                    }
                    sets.add(q);
                } else {
                    // ignore the message lines
                }

                pointsLine = !pointsLine;
                line = reader.readLine();
            }

            return sets;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
