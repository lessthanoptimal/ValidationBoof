package validate.tracking;

import boofcv.abst.geo.RefineEpipolar;
import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.geo.robust.DistanceHomographySq;
import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ejml.data.DMatrixRMaj;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static validate.tracking.BatchEvaluateSummaryAndTime.pathToData;

/**
 * Post processes image sequences to create groups truth.  Image transforms are assumed to be well described
 * by a homography.  For each frame in the sequence lens distortion is first removed then the best fit
 * homography is found to the first frame in the sequence.
 *
 * @author Peter Abeles
 */
public class CreateGroundTruth {

	// where the output should be saved to
	String outputDirectory;

	// removes lens distortion
	ImageDistort<GrayF32,GrayF32> removeLens;

	EvaluationTracker<GrayF32> tracker;

	// transform from the keyframe to the global frame
	Homography2D_F64 globalToKey;
	// minimum number of inliers before it changes keyframe
	int minimumInliers;

	// generates initial model from matched features
	// How the error is measured
	// TODO put more thought into which models and errors to use
	DistanceHomographySq distance = new DistanceHomographySq();
	GenerateHomographyLinear generatorH = new GenerateHomographyLinear(true);
	ModelManager<Homography2D_F64> mm = new ModelManagerHomography2D_F64();

	// Use RANSAC to estimate the Homography matrix
	ModelMatcher<Homography2D_F64,AssociatedPair> robustH =
			new Ransac<Homography2D_F64, AssociatedPair>(123123,mm,generatorH,distance,7500,1);

	RefineEpipolar refineH = FactoryMultiView.refineHomography(1e-8, 1000, EpipolarError.SAMPSON);

	// Refines the initial estimate of the Homography matrix
	RefineHomographTransform<GrayF32,GrayF32> refinePyramidH =
			new RefineHomographTransform<GrayF32,GrayF32>(new int[]{1,2,4,8},GrayF32.class,GrayF32.class);

	// working space when saving images
	BufferedImage storage;

	// features in the key frame
	GrayF32 keyFrame;

	public CreateGroundTruth( CameraPinholeRadial cameraParam , String outputDirectory ) {

		this.outputDirectory = outputDirectory;

		// create distortion to remove lens distortion
		// Adjust the distortion so that the undistorted image only shows image pixels
		Point2Transform2_F32 allInside = LensDistortionOps.
				transformChangeModel_F32(AdjustmentType.EXPAND,cameraParam, null,true,null);
		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
		removeLens = FactoryDistort.distortSB(false,interp, GrayF32.class);
		removeLens.setModel(new PointToPixelTransform_F32(allInside));

		FactoryEvaluationTrackers<GrayF32> factory = new FactoryEvaluationTrackers<GrayF32>(GrayF32.class);

		tracker = factory.createFhSurfKlt();
	}

	public void process( SimpleImageSequence<GrayF32> sequence ) throws FileNotFoundException {

		int frameNumber = 0;
		int resetFraction = 2;

		System.out.println("Processing keyframe");
		keyFrame = undistort( sequence.next() );
		tracker.track(keyFrame);
		minimumInliers = tracker.getCurrent().size()/resetFraction;
		refinePyramidH.setSource(keyFrame);
		globalToKey = new Homography2D_F64();

		storage = new BufferedImage(keyFrame.width,keyFrame.height,BufferedImage.TYPE_INT_RGB);

		PrintStream out = new PrintStream(new FileOutputStream(String.format("%s/homography.txt",outputDirectory)));
		out.println("# Homography transforms from the first frame to frame 'i'");
		out.println("# H11 H12 H13 H21 H22 H23 H31 H32 H33");
		saveHomography(out, globalToKey);

		saveImage(keyFrame, frameNumber++);
		while( sequence.hasNext() ) {
			GrayF32 current = undistort( sequence.next() );

			Homography2D_F64 H = computeHomography( current );

			saveImage(current, frameNumber++);

			H = globalToKey.concat(H,null);

			saveHomography(out, H);

			System.out.println("Processed frame " + frameNumber);

			if( robustH.getMatchSet().size() < minimumInliers ) {
				System.out.println("**** Changing KeyFrame ****");
				globalToKey.set(H);
				keyFrame.setTo(current);
				tracker.reset();
				tracker.track(keyFrame);
				minimumInliers = tracker.getCurrent().size()/resetFraction;
				refinePyramidH.setSource(keyFrame);
			}
		}
		out.close();

	}

