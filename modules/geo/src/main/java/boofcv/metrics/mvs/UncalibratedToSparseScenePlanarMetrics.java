package boofcv.metrics.mvs;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.sfm.structure.ImageSequenceToSparseScene;
import boofcv.io.image.LookUpImageFilesByPath;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;

import java.io.File;
import java.util.List;

/**
 * Metrics for uncalibrated image reconstruction. Processes all the images in a directory and uses known planes
 * to score how well the reconstruction went.
 *
 * @author Peter Abeles
 */
public class UncalibratedToSparseScenePlanarMetrics<T extends ImageGray<T>>
        extends BaseCloudPlanarMetrics {
    // Fraction of views it was able to provide a reconstruction for
    public double fractionReconstructed;

    /**
     * Processes an uncalibrated image sequence inside the directory and attempts to reconstruct it
     */
    public boolean process(File directory, ImageSequenceToSparseScene<T> imagesToScene) {
        // Find all images inside the input directory
        List<String> imageNames = loadImageNames(directory);
        if (imageNames == null)
            return false;

        imagesToScene.getGeneratePairwise().setVerbose(System.out, null);
        imagesToScene.getMetricFromPairwise().setVerbose(System.out, null);
        imagesToScene.getRefineScene().setVerbose(System.out, null);

        fractionReconstructed = 0;

        // Compute the sparse scene from the image sequence while noting how long it took
        long time0 = System.currentTimeMillis();
        if (!imagesToScene.process(imageNames, new LookUpImageFilesByPath((path,output)->
                UtilImageIO.loadImage(new File(directory,path).getPath(), true, output))))
            return false;
        processingTimeMS = System.currentTimeMillis() - time0;

        SceneStructureMetric scene = imagesToScene.getSceneStructure();

        DogArray<Point3D_F64> cloud = new DogArray<>(scene.points.size,Point3D_F64::new);
        MultiViewOps.sceneToCloud3(scene, 1e-8, (idx,pt)->cloud.grow().setTo(pt));

        if (!evaluateCloud(directory, imageNames, scene, cloud.toList(), imagesToScene.getImageIdToSceneViewIdx())) {
            out.printf("%20s FAILED_EVALUATE\n",directory.getName());
            return false;
        }
        computeScore(allErrors, allScore);

        fractionReconstructed = imagesToScene.getSceneStructure().views.size / (double) imageNames.size();
        return true;
    }
}
