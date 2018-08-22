package boofcv.metrics.sfm;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestGeneratePnPObservation {
    /**
     * Test to see if the marker is squares
     */
    @Test
    public void targetSquare() {
        GeneratePnPObservation generator = new GeneratePnPObservation(60,640,480);

        double l = .4;
        generator.targetSquare(l);

        assertEquals(4,generator.marker.size());

        for (int i = 0,j=3; i < 4; j=i,i++) {
            // Does it lie flat on the z=0 plane
            assertEquals(0, generator.marker.get(i).z , UtilEjml.TEST_F64);
            // sides are the expected length
            assertEquals(l,generator.marker.get(i).distance(generator.marker.get(j)), UtilEjml.TEST_F64);
        }
    }

    @Test
    public void simulate() {
        int width = 640;
        int height = 480;
        GeneratePnPObservation generator = new GeneratePnPObservation(60,width,height);
        generator.random = new Random(1234);
        generator.stdevPixel = 0;
        generator.targetSquare(0.3);

        List<Se3_F64> BodyToCameras = new ArrayList<>();
        List<List<Point2D_F64>> observations = new ArrayList<>();

        double distance = 2;
        int N = 150;
        generator.simulate(distance,0.01,N,BodyToCameras,observations,true,false);

        assertEquals(N,BodyToCameras.size());
        assertEquals(N,observations.size());

        for (int i = 0; i < N; i++) {
            assertEquals(distance, BodyToCameras.get(i).T.norm(), 1e-4);

            for( Point2D_F64 p : observations.get(i) ) {
                assertTrue( p.x >= 0 && p.x <= width-1 );
                assertTrue( p.y >= 0 && p.y <= height-1 );
            }
        }
    }

    /**
     * Checks to see if the return value is correct with some simple tests
     */
    @Test
    public void renderMarker_return() {
        GeneratePnPObservation generator = new GeneratePnPObservation(60,640,480);
        generator.random = new Random(1234);
        generator.stdevPixel = 0;

        List<Point3D_F64> marker = new ArrayList<>();
        Se3_F64 BodyToCamera = SpecialEuclideanOps_F64.setEulerXYZ(0,0,0,0,0,5,null);


        marker.add( new Point3D_F64(0,1,0));
        marker.add( new Point3D_F64(1,1,0));
        marker.add( new Point3D_F64(1,0,0));

        List<Point2D_F64> pixels = new ArrayList<>();
        for (int i = 0; i < marker.size(); i++) {
            pixels.add( new Point2D_F64() );
        }

        // marker is in front
        assertTrue(generator.renderMarker(marker,BodyToCamera,pixels));

        // marker is behind
        SpecialEuclideanOps_F64.setEulerXYZ(Math.PI/2.0,0,0,0,0,-5,BodyToCamera);
        assertFalse(generator.renderMarker(marker,BodyToCamera,pixels));

        // outside the image
        SpecialEuclideanOps_F64.setEulerXYZ(Math.PI/2.0,0,0,1000,0,0.1,BodyToCamera);
        assertFalse(generator.renderMarker(marker,BodyToCamera,pixels));
        SpecialEuclideanOps_F64.setEulerXYZ(Math.PI/2.0,0,0,0,1000,0.1,BodyToCamera);
        assertFalse(generator.renderMarker(marker,BodyToCamera,pixels));
    }

    /**
     * If the marker is square and in the center of the image make sure the rendered marker is square too
     */
    @Test
    public void renderMarker_square() {
        GeneratePnPObservation generator = new GeneratePnPObservation(60,640,480);
        generator.random = new Random(1234);
        generator.stdevPixel = 0;
        generator.targetSquare(1);

        Se3_F64 BodyToCamera = SpecialEuclideanOps_F64.setEulerXYZ(0,0,0,0,0,4,null);

        List<Point2D_F64> pixels = new ArrayList<>();
        for (int i = 0; i < generator.marker.size(); i++) {
            pixels.add( new Point2D_F64() );
        }

        assertTrue(generator.renderMarker(generator.marker,BodyToCamera,pixels));

        double length0 = pixels.get(0).distance(pixels.get(1));
        for (int i = 1; i < 4; i++) {
            int j = (i+1)%4;
            double lengthi = pixels.get(i).distance(pixels.get(j));

            assertEquals(length0,lengthi, UtilEjml.TEST_F64);
        }
    }
}