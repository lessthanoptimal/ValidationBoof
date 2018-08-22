package boofcv.metrics.sfm;

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.common.DiscreteRange;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import georegression.transform.InvertibleTransformSequence;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TODO grid distance pixel-noise
 * TODO grid distance orientation and fixed noise
 *
 * @author Peter Abeles
 */
public class GeneratePnPObservation {
    Random random;

    // Camera parameters
    double hfov; // degrees
    int imageWidth;
    int imageHeight;
    CameraPinhole intrinsic = new CameraPinhole();
    Point2Transform2_F64 normToPixel;
    Point2Transform2_F64 pixelToNorm;

    // Features in marker coordinate system
    List<Point3D_F64> marker = new ArrayList<>();

    // Amount of pixel noise added. Normal distribution
    double stdevPixel = 0;

    File directory = new File(".");

    /**
     * Configures simulation camera
     *
     * @param hfov Camera's horizontal FOV in degrees
     * @param width Image width in pixels
     * @param height Image height in pixels
     */
    public GeneratePnPObservation(double hfov, int width, int height) {
        this.hfov = hfov;
        this.imageWidth = width;
        this.imageHeight = height;

        double focalLength = (imageWidth / 2) / Math.tan(UtilAngle.radian(hfov) / 2);
        intrinsic.fsetK(focalLength, focalLength, 0, imageWidth / 2, imageHeight / 2, imageWidth, imageHeight);

        normToPixel = new LensDistortionPinhole(intrinsic).distort_F64(false, true);
        pixelToNorm = new LensDistortionPinhole(intrinsic).undistort_F64(true, false);
    }

    public void initialize( double stdevPixel, String path ) {
        this.stdevPixel = stdevPixel;
        directory = new File(path);

        if( directory.exists() ) {
            try {
                FileUtils.deleteDirectory(directory);
            } catch( IOException e ) {
                throw new RuntimeException(e);
            }
        }
        if( !directory.mkdirs())
            throw new RuntimeException("Can't create directories. "+directory.getPath());
    }

    /**
     * Planar target with square sides
     * @param length Length of side in world units
     */
    public void targetSquare(double length) {
        double r = length/2;
        marker.clear();
        marker.add(new Point3D_F64(-r, -r, 0));
        marker.add(new Point3D_F64(r, -r, 0));
        marker.add(new Point3D_F64(r, r, 0));
        marker.add(new Point3D_F64(-r, r, 0));
    }

