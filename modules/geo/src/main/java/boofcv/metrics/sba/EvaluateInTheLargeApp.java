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

        System.out.printf("%s 50%%=%-5.1f  95%%=%-5.1f views=%-6d obs=%-7d\n",f.getName(),results[0],results[1],
                codec.scene.views.length, codec.observations.getObservationCount());
    }
}
