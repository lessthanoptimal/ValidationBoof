package validate.fiducial.qrcode;

import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.apache.commons.io.FilenameUtils;
import validate.misc.PointFileCodec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static validate.fiducial.BaseEstimateSquareFiducialToCamera.loadImageFilesByPrefix;

/**
 *
 * metrics: true positives, false positives, false negatives  fraction of area overlap in true positives
 *
 * @author Peter Abeles
 */
public class EvaluateQrCodeDetections {
    public static final double MATCH_MINIMUM_FRACTION = 0.1;

    public int totalTruth;
    public int truePositive;
    public int falsePositive;
    public int falseNegative;
    public int multipleDetections;
    public double averageOverlap;

    public void evaluate( File resultsDirectory , File truthDirectory ) {
        resetMetrics();

        List<String> files = loadImageFilesByPrefix(truthDirectory);


        for( String imageName : files ) {
            String dataName = FilenameUtils.getBaseName(imageName)+".txt";

            List<Polygon2D_F64> results = QrCodeFileCodec.loadLocations(new File(resultsDirectory,dataName).getPath());
            List<Polygon2D_F64> truth = loadTruth(new File(truthDirectory,dataName));

            int matchedFound[] = new int[ results.size() ];
            int matchedTruth[] = new int[ truth.size() ];

            for( int i = 0; i < truth.size(); i++ ) {
                Polygon2D_F64 t = truth.get(i);

                double areaTruth = t.areaSimple();

                double bestOverlap = 0;

                for (int j = 0; j < results.size(); j++) {
                    Polygon2D_F64 r = results.get(j);

                    double f = Intersection2D_F64.intersection(t,r)/areaTruth;

                    if( f >= MATCH_MINIMUM_FRACTION ) {
                        matchedTruth[i]++;
                        matchedFound[j]++;
                        if( f > bestOverlap ) {
                            bestOverlap = f;
                        }
                    }
                }

                if( bestOverlap > 0 ) {
                    truePositive++;
                    averageOverlap += bestOverlap;
                } else {
                    falseNegative++;
                }
            }

            for (int i = 0; i < matchedTruth.length; i++) {
                if( matchedTruth[i] > 1 ) {
                    multipleDetections++;
                }
            }

            for (int i = 0; i < matchedFound.length; i++) {
                if( matchedFound[i] == 0 ) {
                    falsePositive++;
                }
            }
            totalTruth += truth.size();
        }

        averageOverlap /= truePositive;
    }

    private List<Polygon2D_F64> loadTruth( File f ) {
        List<List<Point2D_F64>> truth = PointFileCodec.loadSets(f.getPath());

        List<Polygon2D_F64> out = new ArrayList<>();

        for( List<Point2D_F64> l : truth ) {
            Polygon2D_F64 q = new Polygon2D_F64(4);
            for (int i = 0; i < 4; i++) {
                q.set(i,l.get(i).x,l.get(i).y);
            }
            out.add(q);
        }
        return out;
    }

    private void resetMetrics() {
        totalTruth = 0;
        truePositive = 0;
        falsePositive = 0;
        falseNegative = 0;
        multipleDetections = 0;
        averageOverlap = 0;
    }
}
