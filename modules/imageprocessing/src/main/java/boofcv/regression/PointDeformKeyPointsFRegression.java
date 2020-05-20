package boofcv.regression;

import boofcv.abst.distort.ConfigDeformPointMLS;
import boofcv.abst.distort.PointDeformKeyPoints;
import boofcv.alg.distort.mls.TypeDeformMLS;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.FileRegression;
import boofcv.common.RegressionRunner;
import boofcv.factory.distort.FactoryDistort;
import boofcv.metrics.ChangeOutputPointDeformKeyPoints;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class PointDeformKeyPointsFRegression extends BaseRegression implements FileRegression {

    PrintStream out;
    ChangeOutputPointDeformKeyPoints metrics = new ChangeOutputPointDeformKeyPoints();

    public PointDeformKeyPointsFRegression() {
        super("");
    }

    @Override
    public void process() throws IOException {

        out = new PrintStream(new File(directoryMetrics,"ACC_PointDeformKeyPointsChange.txt"));
        BoofRegressionConstants.printGenerator(out, getClass());

        metrics.err = errorLog;
        metrics.out = out;

        out.println("PointDeformKeyPoints: Change Regression\n\n");

        ConfigDeformPointMLS config = new ConfigDeformPointMLS();

        config.type = TypeDeformMLS.RIGID;
        process("MLS-Rigid", FactoryDistort.deformMls(config));
        config.type = TypeDeformMLS.AFFINE;
        process("MLS-Affine", FactoryDistort.deformMls(config));
        config.type = TypeDeformMLS.SIMILARITY;
        process("MLS-Similarity", FactoryDistort.deformMls(config));

        out.close();
    }

    private void process( String name , PointDeformKeyPoints alg ) {
        try {
            metrics.process(name, alg);
        } catch( RuntimeException e ) {
            e.printStackTrace(errorLog);
        }
    }

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        BoofRegressionConstants.clearCurrentResults();
        RegressionRunner.main(new String[]{PointDeformKeyPointsFRegression.class.getName()});
    }

}
