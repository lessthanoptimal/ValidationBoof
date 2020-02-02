package boofcv.metrics.sba;

import boofcv.gui.BoofSwingUtil;
import boofcv.io.geo.CodecBundleAdjustmentInTheLarge;

import java.io.File;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class EvaluateInTheLargeApp {
    public static void main(String[] args) throws IOException {
        File f = BoofSwingUtil.openFileChooser("EvaluateInTheLarge");
        if( f == null ) {
            return;
        }

        CodecBundleAdjustmentInTheLarge codec = new CodecBundleAdjustmentInTheLarge();

        codec.parse(f);

        double results[] = BundleAdjustmentEvaluationTools.computeReprojectionErrorMetrics(codec.scene,codec.observations);

        System.out.printf("%s 50%%=%-7.3f  95%%=%-7.3f views=%-6d obs=%-7d\n",f.getName(),results[0],results[1],
                codec.scene.views.size, codec.observations.getObservationCount());
    }
}
