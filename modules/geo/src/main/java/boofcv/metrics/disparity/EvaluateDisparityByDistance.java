package boofcv.metrics.disparity;

import boofcv.abst.disparity.StereoDisparity;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.GPixelMath;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.impl.ImplImageStatistics;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_F64;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.Random;

/**
 * <p>Renders a planar scene that's parallel to the camera. Then moves the camera away and records the distance and
 * compares against ground truth. This benchmark is designed to flush out subtle performance differences in
 * how disparity is computed. It looks at overall accuracy and measures if it's suffering from pixel locking.
 * Pixel locking is when the disparity is biased in a certain direction.</p>
 *
 * <p>The catastrophicReset parameter was inspired by this benchmark. Set it to a very large value and see what
 * happens to SAD and NCC.</p>
 *
 * @author Peter Abeles
 */
public class EvaluateDisparityByDistance<T extends ImageGray<T>> {
	// Smaller baseline means that sub-pixel becomes more important.
	public double BASELINE = 0.08;

	public double SQUARE_WIDTH = 6.0;
	public double MIN_DISTANCE = 3.0;
	public double MAX_DISTANCE = 12.0;
	public int trials = 200;

	CameraPinhole intrinsic = PerspectiveOps.createIntrinsic(400, 400, 75, -1, null);
	StereoParameters stereoParameters = new StereoParameters();

	public PrintStream out = System.out;

	/** How often the current error sign matched the previous. If random this should be 0.5 */
	public double pixelLockFraction;

	/** Range error as a fraction of the true range */
	public DogArray_F64 errorFraction = new DogArray_F64();

	/** Processing time in milliseconds */
	public DogArray_F64 processingTimeMS = new DogArray_F64();

	Class<T> imageType;

	// If true it will show the rendered images
	boolean gui = false;

	public EvaluateDisparityByDistance( Class<T> imageType ) {
		this.imageType = imageType;
		stereoParameters.left.setTo(intrinsic);
		stereoParameters.right.setTo(intrinsic);
		stereoParameters.right_to_left.T.x = BASELINE;
	}

	/**
	 * Creates a randomly textured image
	 */
	private GrayF32 createSurfaceTexture( long seed, @Nullable GrayF32 image ) {
		// Create the RNG here so that all images are evaluated on the same data
		var rand = new Random(seed);

		// Storage for the texture image
		if (image == null)
			image = new GrayF32(500, 500);

		int minLength = 10;

		for (int i = 0; i < 10_000; i++) {
			// Randomly generate a rectangle to fill in
			int x0 = rand.nextInt(image.width - minLength/2);
			int y0 = rand.nextInt(image.height - minLength/2);
			int width = Math.min(rand.nextInt(image.width/20) + minLength, image.width - x0);
			int height = Math.min(rand.nextInt(image.height/20) + minLength, image.height - y0);

			// Select the color randomly
			float value = rand.nextFloat()*255;

			// Draw the rectangle
			ImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
		}

		// Add noise so that even flat regions have some texture
		ImageMiscOps.addGaussian(image, rand, 20, 0.0f, 255.0f);

//		SwingUtilities.invokeLater(() -> ShowImages.showWindow(image, "Texture"));

		return image;
	}

	public void evaluate( StereoDisparity<T, GrayF32> disparityAlg ) {
		processingTimeMS.reset();
		errorFraction.reset();
		final float disparityMin = disparityAlg.getDisparityMin();
		final float disparityRange = disparityAlg.getDisparityRange();

		var markerToWorld = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, 0, markerToWorld.R);

		var sim = new SimulatePlanarWorld();
		sim.enableHighAccuracy();
		sim.setCamera(intrinsic);

		var worldToCamera = new Se3_F64();

		T frameLeft = GeneralizedImageOps.createImage(imageType, 1, 1, 1);
		T frameRight = GeneralizedImageOps.createImage(imageType, 1, 1, 1);

		var imagePanel = new ListDisplayPanel();
		if (gui) {
			SwingUtilities.invokeLater(() -> {
				imagePanel.setPreferredSize(new Dimension(intrinsic.width + 200, intrinsic.height + 50));
				ShowImages.showWindow(imagePanel, "Renders");
			});
		}

		var ranges = new DogArray_F64();
		var disparities = new DogArray_F64();

		int previousSign = 0;
		int pixelLockCount = 0;

		GrayF32 texture = null;
		var rand = new Random(0xDEADBEEFL);

		double range = MAX_DISTANCE - MIN_DISTANCE;
		for (int testIndex = 0; testIndex < trials; testIndex++) {
			// Randomly generate a new image every trial. This is to avoid the image introducing a bias into
			// the results across all tests.
			texture = createSurfaceTexture(rand.nextLong(), texture);
			sim.resetScene();
			sim.addSurface(markerToWorld, SQUARE_WIDTH, texture);

			worldToCamera.T.z = MIN_DISTANCE + testIndex*range/(trials - 1);

			worldToCamera.T.x = BASELINE/2;
			sim.setWorldToCamera(worldToCamera);
			GConvertImage.convert(sim.render(), frameLeft);

			worldToCamera.T.x = -BASELINE/2;
			sim.setWorldToCamera(worldToCamera);
			GConvertImage.convert(sim.render(), frameRight);

			long time0 = System.nanoTime();
			disparityAlg.process(frameLeft, frameRight);
			long time1 = System.nanoTime();
			processingTimeMS.add((time1 - time0)*1e-6);

			ranges.reset();
			GrayF32 disparityImage = disparityAlg.getDisparity();
			for (int y = 0; y < disparityImage.height; y++) {
				for (int x = 0; x < disparityImage.width; x++) {
					float v = disparityImage.unsafe_get(x, y);
					if (v == 0.0f || v >= disparityRange)
						continue;

					disparities.add(v + disparityMin);
					ranges.add(BASELINE*intrinsic.fx/(v + disparityMin));
				}
			}

			if (gui) {
				BufferedImage buffered = ConvertBufferedImage.convertTo(frameLeft, null, false);
				SwingUtilities.invokeLater(() -> imagePanel.addImage(String.format("Z=%.3f", worldToCamera.T.z), buffered));
			}
			ranges.sort();
			disparities.sort();

			double disparityMeasured = disparities.getFraction(0.5);
			double rangeMeasured = ranges.getFraction(0.5);
			double rangeError = rangeMeasured - worldToCamera.T.z;
			errorFraction.add(Math.abs(rangeError)/worldToCamera.T.z);

			int errorSign = (int)Math.signum(rangeError);
			if (errorSign == previousSign)
				pixelLockCount++;
			previousSign = errorSign;

			System.out.printf("%3d  %.2f  E %6.3f  D %.3f\n", testIndex, worldToCamera.T.z, rangeError, disparityMeasured);
		}
		pixelLockFraction = pixelLockCount/(double)(trials - 1);
		errorFraction.sort();
	}
}
