package boofcv.metrics.mvs;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.sfm.structure.SparseSceneToDenseCloud;
import boofcv.io.geo.MultiViewIO;
import boofcv.io.image.LookUpImageFilesByPath;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageGray;
import gnu.trove.impl.Constants;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.util.List;

/**
 * Given a known scene and corresponding images, compute a dense point cloud and compare results using a planar
 * assumption in marked regions
 *
 * @author Peter Abeles
 */
public class SparseToDenseScenePlanarMetrics<T extends ImageGray<T>>
        extends BaseCloudPlanarMetrics {

    public final static String SCENE_NAME = "scene.yaml";
    public final static String IMAGE_MAP_NAME = "image_to_sbaIdx.csv";


    public boolean process(File directory, SparseSceneToDenseCloud<T> sparseToDense) {
        // Get a list of all file names in this directory
        List<String> imageNames = loadImageNames(directory);
        if (imageNames == null)
            return false;

        // Load all the previously computed scene structure
        SceneStructureMetric scene = MultiViewIO.load(
                new File(directory, SCENE_NAME).getPath(), (SceneStructureMetric) null);
        TIntObjectMap<String> viewIdx_to_ImageID = MultiViewRegressionIO.
                loadMapTIntString(new File(directory,IMAGE_MAP_NAME));
        LookUpImageFilesByPath lookUpImages = new LookUpImageFilesByPath((path,output)->
                UtilImageIO.loadImage(new File(directory,path).getPath(), true, output));

        // Compute the dense cloud
        long time0 = System.currentTimeMillis();
        if (!sparseToDense.process(scene,viewIdx_to_ImageID,lookUpImages)) {
            out.printf("%20s FAILED\n",directory.getName());
            return false;
        }
        processingTimeMS = System.currentTimeMillis()-time0;

        // Score results
        TObjectIntMap<String> imageId_to_viewIdx =
                new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY,Constants.DEFAULT_LOAD_FACTOR,-1);
        viewIdx_to_ImageID.forEachEntry((key,value)->{imageId_to_viewIdx.put(value,key);return true;});
        if (!evaluateCloud(directory, imageNames, scene, sparseToDense.getCloud(), imageId_to_viewIdx)) {
            out.printf("%20s FAILED_EVALUATE\n",directory.getName());
            return false;
        }
        computeScore(allErrors, allScore);

        return true;
    }
}