    /**
     * Generates a set of files with with each file having targets at a specified distance. Targets will appear
     * at all locations throughout the image and be tilted using a uniform distribution
     *
     * @param range Describes discrete distances of each target
     * @param maxTilt maximum tilt applied to object in degrees
     * @param trialsPerSet Number of trials at is discrete distance
     */
    public void generateUniformImageDiscreteDistances(DiscreteRange range, double maxTilt, int trialsPerSet) {
        Random seedGenerator = new Random(0xDEADBEEF);

        maxTilt = UtilAngle.radian(maxTilt);

        for (int i = 0; i < range.count; i++) {
            long seed = seedGenerator.nextLong();
            random = new Random(seed);

            double distance = range.min + i * (range.max - range.min) / (range.count - 1);

            List<Se3_F64> BodyToCameras = new ArrayList<>();
            List<List<Point2D_F64>> observations = new ArrayList<>();

            simulate(distance, maxTilt, trialsPerSet, BodyToCameras, observations,true,false);

            File file = new File(directory, String.format("range_%02d.txt", i));

            System.out.println("Creating "+file.getPath());
            try {
                PrintStream output = new PrintStream(file);
                saveSimulation(marker, BodyToCameras, observations, seed, output);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Target is dead center in the image and the target's orientation is adjusted by fixed angle amounts
     * @param angles
     * @param distance
     * @param trialsPerSet
     */
    public void generateUniformImageDiscreteAngles(DiscreteRange angles, double distance, int trialsPerSet) {
        Random seedGenerator = new Random(0xDEADBEEF);

        for (int i = 0; i < angles.count; i++) {
            long seed = seedGenerator.nextLong();
            random = new Random(seed);

            double angle = angles.min + i * (angles.max - angles.min) / (angles.count - 1);
            angle = UtilAngle.radian(angle);

            List<Se3_F64> BodyToCameras = new ArrayList<>();
            List<List<Point2D_F64>> observations = new ArrayList<>();

            simulate(distance, angle, trialsPerSet, BodyToCameras, observations,false,true);

            File file = new File(directory, String.format("center_angle_%02d.txt", i));

            System.out.println("Creating "+file.getPath());
            try {
                PrintStream output = new PrintStream(file);
                saveSimulation(marker, BodyToCameras, observations, seed, output);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Generates simulated data where every point is the specified distance from the camera
     * @param distance distance marker is from camera's origin
     * @param angle Maximum allowed tilt of marker relative to camera axis
     * @param numTrials Number of MC trials
     * @param BodyToCameras (Output)
     * @param observations (Output)
     */
    void simulate(double distance, double angle, int numTrials,
                       List<Se3_F64> BodyToCameras, List<List<Point2D_F64>> observations,
                       boolean randomTilt , boolean centerImage ) {
        // Coordinate frame transform. body is the marker's body frame
        Se3_F64 BodyToUp = new Se3_F64();
        Se3_F64 UpToTilt = new Se3_F64();
        Se3_F64 TiltToZ = new Se3_F64();
        Se3_F64 ZToCamera = new Se3_F64();
        Se3_F64 BodyToCamera = new Se3_F64();

        // transform to make the marker facing the camera
        ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, 0, 0, BodyToUp.R);

        // Moves the marker to the specified distance from the camera's origin
        TiltToZ.T.set(0, 0, distance);

        // Specifies transform from body to camera reference frames
        InvertibleTransformSequence<Se3_F64> sequence = new InvertibleTransformSequence<>();
        sequence.addTransform(true, BodyToUp);
        sequence.addTransform(true, UpToTilt);
        sequence.addTransform(true, TiltToZ);
        sequence.addTransform(true, ZToCamera);

        Point2D_F64 centerPixel = new Point2D_F64();
        for (int trial = 0; trial < numTrials; trial++) {
            List<Point2D_F64> pixels = new ArrayList<>();
            for (int i = 0; i < marker.size(); i++) {
                pixels.add(new Point2D_F64());
            }

            // Each attempt's goal is to randomly select a trial which is visible inside the camera
            boolean success = false;
            for (int attempt = 0; attempt < 10000; attempt++) {
                // Make it so that the marker doesn't perfectly look at the camera
                double tiltAngle = randomTilt ? 2.0 * (random.nextDouble() - 0.5) * angle : angle;
                randomTilt(tiltAngle, UpToTilt);

                if( !centerImage ) {
                    // randomly select a pixel to be target's center
                    centerPixel.x = random.nextDouble() * (imageWidth - 1);
                    centerPixel.y = random.nextDouble() * (imageHeight - 1);
                    rotateToPixel(centerPixel, ZToCamera);
                }

                sequence.computeTransform(BodyToCamera);

                if (renderMarker(marker, BodyToCamera, pixels)) {
                    sanityCheckObservations(pixels);
                    success = true;
                    observations.add(pixels);
                    BodyToCameras.add(BodyToCamera.copy());
                    break;
                }
            }

            if (!success) {
                throw new RuntimeException("Failed to find a configuration which was visible");
            }
        }
    }

    /**
     * Print a warning if the length of a side is very small and unrealistic to be detected
     */
    private void sanityCheckObservations( List<Point2D_F64> pixels ) {
        for (int i = 0, j = pixels.size()-1; i < pixels.size(); j=i,i++) {
            double d = pixels.get(i).distance(pixels.get(j));

            if( d <= 4) {
                System.err.println("Side less than 5 pixels");
                break;
            }
        }
    }

    /**
     * Render marker in image
     *
     * @param marker points in marker reference frame
     * @param BodyToCamera Transform from marker body to camera
     * @param pixels (Output) location of marker in image
     * @return true if marker is in view and was rendered
     */
     boolean renderMarker(List<Point3D_F64> marker, Se3_F64 BodyToCamera, List<Point2D_F64> pixels) {
        Point3D_F64 p3 = new Point3D_F64();
        for (int indexCorner = 0; indexCorner < marker.size(); indexCorner++) {
            BodyToCamera.transform(marker.get(indexCorner), p3);

            if( p3.z <= 0 )
                return false;

            Point2D_F64 pixel = pixels.get(indexCorner);
            normToPixel.compute(p3.x / p3.z, p3.y / p3.z, pixel);

            pixel.x += random.nextGaussian() * stdevPixel;
            pixel.y += random.nextGaussian() * stdevPixel;

            // make sure it's inside image and could be observed
            if (pixel.x < 0 || pixel.x >= imageWidth - 1)
                return false;
            if (pixel.y < 0 || pixel.y >= imageHeight - 1)
                return false;
        }
        return true;
    }

    private void rotateToPixel(Point2D_F64 pixel, Se3_F64 originToPixel) {
        Point2D_F64 norm = new Point2D_F64();
        pixelToNorm.compute(pixel.x, pixel.y, norm);

        double rotY = Math.atan2(-norm.y, 1);
        double rotX = Math.atan2(-norm.x, 1);

        ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, rotX, rotY, 0, originToPixel.R);
    }

    private void randomTilt(double maxRadian, Se3_F64 transform) {
        Rodrigues_F64 rod = new Rodrigues_F64();

        rod.unitAxisRotation.x = random.nextDouble() - 0.5;
        rod.unitAxisRotation.y = random.nextDouble() - 0.5;
        rod.unitAxisRotation.z = random.nextDouble() - 0.5;
        rod.unitAxisRotation.normalize();

        rod.theta = 2.0 * (random.nextDouble() - 0.5) * maxRadian;
        ConvertRotation3D_F64.rodriguesToMatrix(rod, transform.R);
    }


    /**
     * Saves simulation results to a file
     */
    public void saveSimulation(List<Point3D_F64> corners,
                               List<Se3_F64> BodyToCameras, List<List<Point2D_F64>> observations,
                               long randomSeed,
                               PrintStream out) {
        if (BodyToCameras.size() != observations.size())
            throw new RuntimeException("views doesn't match observation count");
        int N = BodyToCameras.size();

        Rodrigues_F64 rod = new Rodrigues_F64();

        out.println("# Simulated camera observations of 3D points on a rigid body with truth");
        out.println("# Random seed = " + randomSeed);
        out.println("# Pixel Noise = "+stdevPixel);
        out.println("# CAMERA width, height, fx, fy, cx, cy");
        out.printf("CAMERA %d %d %.10f %.10f %.10f %.10f\n",
                imageWidth, imageHeight, intrinsic.fx, intrinsic.fy, intrinsic.cx, intrinsic.cy);
        out.println("# Marker corner locations in marker frame");
        out.println("# CORNERS N x[0] y[0] z[0] ... ");
        out.print("CORNERS "+corners.size());
        for (int i = 0; i < corners.size(); i++) {
            Point3D_F64 c = corners.get(i);
            out.printf(" %.10f %.10f %.10f", c.x, c.y, c.z);
        }
        out.println();

        out.println("# Rodrigues coordinates with a unit rotation vector describes the rotation");
        out.println("# Pose represents the transform from the mark to the camera's reference frame");
        out.println("# POSE X Y Z rodX rodY rodZ rodTheta");
        out.println("# OBSERVATIONS N pixelX[0] pixelY[0] ...");

        for (int i = 0; i < N; i++) {
            Se3_F64 BodyToCamera = BodyToCameras.get(i);
            List<Point2D_F64> observation = observations.get(i);

            ConvertRotation3D_F64.matrixToRodrigues(BodyToCamera.R, rod);

            out.printf("POSE %.10f %.10f %.10f %.10f %.10f %.10f %.10f\n",
                    BodyToCamera.T.x, BodyToCamera.T.y, BodyToCamera.T.z,
                    rod.unitAxisRotation.x, rod.unitAxisRotation.y, rod.unitAxisRotation.z, rod.theta);

            // Save the observations in a line
            out.print("OBSERVATIONS "+observation.size());
            for (int indexPixel = 0; indexPixel < observation.size(); indexPixel++) {
                Point2D_F64 a = observation.get(indexPixel);
                out.printf(" %.3f %.3f", a.x, a.y);
            }
            out.println();
        }
        out.close();
    }

    public double getHfov() {
        return hfov;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public double getStdevPixel() {
        return stdevPixel;
    }

    public static void main(String[] args) {
        GeneratePnPObservation generator = new GeneratePnPObservation(60,1024,768);

        generator.initialize(0.5,"pnp");
        generator.targetSquare(0.2);
        generator.generateUniformImageDiscreteDistances(new DiscreteRange(1,6,20),1,2000);
    }
}
