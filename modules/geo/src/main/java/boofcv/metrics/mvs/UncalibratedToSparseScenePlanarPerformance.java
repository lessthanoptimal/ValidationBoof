package boofcv.metrics.mvs;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.sfm.structure.ImageSequenceToSparseScene;
import boofcv.io.UtilIO;
import boofcv.io.image.LabeledImagePolygonCodec;
import boofcv.io.image.LookUpImageFilesByPath;
import boofcv.io.image.PolygonRegion;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageGray;
import georegression.fitting.plane.GeneratorPlaneGeneral3D_F64;
import georegression.fitting.plane.ModelManagerPlaneGeneral3D_F64;
import georegression.fitting.plane.PointDistanceFromPlaneGeneral_F64;
import georegression.geometry.UtilPlane3D_F64;
import georegression.metric.ClosestPoint3D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.plane.PlaneGeneral3D_F64;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;
import org.ddogleg.stats.StatisticsDogArray;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Metrics for uncalibrated image reconstruction. Processes all the images in a directory and uses known planes
 * to score how well the reconstruction went.
 *
 * @author Peter Abeles
 */
public class UncalibratedToSparseScenePlanarPerformance<T extends ImageGray<T>> {
    public PrintStream out = System.out;
    public PrintStream err = System.err;

    /**
     * The output score for each region
     */
    public final DogArray<RegionScore> regionScore = new DogArray<>(RegionScore::new, RegionScore::reset);

    /**
     * Score summarized across all regions
     */
    public final RegionScore allScore = new RegionScore();

    /**
     * Processing time in milliseconds
     */
    public long processingTimeMS;

    //--------------- Summary statistics
    //
    // total number of regions with points inside
    public int totalRegions;
    // regions that were skipped because of no points
    public int missedRegions;
    // Images that were skipped because scene reconstruction had no estimate for that view
    public int totalSkippedImages;
    // Fraction of views it was able to provide a reconstruction for
    public double fractionReconstructed;

    public boolean failed;

    //--------- Workspace
    private DogArray_F64 allErrors = new DogArray_F64();

    // Use a robust fit to the plane to avoid outliers skewing results
    ModelMatcher<PlaneGeneral3D_F64,Point3D_F64> fitPlane;

    public UncalibratedToSparseScenePlanarPerformance() {
        // use Least Median because we don't know what a reasonable value is for inlier error in advance
        fitPlane = new LeastMedianOfSquares<>(0xBEEF,100,new ModelManagerPlaneGeneral3D_F64(),
                new GeneratorPlaneGeneral3D_F64(), new PointDistanceFromPlaneGeneral_F64());
    }

