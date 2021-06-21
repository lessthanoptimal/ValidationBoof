package boofcv.metrics.mvs;

import boofcv.BoofVerbose;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.similar.ConfigSimilarImagesSceneRecognition;
import boofcv.alg.similar.ConfigSimilarImagesTrackThenMatch;
import boofcv.alg.similar.SimilarImagesSceneRecognition;
import boofcv.alg.similar.SimilarImagesTrackThenMatch;
import boofcv.alg.structure.*;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.factory.structure.ConfigGeneratePairwiseImageGraph;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.io.UtilIO;
import boofcv.io.wrapper.images.LoadFileImageSequence2;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point3D_F64;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Metrics for uncalibrated image reconstruction. Processes all the images in a directory and uses known planes
 * to score how well the reconstruction went.
 *
 * @author Peter Abeles
 */
public class UncalibratedToSparseScenePlanarMetrics<T extends ImageGray<T>>
        extends BaseCloudPlanarMetrics {

    // Configurations it will use
    public ConfigPointTracker configTracker = FactorySceneRecognition.createDefaultTrackerConfig();
    public ConfigSimilarImagesTrackThenMatch configSimilarTracker = new ConfigSimilarImagesTrackThenMatch();
    public ConfigSimilarImagesSceneRecognition configSimilarUnordered = new ConfigSimilarImagesSceneRecognition();

    public ConfigGeneratePairwiseImageGraph configPairwise = new ConfigGeneratePairwiseImageGraph();

    // Fraction of views it was able to provide a reconstruction for
    public double fractionReconstructed;

    // Storage for intermediate results
    PairwiseImageGraph pairwise = null;
    LookUpSimilarImages dbSimilar;
    LookUpCameraInfo dbCams;
    SceneStructureMetric scene = null;

    public long timeSimilarMS;
    public long timePairwiseMS;
    public long timeMetricMS;
    public long timeBundleMS;

    public UncalibratedToSparseScenePlanarMetrics() {
    }

    /**
     * Processes an uncalibrated image sequence inside the directory and attempts to reconstruct it
     */
    public boolean processSequence(File directory) {
        processingTimeMS = 0;
        fractionReconstructed = 0;

        // Find all images inside the input directory
        List<String> imageNames = UtilIO.listSmartImages(directory.getPath(), true);
        if (imageNames.isEmpty())
            return false;

        SimilarImagesTrackThenMatch<GrayU8, ?> similarImages = FactorySceneReconstruction.createTrackThenMatch(configSimilarTracker, ImageType.SB_U8);
        PointTracker<GrayU8> tracker = FactoryPointTracker.tracker(configTracker, GrayU8.class, null);
        List<PointTrack> activeTracks = new ArrayList<>();

        LoadFileImageSequence2<GrayU8> images = new LoadFileImageSequence2<>(imageNames, ImageType.SB_U8);

        // Compute the sparse scene from the image sequence while noting how long it took
        long time0 = System.currentTimeMillis();
        similarImages.initialize(images.getWidth(), images.getHeight());
        dbCams = new LookUpCameraInfo();
        dbCams.addCameraCanonical(images.getWidth(), images.getHeight(), 60);
        while (images.hasNext()) {
            GrayU8 gray = images.next();
            tracker.process(gray);
            tracker.spawnTracks();
            tracker.getActiveTracks(activeTracks);
            similarImages.processFrame(gray, activeTracks, tracker.getFrameID());
            dbCams.addView(similarImages.frames.getTail().frameID, 0);
        }
        similarImages.finishedTracking();
        timeSimilarMS = System.currentTimeMillis() - time0;
        this.dbSimilar = similarImages;

        return computeReconstruction(directory, imageNames);
    }

    public boolean processUnordered(File directory) {
        processingTimeMS = 0;
        fractionReconstructed = 0;

        // Find all images inside the input directory
        List<String> imageNames = UtilIO.listSmartImages(directory.getPath(), true);
        if (imageNames.isEmpty())
            return false;

        SimilarImagesSceneRecognition<GrayU8, ?> similarImages = FactorySceneReconstruction.createSimilarImages(configSimilarUnordered, ImageType.SB_U8);

        LoadFileImageSequence2<GrayU8> images = new LoadFileImageSequence2<>(imageNames, ImageType.SB_U8);

        // Compute the sparse scene from the image sequence while noting how long it took
        long time0 = System.currentTimeMillis();
        dbCams = new LookUpCameraInfo();
        dbCams.addCameraCanonical(images.getWidth(), images.getHeight(), 60);
        while (images.hasNext()) {
            GrayU8 gray = images.next();
            String viewID = images.getFrameNumber() + "";
            similarImages.addImage(viewID, gray);
            dbCams.addView(viewID, 0);
        }
        similarImages.fixate();
        timeSimilarMS = System.currentTimeMillis() - time0;
        this.dbSimilar = similarImages;

        return computeReconstruction(directory, imageNames);
    }

    private boolean computeReconstruction(File directory, List<String> imageNames) {
        computePairwise();
        SceneWorkingGraph working = computeMetric();
        if (working == null) {
            err.printf("%20s FAILED_METRIC\n", directory.getName());
            return false;
        }

        if (!bundleAdjustmentRefine(working)) {
            err.printf("%20s FAILED_SBA\n", directory.getName());
            return false;
        }

        TObjectIntMap<String> image_to_viewSbaIdx = new TObjectIntHashMap<>();
        for (int idx = 0; idx < working.listViews.size(); idx++) {
            image_to_viewSbaIdx.put(imageNames.get(idx), idx);
        }

        DogArray<Point3D_F64> cloud = new DogArray<>(scene.points.size, Point3D_F64::new);
        MultiViewOps.sceneToCloud3(scene, 1e-8, (idx, pt) -> cloud.grow().setTo(pt));

        if (!evaluateCloud(directory, imageNames, scene, cloud.toList(), image_to_viewSbaIdx)) {
            err.printf("%20s FAILED_EVALUATE\n", directory.getName());
            return false;
        }
        computeScore(allErrors, allScore);

        // Intentionally printing to stdout as this is logged in runtime
        System.out.printf("Time (s): similar=%.1f pairwise=%.1f metric=%.1f bundle=%.1f\n",
                timeSimilarMS / 1000.0, timePairwiseMS / 1000.0, timeMetricMS / 1000.0, timeBundleMS / 1000.0);

        fractionReconstructed = image_to_viewSbaIdx.size() / (double) imageNames.size();
        processingTimeMS += timeSimilarMS + timePairwiseMS + timeMetricMS + timeBundleMS;
        return true;
    }

    private void computePairwise() {
        out.println("  computing pairwise");
        GeneratePairwiseImageGraph generatePairwise = FactorySceneReconstruction.generatePairwise(configPairwise);
        generatePairwise.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));

        long time0 = System.currentTimeMillis();
        generatePairwise.process(dbSimilar, dbCams);
        pairwise = generatePairwise.getGraph();
        timePairwiseMS = System.currentTimeMillis() - time0;
    }

    private @Nullable
    SceneWorkingGraph computeMetric() {
        out.println("  computing metric");
        MetricFromUncalibratedPairwiseGraph metric = new MetricFromUncalibratedPairwiseGraph();
        metric.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));

        long time0 = System.currentTimeMillis();
        if (!metric.process(dbSimilar, dbCams, pairwise)) {
            return null;
        }
        timeMetricMS = System.currentTimeMillis() - time0;

        return metric.getLargestScene();
    }

    public boolean bundleAdjustmentRefine(SceneWorkingGraph working) {
        out.println("  computing final bundle adjustment");
        RefineMetricWorkingGraph refine = new RefineMetricWorkingGraph();
        long time0 = System.currentTimeMillis();
        // Bundle adjustment is run twice, with the worse 5% of points discarded in an attempt to reduce noise
        refine.metricSba.keepFraction = 0.95;
        if (!refine.process(dbSimilar, working)) {
            return false;
        }
        scene = refine.metricSba.structure;
        timeBundleMS = System.currentTimeMillis() - time0;
        return true;
    }
}
