package boofcv.metrics.reconstruction;

import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.structure.EpipolarScore3D;
import boofcv.factory.structure.ConfigEpipolarScore3D;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ejml.data.DMatrixRMaj;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Evaluates EpipolarScore3D by giving it synthetic scenes with known relationships.
 *
 * @author Peter Abeles
 */
public class EvaluateEpipolarScore3D {
    public EpipolarScore3D score3D;
    // Individual scenario results are sent to this log
    public PrintStream scenarioLog = System.out;
    // Summary results are sent to this log
    public PrintStream summaryLog = System.out;
    // Errors are sent to this log
    public PrintStream errorLog = System.err;

    // how many trials must have correct classification for it ot be a success
    public static final double SUCCESS_FRACTION = 0.6;
    public static double defaultNoisePixels = 0.5;

    private double cloudDistanceZ = -1;

    // Results for each scenario considered
    DogArray<ScenarioResults> scenarioResults = new DogArray<>(ScenarioResults::new, ScenarioResults::reset);

    public final DogArray_F64 timingMS = new DogArray_F64();

    // Actual camera model
    CameraPinholeBrown cameraA = new CameraPinholeBrown(2);
    CameraPinholeBrown cameraB = new CameraPinholeBrown(2);

    // Passed in "prior" camera model
    CameraPinholeBrown priorA = new CameraPinholeBrown(2);
    CameraPinholeBrown priorB = new CameraPinholeBrown(2);

    Random rand = new Random(32);
    List<Point3D_F64> cloud = new ArrayList<>();

    List<String> scenarioSummaries = new ArrayList<>();

    // work space
    DMatrixRMaj fundamental = new DMatrixRMaj(3, 3);
    DogArray_I32 inliersIdx = new DogArray_I32();
    int featuresA = 0;
    int featuresB = 0;

    public void evaluate() {
        // Reset all data structures
        rand = new Random(32);
        scenarioSummaries.clear();
        timingMS.reset();
        scenarioResults.reset();

        evaluateClassification();
        evaluateRelativeScore();
        evaluateBadPriors();
        evaluateForwardMotion();
        evaluateFocalZoom();

        printClassificationSummary("All Summary", scenarioResults);
        for (String text : scenarioSummaries) {
            summaryLog.print(text);
        }
    }

    private void reset() {
        cameraA.fsetK(500, 500, 0.0, 500, 600, 1000, 1200);
        cameraB.fsetK(600, 600, 0.0, 500, 600, 1000, 1200);
        priorA.setTo(cameraA);
        priorB.setTo(cameraB);
        cloudDistanceZ = 2;
    }

    /**
     * The amount of translation is increased and the number of pairs kept constant. The score should be increasing
     * every frame.
     */
    protected void evaluateRelativeScore() {
        reset();

        DogArray<ScenarioResults> results = new DogArray<>(ScenarioResults::new);

        evaluateRelativeScore(false, results.grow().performance);
        evaluateRelativeScore(true, results.grow().performance);

        int errors = 0;
        int failed = 0; // scenarios it completed failed to classify correctly
        int trials = 0;

        // Score should increase every frame. Number of pairs should be the same too
        int higherScores = 0;
        for (int indexResults = 0; indexResults < results.size; indexResults++) {
            FastAccess<Performance> performance = results.get(indexResults).performance;
            trials += performance.size;

            // check how well it classified the results
            int scenarioErrors = 0;
            for (int i = 1; i < performance.size; i++) {
                if (!performance.get(i).correctClassification) {
                    scenarioErrors++;
                }
            }
            errors += scenarioErrors;
            if (performance.size - scenarioErrors < performance.size * SUCCESS_FRACTION) {
                failed++;
            }

            // See if the geometric score constantly increased
            for (int i = 1; i < performance.size; i++) {
                Performance a = performance.get(i - 1);
                Performance b = performance.get(i);
                if (b.score > a.score)
                    higherScores++;

                // This test is designed to evaluate how well it can evaluate geometric quality and not just the number
                // of inliers so the number of inliers must be the same
                if (a.pairCount != b.pairCount)
                    throw new RuntimeException("Try tuning again");
            }
        }


        scenarioSummaries.add(String.format("%15s: scenarios=%d failed=%d, trails=%d better=%.2f errors=%d\n",
                "Relative Score", results.size, failed,
                trials, higherScores / (double) (trials - scenarioResults.size), errors));

        // Add scenario results to the overall results
        scenarioResults.copyAll(results.toList(), (src, dst) -> dst.setTo(src));
    }

