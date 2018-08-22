package boofcv.metrics.sfm;

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.io.UtilIO;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.GrowQueue_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluatePnPObservations {

    // results are sent to these streams
    PrintStream err = System.err;
    PrintStream out = System.out;

    // Internal workspace
    CameraPinhole intrinsics = new CameraPinhole();
    Point2Transform2_F64 p2n;
    List<Point2D3D> points = new ArrayList<>();
    Se3_F64 found = new Se3_F64();
    Se3_F64 expected = new Se3_F64();

    Rodrigues_F64 rod = new Rodrigues_F64();

    GrowQueue_F64 errorAngle = new GrowQueue_F64();
    GrowQueue_F64 errorTranslation = new GrowQueue_F64();
    int totalFailures,totalEstimated;
    DMatrixRMaj R = new DMatrixRMaj(3,3);

    public void printHeader() {
        out.println("        file                N   tran50  tran95  ang50   ang95");
    }

    public void evaluate(File directory  , Estimate1ofPnP pnp ) {

        List<String> files = UtilIO.listAll(directory.getPath());
        Collections.sort(files);


        for( String path : files ) {
            if( !path.endsWith(".txt"))
                continue;

            File f = new File(path);
            try {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                evaluate(reader,pnp);

                errorAngle.sort();
                errorTranslation.sort();

                out.printf("%-25s %4d %7.3f %7.3f %7.4f %7.4f\n", FilenameUtils.removeExtension(f.getName()),
                        errorAngle.size,
                        errorTranslation.getFraction(0.5),errorTranslation.getFraction(0.95),
                        errorAngle.getFraction(0.5),errorAngle.getFraction(0.95));
            } catch (IOException e) {
                e.printStackTrace(err);
            }
        }
    }

    void evaluate(BufferedReader reader , Estimate1ofPnP pnp ) throws IOException {

        errorAngle.reset();
        errorTranslation.reset();
        totalFailures = 0;
        totalEstimated = 0;

        int lineCount = 0;
        while( true ) {
            String line = reader.readLine();
            if( line == null )
                break;
            int lineAt = lineCount++;

            if( line.length() == 0 || line.charAt(0) == '#' )
                continue;
            String words[] = line.split("\\s+");
            if( words.length < 2 )
                throw new IOException("Unexpected number of words on line "+lineAt);

            switch( words[0] ) {
                case "CAMERA":
                    parseCamera(words);
                    break;

                case "CORNERS":
                    parseCorners(words);
                    break;

                case "POSE":
                    parsePose(words);
                    break;

                case "OBSERVATIONS":
                    parseObservations(words);
                    if( pnp.process(points,found) ) {
                        totalEstimated++;
                        errorAngle.add( computeAngleError() );
                        errorTranslation.add( found.T.distance(expected.T));
                    } else {
                        totalFailures++;
                    }
                    break;

                default:
                    throw new IOException("Unknown type "+words[0]+" on line "+lineAt);
            }
        }
    }

    private double computeAngleError() {
        CommonOps_DDRM.multTransA(found.R,expected.R,R);
        ConvertRotation3D_F64.matrixToRodrigues(R,rod);
        return rod.theta;
    }

    private void parseCamera(String[] words) throws IOException {
        if( words.length != 7 )
            throw new IOException("Unexpected number of words");
        intrinsics.width = Integer.parseInt(words[1]);
        intrinsics.height = Integer.parseInt(words[2]);
        intrinsics.fx = Double.parseDouble(words[3]);
        intrinsics.fy = Double.parseDouble(words[4]);
        intrinsics.cx = Double.parseDouble(words[5]);
        intrinsics.cy = Double.parseDouble(words[6]);

        p2n = new LensDistortionPinhole(intrinsics).distort_F64(true,false);
    }

    private void parseCorners(String[] words) throws IOException {
        int N = Integer.parseInt(words[1]);
        if( words.length != 2+N*3 )
            throw new IOException("Unexpected number of words");

        if( points.size() != N ) {
            points.clear();
            for (int i = 0; i < N; i++) {
                points.add( new Point2D3D() );
            }
        }

        for (int i = 0; i < N; i++) {
            Point2D3D p = points.get(i);
            p.location.x = Double.parseDouble(words[i*3+2]);
            p.location.y = Double.parseDouble(words[i*3+3]);
            p.location.z = Double.parseDouble(words[i*3+4]);
        }
    }

    private void parsePose(String[] words) throws IOException {
        if( words.length != 8 )
            throw new IOException("Unexpected number of words");

        expected.T.x = Double.parseDouble(words[1]);
        expected.T.y = Double.parseDouble(words[2]);
        expected.T.z = Double.parseDouble(words[3]);
        rod.unitAxisRotation.x = Double.parseDouble(words[4]);
        rod.unitAxisRotation.y = Double.parseDouble(words[5]);
        rod.unitAxisRotation.z = Double.parseDouble(words[6]);
        rod.theta = Double.parseDouble(words[7]);

        rod.unitAxisRotation.normalize();
        ConvertRotation3D_F64.rodriguesToMatrix(rod,expected.R);
    }

    private void parseObservations(String[] words) throws IOException {
        int N = Integer.parseInt(words[1]);
        if( words.length != 2+N*2 )
            throw new IOException("Unexpected number of words");

        if( points.size() != N ) {
            throw new IOException("Number of observations must match number of corners.");
        }

        for (int i = 0; i < N; i++) {
            Point2D3D p = points.get(i);
            // read in pixels
            double x = Double.parseDouble(words[i*2+2]);
            double y = Double.parseDouble(words[i*2+3]);

            // Convert the normalized image coordinates
            p2n.compute(x,y,p.observation);
        }
    }

    public static void main(String[] args) {
        EvaluatePnPObservations evaluator = new EvaluatePnPObservations();

        Estimate1ofPnP alg = FactoryMultiView.computePnP_1(EnumPNP.IPPE,-1,0);
//        Estimate1ofPnP alg = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,1);

        evaluator.printHeader();
        evaluator.evaluate(new File("pnp"), alg);
    }

    public void setErr(PrintStream err) {
        this.err = err;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }
}
