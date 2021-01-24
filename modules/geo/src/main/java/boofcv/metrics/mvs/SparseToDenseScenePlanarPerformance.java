package boofcv.metrics.mvs;

import boofcv.alg.sfm.structure.SparseSceneToDenseCloud;
import boofcv.struct.image.ImageGray;

import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class SparseToDenseScenePlanarPerformance<T extends ImageGray<T>> {
    public PrintStream out = System.out;
    public PrintStream err = System.err;

    public void process(SparseSceneToDenseCloud<T> sparseToDense ) {

//        sparseToDense.process(scene,viewIdx_to_ImageID,lookUpImages);

    }
}
