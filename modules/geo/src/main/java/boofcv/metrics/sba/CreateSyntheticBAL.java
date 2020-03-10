package boofcv.metrics.sba;

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSnavely;
import boofcv.io.geo.CodecBundleAdjustmentInTheLarge;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Creates a synthetic scenario and saves it in the Bundle Adjustment in the Large format.
 *
 * @author Peter Abeles
 */
public class CreateSyntheticBAL {

    Random rand = new Random(234);

    double pixelNoise = 0.2;

    public SceneStructureMetric scene;
    public SceneObservations observations;

    public void smallWorld() {
        scene = new SceneStructureMetric(false);
        scene.initialize(1,3,10);
        observations = new SceneObservations();
        observations.initialize(scene.views.size);

        double radius = 300;
        BundlePinholeSnavely camera = new BundlePinholeSnavely();
        camera.f = 500;
        camera.k1 = 0.01;
        camera.k2 = 0.001;
        scene.setCamera(0,false,camera);

        Rodrigues_F64 rod = new Rodrigues_F64();
        for (int i = 0; i < scene.views.size; i++) {
            Se3_F64 worldToView = new Se3_F64();
            worldToView.T.x = -0.5 + i;

            // Small rotation to avoid edge conditions in optimizer. Easier to compare methods
            rod.unitAxisRotation.x = rand.nextGaussian();
            rod.unitAxisRotation.y = rand.nextGaussian();
            rod.unitAxisRotation.z = rand.nextGaussian();
            rod.unitAxisRotation.normalize();
            rod.theta = rand.nextGaussian()*0.001;
            ConvertRotation3D_F64.rodriguesToMatrix(rod,worldToView.R);

            scene.setView(i,false,worldToView);
            scene.connectViewToCamera(i,0);
        }


        Point2D_F64 pixel = new Point2D_F64();
        Point3D_F64 viewPt = new Point3D_F64();
        for (int pixelIdx = 0; pixelIdx < scene.points.size; pixelIdx++) {
            Point3D_F64 p = new Point3D_F64();
            p.x = -1 + 2.0*pixelIdx/(scene.points.size-1);
            p.y = rand.nextGaussian();
            p.z = -3 + rand.nextGaussian()*0.1;

            scene.setPoint(pixelIdx,p.x,p.y,p.z);

            for (int viewIdx = 0; viewIdx < scene.views.size; viewIdx++) {
                Se3_F64 worldToView = scene.views.data[viewIdx].worldToView;
                worldToView.transform(p,viewPt);

                if( viewPt.z >= 0 ) {
                    System.out.println("Egads");
                    continue;
                }

                camera.project(viewPt.x,viewPt.y,viewPt.z,pixel);

                // Add noise
                pixel.x += rand.nextGaussian()*pixelNoise;
                pixel.y += rand.nextGaussian()*pixelNoise;

                // make sure it's in the FOV
                if( pixel.x < -radius || pixel.x > radius || pixel.y < -radius || pixel.y > radius ) {
                    continue;
                }

                observations.getView(viewIdx).add(pixelIdx,(float)pixel.x,(float)pixel.y);
            }
        }
    }

    public void save( String path ) throws IOException {
        CodecBundleAdjustmentInTheLarge codec = new CodecBundleAdjustmentInTheLarge();
        codec.scene = scene;
        codec.observations = observations;
        codec.save(new File(path));
    }

    public static void main(String[] args) throws IOException {
        CreateSyntheticBAL app = new CreateSyntheticBAL();
        app.smallWorld();
        app.save("small_workd_bundle.txt");
    }
}
