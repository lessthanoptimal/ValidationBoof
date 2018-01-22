package boofcv.regression;

import boofcv.abst.distort.ConfigDeformPointMLS;
import boofcv.abst.distort.PointDeformKeyPoints;
import boofcv.alg.distort.mls.TypeDeformMLS;
import boofcv.common.BaseImageRegression;
import boofcv.factory.distort.FactoryDistort;
import boofcv.metrics.ChangeOutputPointDeformKeyPoints;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class PointDeformKeyPointsRegression extends BaseImageRegression {

    PrintStream out;
    ChangeOutputPointDeformKeyPoints metrics = new ChangeOutputPointDeformKeyPoints();

    @Override
    public void process(ImageDataType type) throws IOException {

        out = new PrintStream(new File(directory,"PointDeformKeyPointsChange.txt"));

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

    public static void main(String[] args) throws IOException {
        PointDeformKeyPointsRegression regression = new PointDeformKeyPointsRegression();

        regression.setOutputDirectory(".");
        regression.process(null);
    }

}
