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
    public PrintStream metricsLog = System.out;
    public PrintStream errorLog = System.err;

    public static double defaultNoisePixels = 0.5;
    public static double defaultInlierFraction = 0.75;

    private double cloudDistanceZ = -1;

    // Used to compute overall summary of results
    DogArray<Performance> overallResults = new DogArray<>(Performance::new);

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
    int failures = 0;
    int featuresA = 0;
    int featuresB = 0;

    public void evaluate() {
        // Reset all data structures
        rand = new Random(32);
        scenarioSummaries.clear();
        timingMS.reset();
        overallResults.reset();

        evaluateClassification();
        evaluateRelativeScore();
        evaluateBadPriors();

        printClassificationSummary("All Summary", overallResults);
        for (String text : scenarioSummaries) {
            metricsLog.print(text);
        }

        // One last metric to see if there's a serious problem that summary results won't highlight
        computeMostFailuresInARow();
    }

    /**
     * Little bit of a hack, but this is intended to detect situations where it constantly failures due to
     * an unhandled degerate case
     */
    private void computeMostFailuresInARow() {
        int mostFailuresInARow = 0;
        int failuresInARow = 0;
        for (int i = 1; i < overallResults.size; i++) {
            if (overallResults.get(i).correctClassification) {
                failuresInARow = 0;
                continue;
            }

            failuresInARow++;
            if (failuresInARow > mostFailuresInARow)
                mostFailuresInARow = failuresInARow;
        }
        metricsLog.println("Most miss classifications in a row: " + mostFailuresInARow);
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

        failures = 0;
        DogArray<Performance> scenarioResults = new DogArray<>(Performance::new);

        evaluateRelativeScore(false, scenarioResults);
        evaluateRelativeScore(true, scenarioResults);

        // Score should increase every frame. Number of pairs should be the same too
        int higherScores = 0;
        for (int i = 1; i < scenarioResults.size; i++) {
            Performance a = scenarioResults.get(i - 1);
            Performance b = scenarioResults.get(i);
            if (b.score > a.score)
                higherScores++;

            // Make this as a failure so that it gets debugged. simulation parameters should be tuned so that
            // the pair count is constant and only geometric information is available to compare the scores
            if (a.pairCount != b.pairCount)
                failures++;
        }

        scenarioSummaries.add(String.format("Relative Score: N=%d, better=%.2f, failed=%d\n",
                scenarioResults.size, higherScores / (double) (scenarioResults.size - 1), failures));

        // Add scenario results to the overall results
        overallResults.copyAll(scenarioResults.toList(), (src, dst) -> dst.setTo(src));
    }

    protected void evaluateRelativeScore(boolean planar, DogArray<Performance> allResults) {
        // Increase the FOV so more points are visible
        cameraA.fx = cameraA.fy = 350;
        cameraB.fx = cameraB.fy = 350;
        priorA.setTo(cameraA);
        priorB.setTo(cameraB);

        metricsLog.println("relative_score_" + (planar ? "planar" : "general"));
        metricsLog.println("#  translation, pairs, score, correct inliers, correct classification");

        createCloud(planar, 500);

        for (double translation : new double[]{0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 1.0}) {
            boolean success = evaluateRelativeScore(translation, allResults.grow());
            if (!success) {
                allResults.removeTail();
                failures++;
            }
        }
        metricsLog.println();
    }

    protected boolean evaluateRelativeScore(double translation, Performance performance) {
        // make it a little bit more interesting by translating along
        double dx = Math.cos(0.1) * translation;
        double dy = Math.sin(0.1) * translation;

        Se3_F64 viewA_to_viewB = SpecialEuclideanOps_F64.eulerXyz(dx, dy, 0.01, 0.01, 0.05, 0.0, null);

        boolean success = evaluateScene(translation != 0.0, 0.0, performance, viewA_to_viewB);
        if (success) {
            metricsLog.printf("%4.2f %3d %5.1f %5.3f %s\n", translation, performance.pairCount,
                    performance.score, performance.correctInlierFraction, performance.correctClassification);
            timingMS.add(performance.timeMS);
        } else {
            metricsLog.printf("%3.1f EXCEPTION\n", translation);
        }

        return success;
    }

    /**
     * Passes in different priors for the focal length and sees how that affects the score
     */
    protected void evaluateBadPriors() {
        reset();

        failures = 0;
        DogArray<Performance> scenarioResults = new DogArray<>(Performance::new);

        evaluateBadPriors(false, scenarioResults);
        evaluateBadPriors(true, scenarioResults);

        printClassificationSummary("Priors", scenarioResults);
    }

    protected void evaluateBadPriors(boolean planar, DogArray<Performance> allResults) {
        metricsLog.println("bad_prior_" + (planar ? "planar" : "general"));
        metricsLog.println("#  prior-f, pairs, score, correct inliers, correct classification");

        int numPoints = 500;
        createCloud(planar, numPoints);

        Se3_F64 viewA_to_viewB = SpecialEuclideanOps_F64.eulerXyz(0.3, 0.0, 0.05, 0.01, 0.05, 0.0, null);

        for (double priorFocal : new double[]{50, 75, 100, 200, 300, 400, 500, 700, 1000, 2000, 3000}) {
            priorA.fx = priorA.fy = priorFocal;
            priorB.fx = priorB.fy = priorFocal;

            Performance performance = allResults.grow();
            boolean success = evaluateScene(true, defaultNoisePixels, performance, viewA_to_viewB);
            if (success) {
                metricsLog.printf("%6.1f %3d %5.1f %5.3f %s\n", priorFocal, performance.pairCount,
                        performance.score, performance.correctInlierFraction, performance.correctClassification);
                timingMS.add(performance.timeMS);
            } else {
                allResults.removeTail();
                failures++;
                metricsLog.printf("%6.1f EXCEPTION\n", priorFocal);
            }
        }
        metricsLog.println();
    }

    /**
     * Evalute performance when 3D vs rotation is very clear, but the amount of noise is increase. Tests under
     * general 3D cloud and planar scene.
     */
    protected void evaluateClassification() {
        reset();
        failures = 0;

        DogArray<Performance> scenarioResults = new DogArray<>(Performance::new);
        evaluateClassification(true, false, scenarioResults);
        evaluateClassification(false, false, scenarioResults);
        evaluateClassification(true, true, scenarioResults);
        evaluateClassification(false, true, scenarioResults);

        printClassificationSummary("Classification", scenarioResults);
    }

    private void printClassificationSummary(String metricName, DogArray<Performance> scenarioResults) {
        // Copy into the overall summary
        if (scenarioResults != overallResults)
            overallResults.copyAll(scenarioResults.toList(), (src, dst) -> dst.setTo(src));

        // Summarize the performance across all scenarios
        double meanInlier = 0.0;
        int totalCorrectClassification = 0;
        for (Performance p : scenarioResults.toList()) {
            meanInlier += p.correctInlierFraction;
            if (p.correctClassification)
                totalCorrectClassification += 1;
        }
        meanInlier /= scenarioResults.size;
        double fractionCorrect = totalCorrectClassification / (double) scenarioResults.size;

        scenarioSummaries.add(String.format("%s: N=%d, correct=%.2f, mean_inlier=%.2f, failures=%d\n",
                metricName, scenarioResults.size, fractionCorrect, meanInlier, failures));
    }

    protected void evaluateClassification(boolean translate, boolean planar, DogArray<Performance> allResults) {
        String name = "classification_";
        name += translate ? "translate" : "rotate";
        name += "_" + (planar ? "planar" : "general");

        metricsLog.println(name);
        metricsLog.println("#  noise (px), pairs, correct inliers, correct classification");
        for (double sigma : new double[]{0.0, 0.1, 0.2, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0}) {
            Performance performance = allResults.grow();
            boolean success = evaluateClassification(translate, planar, sigma, performance);
            if (success) {
                metricsLog.printf("%4.2f %3d %5.3f %s\n", sigma, performance.pairCount,
                        performance.correctInlierFraction, performance.correctClassification);
                timingMS.add(performance.timeMS);
            } else {
                metricsLog.printf("%4.2f EXCEPTION\n", sigma);
                failures++;
            }
        }

        metricsLog.println();
    }

    protected boolean evaluateClassification(boolean translate, boolean planar, double noiseSigma, Performance performance) {
        int numPoints = 500;
        createCloud(planar, numPoints);

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
                    -1, 1, -1, 1, -0.5, 0.5,numPoints, rand);
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

    static class Performance {
        double correctInlierFraction;
        boolean correctClassification;
        double timeMS;
        int pairCount;
        double score;

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
//        evaluator.evaluateRelativeScore();
        evaluator.evaluate();
    }
}