    protected void evaluateRelativeScore(boolean planar, DogArray<Performance> allResults) {
        // Increase the FOV so more points are visible
        cameraA.fx = cameraA.fy = 350;
        cameraB.fx = cameraB.fy = 350;
        priorA.setTo(cameraA);
        priorB.setTo(cameraB);

        scenarioLog.println("relative_score_" + (planar ? "planar" : "general"));
        scenarioLog.println("#  translation, pairs, score, correct inliers, correct classification");

        createCloud(planar, 500);

        for (double translation : new double[]{0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 1.0}) {
            boolean success = evaluateRelativeScore(translation, allResults.grow());
            if (!success) {
                allResults.removeTail();
            }
        }
        scenarioLog.println();
    }

    protected boolean evaluateRelativeScore(double translation, Performance performance) {
        // make it a little bit more interesting by translating along
        double dx = Math.cos(0.1) * translation;
        double dy = Math.sin(0.1) * translation;

        Se3_F64 viewA_to_viewB = SpecialEuclideanOps_F64.eulerXyz(dx, dy, 0.01, 0.01, 0.05, 0.0, null);

        boolean success = evaluateScene(translation != 0.0, 0.0, performance, viewA_to_viewB);
        if (success) {
            scenarioLog.printf("%4.2f %3d %5.1f %5.3f %s\n", translation, performance.pairCount,
                    performance.score, performance.correctInlierFraction, performance.correctClassification);
            timingMS.add(performance.timeMS);
        } else {
            scenarioLog.printf("%3.1f EXCEPTION\n", translation);
        }

        return success;
    }

    /**
     * Passes in different priors for the focal length and sees how that affects the score
     */
    protected void evaluateBadPriors() {
        reset();

        DogArray<ScenarioResults> results = new DogArray<>(ScenarioResults::new);

        evaluateBadPriors(false, results.grow().performance);
        evaluateBadPriors(true, results.grow().performance);

        printClassificationSummary("Priors", results);
    }

    protected void evaluateBadPriors(boolean planar, DogArray<Performance> allResults) {
        scenarioLog.println("bad_prior_" + (planar ? "planar" : "general"));
        scenarioLog.println("#  prior-f, pairs, score, correct inliers, correct classification");

        int numPoints = 500;
        createCloud(planar, numPoints);

        Se3_F64 viewA_to_viewB = SpecialEuclideanOps_F64.eulerXyz(0.3, 0.0, 0.05, 0.01, 0.05, 0.0, null);

        for (double priorFocal : new double[]{50, 75, 100, 200, 300, 400, 500, 700, 1000, 2000, 3000}) {
            priorA.fx = priorA.fy = priorFocal;
            priorB.fx = priorB.fy = priorFocal;

            Performance performance = allResults.grow();
            boolean success = evaluateScene(true, defaultNoisePixels, performance, viewA_to_viewB);
            if (success) {
                scenarioLog.printf("%6.1f %3d %5.1f %5.3f %s\n", priorFocal, performance.pairCount,
                        performance.score, performance.correctInlierFraction, performance.correctClassification);
                timingMS.add(performance.timeMS);
            } else {
                allResults.removeTail();
                scenarioLog.printf("%6.1f EXCEPTION\n", priorFocal);
            }
        }
        scenarioLog.println();
    }

    /**
     * Move the camera forward in the +z direction. This type of motion can be confused with a change in focal length.
     */
    protected void evaluateForwardMotion() {
        DogArray<ScenarioResults> results = new DogArray<>(ScenarioResults::new);
        evaluateForwardMotion(true, results.grow().performance);
        evaluateForwardMotion(false, results.grow().performance);

        printClassificationSummary("ForwardZ", results);
    }

    protected void evaluateForwardMotion(boolean twoCameras, DogArray<Performance> allResults) {
        reset();
        createCloud(false, 500);

        if (!twoCameras) {
            cameraB.setTo(cameraA);
            priorB.setTo(priorA);
        }

        String name = "forward_";
        name += twoCameras ? "two" : "one";

        scenarioLog.println(name);
        scenarioLog.println("#  noise (px), pairs, correct inliers, correct classification");
        for (double z : new double[]{0.05, 0.1, 0.2}) {
            for (double sigma : new double[]{0.0, 0.1, 0.2, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0}) {

                // Specify the relationship as either pure translation or pure rotation
                Se3_F64 viewA_to_viewB = SpecialEuclideanOps_F64.eulerXyz(0.0, 0.0, z, 0.0, 0.0, 0.0, null);
                Performance performance = allResults.grow();

                if (evaluateScene(true, sigma, performance, viewA_to_viewB)) {
                    scenarioLog.printf("%4.2f %3d %5.3f %s\n", sigma, performance.pairCount,
                            performance.correctInlierFraction, performance.correctClassification);
                    timingMS.add(performance.timeMS);
                } else {
                    scenarioLog.printf("%4.2f EXCEPTION\n", sigma);
                }
            }
        }

        scenarioLog.println();
    }