    /**
     * Processes an uncalibrated image sequence inside the directory and attempts to reconstruct it
     */
    public boolean process(File directory, ImageSequenceToSparseScene<T> imagesToScene) {
        // Find all images inside the input directory
        List<String> imagePaths = findImages(directory);
        if (imagePaths == null) {
            return false;
        }

        imagesToScene.getGeneratePairwise().setVerbose(System.out, null);
        imagesToScene.getMetricFromPairwise().setVerbose(System.out, null);
        imagesToScene.getRefineScene().setVerbose(System.out, null);

        // reset summary statistics
        totalRegions = 0;
        totalSkippedImages = 0;
        missedRegions = 0;
        allErrors.reset();
        failed = false;

        // Print header
        out.println("# directory=" + directory.getPath()+" error in pixels");
        out.println("# View, Point Count, mean, p50, p95, p100");
        out.println();

        // Compute the sparse scene from the image sequence while noting how long it took
        long time0 = System.currentTimeMillis();
        if (!imagesToScene.process(imagePaths, new LookUpImageFilesByPath()))
            return false;
        processingTimeMS = System.currentTimeMillis() - time0;


        TObjectIntMap<String> image_to_viewSbaIdx = imagesToScene.getImageIdToSceneViewIdx();

        // For each image with a set of labeled image planes, compute a plane fit score
        BoofMiscOps.forIdx(imagePaths, (imageIdx, imagePath) -> {
            File imageFile = new File(imagePath);

            String name = FilenameUtils.getBaseName(imageFile.getName()) + "_polygons.txt";
            File file = new File(imageFile.getParent(), name);
            if (!file.exists())
                return;

            ImageDimension shape = new ImageDimension();
            DogArray<PolygonRegion> regions = new DogArray<>(PolygonRegion::new);
            try {
                LabeledImagePolygonCodec.decode(new FileInputStream(file), shape, regions);
            } catch (IOException e) {
                e.printStackTrace(err);
                return;
            }

            // See if this image was used in the reconstruction. If not skip it.
            if (!image_to_viewSbaIdx.containsKey(imagePath)) {
                totalSkippedImages++;
                return;
            }

            int indexSbaIdx = image_to_viewSbaIdx.get(imagePath);

            scorePlanarRegions(regions, shape, indexSbaIdx, imagesToScene.getSceneStructure());
            out.println(imageFile.getName());
            regionScore.forIdx((idx, score) -> {
                out.printf("%2d %4d %5.1f %5.1f %5.1f %5.1f\n",
                        idx, score.count, score.mean, score.p50, score.p95, score.p100);
                // if no points were found in the region just move along
                if (score.count==0) {
                    missedRegions++;
                    return;
                }
                totalRegions += 1;
            });
            out.flush();
        });

        fractionReconstructed = imagesToScene.getSceneStructure().views.size/(double)imagePaths.size();

        computeScore(allErrors, allScore);
        return true;
    }

    private void scorePlanarRegions(DogArray<PolygonRegion> regions,
                                    ImageDimension shape,
                                    int indexSbaIdx,
                                    SceneStructureMetric scene) {
        DogArray<RegionPoints> regionPoints = new DogArray<>(regions.size, RegionPoints::new);
        regionPoints.resize(regions.size);

        // Pixels are shifted so that the image center is the origin. This compensates for that.
        int radiusX = shape.width / 2;
        int radiusY = shape.height / 2;

        // Find which points belong to which region / plane
        projectCloudToRegions(regions, shape, indexSbaIdx, scene, regionPoints, radiusX, radiusY);

        // Compute the score for each region
        scoreReprojectionError(regions, indexSbaIdx, scene, regionPoints, radiusX, radiusY);
    }

    private void projectCloudToRegions(DogArray<PolygonRegion> regions, ImageDimension shape,
                                       int indexSbaIdx, SceneStructureMetric scene,
                                       DogArray<RegionPoints> regionPoints, int radiusX, int radiusY) {
        MultiViewOps.scenePointsToPixels(scene, indexSbaIdx, (pointIdx, camPt, pixel) -> {
            // if it's behind the camera it's not visible
            if (camPt.z < 0.0)
                return;

            pixel.x += radiusX;
            pixel.y += radiusY;

            // make sure it was projected onto the image
            if (pixel.x < 0.0 || pixel.y < 0.0 || pixel.x >= shape.width || pixel.y >= shape.height)
                return;

            // See which region this projects into
            int regionIdx = regions.findIdx(r -> Intersection2D_F64.containsConcave(r.polygon, pixel));
            if (regionIdx < 0)
                return;

            // add the point to the region
            double w = scene.isHomogenous() ? scene.points.get(pointIdx).getW() : 1.0;
            // skip points at infinity since we can't fit a plane to those
            if ( camPt.norm() >= 1e8*w)
                return;

            regionPoints.get(regionIdx).cloud.grow().setTo(camPt.x/w, camPt.y/w, camPt.z/w);
            regionPoints.get(regionIdx).pixels.grow().setTo(pixel);
        });
    }

