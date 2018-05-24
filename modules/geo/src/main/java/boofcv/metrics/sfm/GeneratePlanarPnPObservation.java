package boofcv.metrics.sfm;

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
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
import georegression.transform.se.SePointOps_F64;

import java.io.File;
import java.io.FileNotFoundException;
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
public class GeneratePlanarPnPObservation {
    // TODO 4-points - fixed distance. random roll - fixed yaw
    // TODO 500-points - fixed distance. random roll - fixed yaw

    Random random = new Random(234);

    // Camera parameters
    double hfov = 90; // degrees
    int imageWidth = 640;
    int imageHeight = 480;
    CameraPinhole intrinsic = new CameraPinhole();
    Point2Transform2_F64 normToPixel;
    Point2Transform2_F64 pixelToNorm;


    // The smallest the target should appear in the camera in pixel
    double minMarkerPixels = 20;

    // Length of a size on the target
    double sizeLength = 1;

    // +- rotation applies to target in target frame
    double rangeRotX = 0;
    double rangeRotY = 0;
    double rangeRotZ = 0;

    // Amount of pixel noise added. Normal distribution
    double stdevPixel = 0;

    public GeneratePlanarPnPObservation() {
        double focalLength = (imageWidth /2)/Math.tan(UtilAngle.radian(hfov)/2);
        intrinsic.fsetK(focalLength,focalLength,0, imageWidth /2, imageHeight /2, imageWidth, imageHeight);

        normToPixel = new LensDistortionPinhole(intrinsic).distort_F64(false,true);
        pixelToNorm = new LensDistortionPinhole(intrinsic).undistort_F64(true,false);
    }


    /**
     * Computes how far away the target will be when it appears this wide to the camera if it's facing directly
     * at the camera in the image center.
     */
    public double distanceToApparentSize(double targetPixels ) {
        double r = sizeLength/2.0;
        double inner = targetPixels*hfov*Math.PI/(360.0* imageWidth);
        return r/Math.tan(inner);
    }

    public void generateSquareDistanceNoiseGrid( File directory ,
                                                 int numDistances , int numNoise,  double maxPixelStdev )
            throws FileNotFoundException
    {
        // Compute how far away a head on marker would be to appear to have a length of minimal size
        double maxDistance = distanceToApparentSize(20)-1;

        List<Point3D_F64> corners = specifyCorners4(sizeLength);

        // jiggle the orientation around a bit but don't really stress it with orientation
        this.rangeRotX = 0.2;
        this.rangeRotY = 0.2;
        this.rangeRotZ = 0.2;

        for (int indexDistance = 0; indexDistance < numDistances; indexDistance++) {
            double distanceZ = sizeLength + maxDistance*indexDistance/(numDistances-1.0);

            for (int indexNoise = 0; indexNoise < numNoise; indexNoise++) {

                File f = new File(directory,String.format("squarep4p_%02d_%02d.txt",indexDistance,indexNoise));
                this.stdevPixel = maxPixelStdev*indexNoise/(numNoise-1.0);
                createObservation(corners,distanceZ,1000, random.nextLong(),f);
            }
        }
    }

    public void generateSquareDistanceOrientationGrid( File directory ,
                                                       int numDistances , int numAngles )
    {

    }


    /**
     * Given the distance the target is away
     * @param distanceZ Distance away the target is from the camera
     * @param numObs Number of observations to generate
     */
    public void createObservation( List<Point3D_F64>  corners,
                                   double distanceZ , int numObs  , long randomSeed, File file  )
            throws FileNotFoundException
    {
        Random random = new Random(randomSeed);

        PrintStream out = new PrintStream(file);

        // Coordinate frame transform. body is the marker's body frame
        Se3_F64 BodyToUp = new Se3_F64();
        Se3_F64 UpToPose1 = new Se3_F64();
        Se3_F64 Pose1ToCamera = new Se3_F64();
        Se3_F64 BodyToCamera = new Se3_F64();

        // Flip the 2D target so that it's facing the camera
        ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, Math.PI/2.0,0,0, BodyToUp.R);

        Pose1ToCamera.T.z = distanceZ;

        // Specifies transform from body to camera reference frames
        InvertibleTransformSequence<Se3_F64> sequence = new InvertibleTransformSequence<>();
        sequence.addTransform(true,BodyToUp);
        sequence.addTransform(true,UpToPose1);
        sequence.addTransform(true,Pose1ToCamera);