    /**
     * Focal length changes but there's pure rotation
     */
    protected void evaluateFocalZoom() {
        DogArray<ScenarioResults> results = new DogArray<>(ScenarioResults::new);
        evaluateFocalZoom(false, results.grow().performance);
        evaluateFocalZoom(true, results.grow().performance);

        printClassificationSummary("FocalZoom", results);
    }

    protected void evaluateFocalZoom(boolean planar, DogArray<Performance> allResults) {
        reset();

        // There is no actual motion
        Se3_F64 viewA_to_viewB = new Se3_F64();
        createCloud(planar, 500);

        String name = "focal_zoom_" + (planar ? "planar" : "cloud");

        scenarioLog.println(name);
        scenarioLog.println("#  noise (px), pairs, correct inliers, correct classification");
        for (double f : new double[]{800, 1000, 1200}) {
            cameraB.fx = cameraB.fy = f;
            priorB.setTo(cameraB);

            for (double sigma : new double[]{0.0, 0.1, 0.2, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0}) {

                Performance performance = allResults.grow();
                if (evaluateScene(false, sigma, performance, viewA_to_viewB)) {
                    scenarioLog.printf("%4.2f %3d %5.3f %s\n", sigma, performance.pairCount,
                            performance.correctInlierFraction, performance.correctClassification);
                    timingMS.add(performance.timeMS);
                } else {
                    scenarioLog.printf("%4.2f EXCEPTION\n", sigma);
                }
            }
        }

        scenarioLog.println();
    }

    /**
     * Evaluate performance when 3D vs rotation is very clear, but the amount of noise is increase. Tests under
     * general 3D cloud and planar scene.
     */
    protected void evaluateClassification() {
        reset();

        DogArray<ScenarioResults> results = new DogArray<>(ScenarioResults::new);
        evaluateClassification(true, false, results.grow().performance);
        evaluateClassification(false, false, results.grow().performance);
        evaluateClassification(true, true, results.grow().performance);
        evaluateClassification(false, true, results.grow().performance);

        printClassificationSummary("Classification", results);
    }

    protected void evaluateClassification(boolean translate, boolean planar, DogArray<Performance> allResults) {
        String name = "classification_";
        name += translate ? "translate" : "rotate";
        name += "_" + (planar ? "planar" : "general");

        scenarioLog.println(name);
        scenarioLog.println("#  noise (px), pairs, correct inliers, correct classification");
        for (double sigma : new double[]{0.0, 0.1, 0.2, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0}) {
            Performance performance = allResults.grow();
            boolean success = evaluateClassification(translate, planar, sigma, performance);
            if (success) {
                scenarioLog.printf("%4.2f %3d %5.3f %s\n", sigma, performance.pairCount,
                        performance.correctInlierFraction, performance.correctClassification);
                timingMS.add(performance.timeMS);
            } else {
                scenarioLog.printf("%4.2f EXCEPTION\n", sigma);
            }
        }

        scenarioLog.println();
    }

    protected boolean evaluateClassification(boolean translate, boolean planar, double noiseSigma, Performance performance) {
        createCloud(planar, 500);

        // Specify the relationship as either pure translation or pure rotation
        Se3_F64 viewA_to_viewB;
        if (translate)
            viewA_to_viewB = SpecialEuclideanOps_F64.eulerXyz(0.3, -0.03, 0.02, 0.0, 0.0, 0.0, null);
        else
            viewA_to_viewB = SpecialEuclideanOps_F64.eulerXyz(0.0, 0.0, 0.0, -0.05, 0.2, 0.1, null);

        return evaluateScene(translate, noiseSigma, performance, viewA_to_viewB);
    }