    private void scoreReprojectionError(DogArray<PolygonRegion> regions,
                                        int indexSbaIdx, SceneStructureMetric scene,
                                        DogArray<RegionPoints> regionPoints, int radiusX, int radiusY) {
        regionScore.reset();
        regionScore.resize(regions.size);

        // Storage for reprojection errors. Later used to sort and compute metrics from
        DogArray_F64 regionErrors = new DogArray_F64();

        // Used to project points in camera coordinates on to the image
        BundleAdjustmentCamera camera = Objects.requireNonNull(scene.getViewCamera(scene.views.get(indexSbaIdx)).model);

        // workspace
        PlaneNormal3D_F64 planeNorm = new PlaneNormal3D_F64();
        Point3D_F64 closest = new Point3D_F64();
        Point2D_F64 projectedClosest = new Point2D_F64();

        regionPoints.forIdx((idx, points) -> {
            RegionScore score = regionScore.get(idx);
            score.count = points.size();

            // if there are not enough points to fit a plane, give up and go to the next region
            if (points.size() < 4) {
                return;
            }

            if (!fitPlane.process(points.cloud.toList())) {
                err.println("Failed to fit plane. regionIdx=" + idx);
                return;
            }
            UtilPlane3D_F64.convert(fitPlane.getModelParameters(), planeNorm);

            regionErrors.resize(points.size());
            for (int cloudIdx = 0; cloudIdx < points.pixels.size; cloudIdx++) {
                Point3D_F64 p3 = points.cloud.get(cloudIdx);

                // find closest point on plane to 3D point
                ClosestPoint3D_F64.closestPoint(planeNorm, p3, closest);

                // Project the point on the plane onto the image
                camera.project(closest.x, closest.y, closest.z, projectedClosest);

                // Compensate for image center offset
                projectedClosest.x += radiusX;
                projectedClosest.y += radiusY;

                // Pixel error
                double error = projectedClosest.distance(points.pixels.get(cloudIdx));
                regionErrors.set(cloudIdx, error);
            }

            // Compute various statistics
            computeScore(regionErrors, score);

            allErrors.addAll(regionErrors);
        });
    }

    private void computeScore(DogArray_F64 regionErrors, RegionScore score) {
        regionErrors.sort();
        score.count = regionErrors.size;
        score.p50 = regionErrors.getFraction(0.5);
        score.p95 = regionErrors.getFraction(0.95);
        score.p100 = regionErrors.getFraction(1.0);
        score.mean = StatisticsDogArray.mean(regionErrors);
    }

    private List<String> findImages(File directory) {
        List<String> imagePaths = UtilIO.listByRegex(directory.getPath(), "([^\\s]+(\\.(?i)(jpe?g|png|gif|bmp))$)");
        if (imagePaths.isEmpty()) {
            err.println("No images found at " + directory.getPath());
            return null;
        }
        Collections.sort(imagePaths);

        imagePaths = imagePaths.stream().filter(imagePath -> {
            File f = new File(imagePath);
            return !(f.isDirectory() || f.isHidden()) && UtilImageIO.isImage(f);
        }).collect(Collectors.toList());

        if (imagePaths.isEmpty()) {
            err.println("No images found at " + directory.getPath());
            return null;
        }
        return imagePaths;
    }

    public static class RegionPoints {
        // point cloud of points which project onto the plane in world
        public DogArray<Point3D_F64> cloud = new DogArray<>(Point3D_F64::new);
        // points projected onto the image
        public DogArray<Point2D_F64> pixels = new DogArray<>(Point2D_F64::new);

        public int size() {
            return pixels.size;
        }
    }

    /**
     * Reprojection error statistics for a region
     */
    public static class RegionScore {
        public int count;
        public double mean;
        public double p50;
        public double p95;
        public double p100;

        public void reset() {
            count = -1;
            mean = Double.NaN;
            p50 = Double.NaN;
            p95 = Double.NaN;
            p100 = Double.NaN;
        }
    }
}
