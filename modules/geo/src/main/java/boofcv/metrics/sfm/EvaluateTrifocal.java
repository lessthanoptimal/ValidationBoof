package boofcv.metrics.sfm;

import boofcv.alg.geo.MultiViewOps;
import boofcv.io.UtilIO;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point3D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.stats.StatisticsDogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ejml.data.DMatrixRMaj;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evaluates the quality of the trifocal tensor estimate
 *
 * @author Peter Abeles
 */
public class EvaluateTrifocal {

    public File directoryObservations = new File(".");
    public File directoryResults = new File(".");

    public PrintStream out = System.out;
    public PrintStream err = System.err;

    public static TrifocalTensor readTensor(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        List<double[]> data = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0 || line.charAt(0) == '#')
                continue;

            String a[] = line.split("\\s+");

            double d[] = new double[3];
            d[0] = Double.parseDouble(a[0]);
            d[1] = Double.parseDouble(a[1]);
            d[2] = Double.parseDouble(a[2]);

            data.add(d);
        }

        TrifocalTensor tensor = new TrifocalTensor();

        for (int i = 0; i < 3; i++) {
            int row = i * 3;

            DMatrixRMaj T = tensor.getT(i);

            for (int j = 0; j < 3; j++) {
                T.data[j] = data.get(row)[j];
                T.data[j + 3] = data.get(row + 1)[j];
                T.data[j + 6] = data.get(row + 2)[j];
            }
        }

        return tensor;
    }


    public static DogArray_F64 computeTransferErrors(TrifocalTensor tensor, List<AssociatedTriple> obs) {
        // adjust the scale so that there is no advantage there
        tensor.normalizeScale();

        DogArray_F64 errors = new DogArray_F64();

        Point3D_F64 predicted = new Point3D_F64();
        for (AssociatedTriple o : obs) {
            MultiViewOps.transfer_1_to_2(tensor, o.p1, o.p3, predicted);
            errors.add(o.p2.distance(predicted.x / predicted.z, predicted.y / predicted.z));
            MultiViewOps.transfer_1_to_3(tensor, o.p1, o.p2, predicted);
            errors.add(o.p3.distance(predicted.x / predicted.z, predicted.y / predicted.z));
        }

        return errors;
    }

    public void evaluate(String library) throws IOException {
        List<String> files = UtilIO.listAll(directoryObservations.getPath());
        Collections.sort(files);

        out.println(library);
        out.printf("%30s %5s %5s %5s %5s %5s\n", "", "mean", "p50", "p75", "p95", "p100");

        DogArray_F64 allErrors = new DogArray_F64();

        for (String s : files) {
            File f = new File(s);
            String name = f.getName();
            if (!f.isFile() || !name.startsWith("tensor_pixel") || !name.endsWith(".txt"))
                continue;

            List<AssociatedTriple> observations = ComputeTrifocalTensor.readObservations(f);

            TrifocalTensor tensor = readTensor(new File(directoryResults, library + "_" + name));
            DogArray_F64 errors = computeTransferErrors(tensor, observations);
            errors.sort();

            double mean = StatisticsDogArray.mean(errors);

            out.printf("%30s %5.1f %5.1f %5.1f %5.1f %5.1f\n", FilenameUtils.removeExtension(f.getName()),
                    mean, errors.getFraction(0.5), errors.getFraction(0.75),
                    errors.getFraction(0.95), errors.getFraction(1.0));
            allErrors.addAll(errors);
        }

        allErrors.sort();
        out.printf("  Summary: p50=%.1f p95=%.1f\n", allErrors.getFraction(0.5), allErrors.getFraction(0.95));

        out.println();
    }

    public static void main(String[] args) throws IOException {
        EvaluateTrifocal app = new EvaluateTrifocal();
        app.directoryObservations = new File("trifocal");
        app.directoryResults = new File("trifocal/results");

        app.evaluate("algebraic7");
        app.evaluate("linear7");

    }
}