        Rodrigues_F64 rod = new Rodrigues_F64();

        out.println("# Simulated observations of a rigid object");
        out.println("# Random seed = "+randomSeed);
        out.println("# Marker corner locations in marker frame");
        out.println("# CORNERS N x0 y0 z0 ... x"+(numObs-1)+" y"+(numObs-1)+" z"+(numObs-1));
        out.print("CORNERS");
        for (int i = 0; i < corners.size(); i++) {
            Point3D_F64 c = corners.get(i);
            out.printf(" %.10f %.10f %.10f",c.x,c.y,c.z);
        }
        out.println();

        out.println("# Rodrigues coordinates with a unit rotation vector describes the rotation");
        out.println("# Pose represents the transform from the mark to the camera's reference frame");
        out.println("# POSE X Y Z rodX rodY rodZ rodTheta");
        out.println("# OBSERVATIONS pixelX0 pixelY0 ... pixelX"+(numObs-1)+" pixelY"+(numObs-1));

        for (int i = 0; i < numObs; i++) {

            // Randomly perturb the target's orientation in it's body frame
            double rotX = random.nextDouble()*rangeRotX - rangeRotX/2;
            double rotY = random.nextDouble()*rangeRotY - rangeRotY/2;
            double rotZ = random.nextDouble()*rangeRotZ - rangeRotZ/2;

            ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, rotX,rotY,rotZ, UpToPose1.R);

            // Pixel a random pixel which will act as the marker's center
            // Make sure the entire marker is inside the camera's field of view
            List<Point2D_F64> observation = new ArrayList<>();
            Point2D_F64 norm = new Point2D_F64();
            Point2D_F64 pixel = new Point2D_F64();
            Point3D_F64 p3 = new Point3D_F64();

            for (int attempt = 0; attempt < 10000; attempt++) {
                double pixelX = random.nextDouble()*(imageWidth -1);
                double pixelY = random.nextDouble()*(imageHeight -1);

                pixelToNorm.compute(pixelX,pixelY,norm);

                Pose1ToCamera.T.x = norm.x*distanceZ;
                Pose1ToCamera.T.y = norm.y*distanceZ;
                Pose1ToCamera.T.z = distanceZ;

                sequence.computeTransform(BodyToCamera);
                for( int indexCorner = 0; indexCorner < corners.size(); indexCorner++ ) {
                    SePointOps_F64.transform(BodyToCamera, corners.get(indexCorner), p3);

                    normToPixel.compute(p3.x/p3.z,p3.y/p3.z, pixel);

                    pixel.x += random.nextGaussian()*stdevPixel;
                    pixel.y += random.nextGaussian()*stdevPixel;

                    // make sure it's inside image and could be observed
                    if( pixel.x < 0 || pixel.x >= imageWidth -1 )
                        break;
                    if( pixel.y < 0 || pixel.y >= imageHeight -1 )
                        break;

                    observation.add(pixel);
                }

                if( observation.size() == corners.size() )
                    break;
            }

            if( observation.size() != corners.size() )
                throw new RuntimeException("Failed to find a valid marker location");

            // Save the transform in its own line
            ConvertRotation3D_F64.matrixToRodrigues(BodyToCamera.R, rod);

            out.printf("POSE %.10f %.10f %.10f %.10f %.10f %.10f %.10f\n",
                    BodyToCamera.T.x, BodyToCamera.T.y, BodyToCamera.T.z,
                    rod.unitAxisRotation.x, rod.unitAxisRotation.y, rod.unitAxisRotation.z , rod.theta);

            // Save the observations in a line
            out.print("OBSERVATIONS");
            for( int indexPixel = 0; indexPixel < observation.size(); indexPixel++ ) {
                Point2D_F64 a = observation.get(indexPixel);
                out.printf(" %.3f %.3f",a.x,a.y);
            }
            out.println();
        }
        out.close();
    }

    public List<Point3D_F64> specifyCorners4( double width ) {
        double r = width/2;
        List<Point3D_F64> corners = new ArrayList<>();
        corners.add( new Point3D_F64(-r,-r,0));
        corners.add( new Point3D_F64(r,-r,0));
        corners.add( new Point3D_F64(r,r,0));
        corners.add( new Point3D_F64(-r,r,0));
        return corners;
    }
}
