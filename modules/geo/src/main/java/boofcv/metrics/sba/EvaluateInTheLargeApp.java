package boofcv.metrics.sba;

import boofcv.gui.BoofSwingUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class EvaluateInTheLargeApp {
    public static void main(String[] args) throws IOException {
        File f = BoofSwingUtil.openFileChooser(null);
        if( f == null ) {
            return;
        }

        CodecBundleAdjustmentInTheLarge codec = new CodecBundleAdjustmentInTheLarge();

        codec.parse(f);

        double results[] = BundleAdjustmentEvaluationTools.computeReprojectionErrorMetrics(codec.scene,codec.observations);

        System.out.println(f.getName()+" 50% = "+results[0]+" 95% = "+results[1]);
    }
}