    private void createCloud(boolean planar, int numPoints) {
        if (planar) {
            PlaneNormal3D_F64 plane = new PlaneNormal3D_F64(0, 0, cloudDistanceZ, -0.1, 0.04, -1.0);
            cloud = UtilPoint3D_F64.random(plane, 1.0, numPoints, rand);
        } else {
            cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, cloudDistanceZ),
                    -1, 1, -1, 1, -0.5, 0.5, numPoints, rand);
        }
    }

    private boolean evaluateScene(boolean translate, double noiseSigma,
                                  Performance performance, Se3_F64 viewA_to_viewB) {
        DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);
        pairs.reserve(cloud.size());

        // Number of features seen in each view
        featuresA = 0;
        featuresB = 0;

        WorldToCameraToPixel w2a = new WorldToCameraToPixel();
        w2a.configure(cameraA, new Se3_F64());
        WorldToCameraToPixel w2b = new WorldToCameraToPixel();
        w2b.configure(cameraB, viewA_to_viewB);

        // Render observations while adding noise
        for (int i = 0; i < cloud.size(); i++) {
            AssociatedPair pair = pairs.grow();
            Point3D_F64 X = cloud.get(i);

            boolean success = true;

            if (w2a.transform(X, pair.p1)) {
                featuresA++;
                pair.p1.x += rand.nextGaussian() * noiseSigma;
                pair.p1.y += rand.nextGaussian() * noiseSigma;
                success &= BoofMiscOps.isInside(cameraA.width, cameraA.height, pair.p1.x, pair.p1.y);
            } else {
                success = false;
            }

            if (w2b.transform(X, pair.p2)) {
                featuresB++;
                pair.p2.x += rand.nextGaussian() * noiseSigma;
                pair.p2.y += rand.nextGaussian() * noiseSigma;
                success &= BoofMiscOps.isInside(cameraB.width, cameraB.height, pair.p2.x, pair.p2.y);
            } else {
                success = false;
            }

            // if it didn't
            if (!success)
                pairs.removeTail();
        }

        // if the two cameras are identical tell the scoring algorithm
        boolean identical = priorA.isEquals(priorB, 1e-8);

        long time0 = System.nanoTime();
        try {
            score3D.process(priorA, identical ? null : priorB, featuresA, featuresB, pairs.toList(), fundamental, inliersIdx);
        } catch (RuntimeException e) {
            e.printStackTrace(errorLog);
            return false;
        }
        long time1 = System.nanoTime();

        performance.correctInlierFraction = inliersIdx.size / (double) pairs.size;
        performance.correctClassification = translate == score3D.is3D();
        performance.timeMS = (time1 - time0) * 1e-6;
        performance.pairCount = pairs.size;
        performance.score = score3D.getScore();

        return true;
    }

    private void printClassificationSummary(String metricName, DogArray<ScenarioResults> results) {
        // Copy into the overall summary
        if (scenarioResults != results)
            scenarioResults.copyAll(results.toList(), (src, dst) -> dst.setTo(src));

        // Summarize the performance across all scenarios
        double meanInlier = 0.0;
        int totalCorrectClassification = 0;
        int failed = 0; // scenarios it completed failed to classify correctly
        int errors = 0;
        int trials = 0;
        for (ScenarioResults r : results.toList()) {
            trials += r.performance.size;
            int scenarioCorrect = 0;
            for (Performance p : r.performance.toList()) {
                meanInlier += p.correctInlierFraction;
                if (p.correctClassification) {
                    scenarioCorrect++;
                    totalCorrectClassification += 1;
                }
            }
            errors += r.performance.size - scenarioCorrect;
            if (scenarioCorrect < r.performance.size * SUCCESS_FRACTION) {
                failed++;
            }
        }
        meanInlier /= trials;
        double fractionCorrect = totalCorrectClassification / (double) trials;

        scenarioSummaries.add(String.format("%15s: scenarios=%d failed=%d, trials=%d correct=%.2f mean_inlier=%.2f errors=%d\n",
                metricName, results.size, failed, trials, fractionCorrect, meanInlier, errors));
    }

    static class ScenarioResults {
        public DogArray<Performance> performance = new DogArray<>(Performance::new, Performance::reset);

        public void reset() {
            performance.reset();
        }

        public void setTo(ScenarioResults src) {
            performance.copyAll(src.performance.toList(), (s, d) -> d.setTo(s));
        }
    }

    static class Performance {
        double correctInlierFraction;
        boolean correctClassification;
        double timeMS;
        int pairCount;
        double score;

        public void reset() {
            correctInlierFraction = 0.0;
            correctClassification = false;
            timeMS = 0;
            pairCount = 0;
            score = -1;
        }

        public void setTo(Performance src) {
            this.correctInlierFraction = src.correctInlierFraction;
            this.correctClassification = src.correctClassification;
            this.timeMS = src.timeMS;
            this.pairCount = src.pairCount;
            this.score = src.score;
        }
    }

    public static void main(String[] args) {
        ConfigEpipolarScore3D config = new ConfigEpipolarScore3D();
//        config.type = ConfigEpipolarScore3D.Type.FUNDAMENTAL_COMPATIBLE;

        EvaluateEpipolarScore3D evaluator = new EvaluateEpipolarScore3D();
        evaluator.score3D = FactorySceneReconstruction.epipolarScore3D(config);
//        evaluator.score3D.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
        evaluator.evaluateFocalZoom();
//        evaluator.evaluate();
    }
}