	private void saveHomography(PrintStream out, Homography2D_F64 h) {
		out.printf("%15.10f %15.10f %15.10f %15.10f %15.10f %15.10f %15.10f %15.10f %15.10f\n",
				h.a11, h.a12, h.a13, h.a21, h.a22, h.a23, h.a31, h.a32, h.a33);
	}

	/**
	 * Saves the image to disk using a lossless image format
	 */
	private void saveImage( GrayF32 image , int frameNumber ) {
		ConvertBufferedImage.convertTo(image,storage);
		UtilImageIO.saveImage(storage, String.format("%s/frame%06d.png", outputDirectory, frameNumber));
	}

	/**
	 * Removes lens distortion from the input image.  Returns undistorted image.
	 */
	private GrayF32 undistort( GrayF32 input ) {
		GrayF32 ret = new GrayF32(input.width,input.height);
		removeLens.apply(input, ret);
		return ret;
	}

	/**
	 * Computes the homography transform between the key frame and the current frame using a two step process.
	 * First RANSAC is used to provide an initial estimate followed by non-linear refinement
	 */
	private Homography2D_F64 computeHomography( GrayF32 dst ) {
		System.out.print("  initial estimate of H");
		Homography2D_F64 H_approx = initialEstimate(dst);
		System.out.println(" refining estimate of H");

		return refineEstimate(dst,H_approx);
	}

	/**
	 * Creates an initial estimate of the Homography using sparse features detected
	 * inside the image
	 */
	private Homography2D_F64 initialEstimate( GrayF32 dst ) {
		tracker.track(dst);
		List<Point2D_F64> initial = tracker.getInitial();
		List<Point2D_F64> current = tracker.getCurrent();

		System.out.print(" tracks = "+initial.size()+" ");

		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		for( int i = 0; i < initial.size(); i++ ) {
			Point2D_F64 p0 = initial.get(i);
			Point2D_F64 p1 = current.get(i);

			pairs.add( new AssociatedPair(p0,p1));
		}

		// fit a homography to the data using RANSAC
		if( !robustH.process(pairs) )
			throw new RuntimeException("RANSAC failed");

		System.out.print(" inliers = "+robustH.getMatchSet().size()+" -- ");

		DMatrixRMaj H_mat = new DMatrixRMaj(3,3);
		UtilHomography_F64.convert(robustH.getModelParameters(),H_mat);
		DMatrixRMaj H_refined = new DMatrixRMaj(3,3);

		if( !refineH.fitModel(robustH.getMatchSet(),H_mat,H_refined) )
			throw new RuntimeException("Refine Point Failed");

		return UtilHomography_F64.convert(H_refined,(Homography2D_F64)null);
	}

	private Homography2D_F64 refineEstimate( GrayF32 dst ,
											 Homography2D_F64 initial )
	{
		// minimize errors using non-linear optimization
		refinePyramidH.process(dst, initial);

		return refinePyramidH.getRefinement();
	}


	public static void main( String args[] ) throws FileNotFoundException {

//		String whichData = "bricks/skew";
//		String whichData = "bricks/rotate2";
//		String whichData = "bricks/move_out";
//		String whichData = "bricks/move_in";
		String whichData = "urban_pano";


		SimpleImageSequence sequence =
				DefaultMediaManager.INSTANCE.openVideo(pathToData+whichData+".mjpeg", ImageType.single(GrayF32.class));

		CameraPinholeRadial cameraParam = CalibrationIO.load("data/intrinsic.yaml");

		String outputDir = "data/temp";

		CreateGroundTruth alg = new CreateGroundTruth(cameraParam,outputDir);

		alg.process(sequence);

		sequence.close();
	}
}
