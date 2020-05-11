package boofcv.metrics.vo;

import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;

import java.io.PrintStream;

/**
 * Processes a sequence of stereo images and computes egomotion while evaluating performance using ground truth.
 *
 * @author Peter Abeles
 */
public class EvaluateVisualOdometryStereo<T extends ImageBase<T>> {
	protected StereoVisualOdometry<T> alg;
	protected SequenceStereoImages data;

	protected T inputLeft;
	protected T inputRight;

	int numFaults = 0;
	int numSkipUpdate = 0;
	int skipFrame = 20;

	Se3_F64 previousWorldToLeftFound = new Se3_F64();
	Se3_F64 previousWorldToLeft = new Se3_F64();
	Se3_F64 initialWorldToLeft = new Se3_F64();

	Se3_F64 prevSkipEstimated = new Se3_F64();
	Se3_F64 prevSkipTruth = new Se3_F64();

	double totalErrorDistanceSkip;
	double totalErrorRotationSkip;
	double totalErrorDistance;
	double totalErrorRotation;
	double integralFoundDistance;
	double integralFoundRotation;
	double integralTrueDistance;
	double integralTrueRotation;
	double absoluteLocation;
	double absoluteRotation;

	int numEstimates;

	int frame;

	boolean verbose = true;

	PrintStream out;

	double totalTimeMilli = 0;

	public EvaluateVisualOdometryStereo( SequenceStereoImages data,
										 StereoVisualOdometry<T> alg,
										 ImageType<T> imageType )
	{
		this.data = data;
		this.alg = alg;
		inputLeft = imageType.createImage(1,1);
		inputRight = imageType.createImage(1,1);
	}

	public void setOutputStream(PrintStream out) {
		this.out = out;
	}

	public void initialize() {
		alg.reset();
		if( !data.next() )
			throw new IllegalArgumentException("No data to process");

		totalErrorDistanceSkip = 0;
		totalErrorRotationSkip = 0;
		totalErrorDistance = 0;
		totalErrorRotation = 0;
		integralFoundDistance = 0;
		integralFoundRotation = 0;
		integralTrueDistance = 0;
		integralTrueRotation = 0;

		totalTimeMilli = 0.0;

		numEstimates = 0;

		numFaults = 0;
		numSkipUpdate = 0;

		frame = 0;

		inputLeft.reshape(data.getLeft().getWidth(),data.getLeft().getHeight());
		inputRight.reshape(data.getRight().getWidth(),data.getRight().getHeight());

		previousWorldToLeftFound.reset();
		data.getLeftToWorld().invert(previousWorldToLeft);
		initialWorldToLeft.set(previousWorldToLeft);

		prevSkipEstimated.reset();
		prevSkipTruth.set(data.getLeftToWorld());

		if( data.isCalibrationFixed() )
			alg.setCalibration(data.getCalibration());
	}

	public boolean nextFrame() {
		if( !data.next() )  {
			computeFinalStatistics();
			return false;
		} else {
			processFrame();
			return true;
		}
	}

	private void processFrame() {
		ConvertBufferedImage.convertFrom(data.getLeft(), inputLeft, true);
		ConvertBufferedImage.convertFrom(data.getRight(), inputRight, true);

		long before = System.nanoTime();
		if( !data.isCalibrationFixed() )
			alg.setCalibration(data.getCalibration());
		boolean updated = alg.process(inputLeft,inputRight);
		long after = System.nanoTime();

		totalTimeMilli += (after-before)*1e-6;
		double fps = (frame+1) / (totalTimeMilli*1e-3);

		if( !updated ) {
			numSkipUpdate++;
			if( alg.isFault() ) {
				numFaults++;
			}
			if( verbose )
				System.out.printf("%d %6.2f NO UPDATE fault = %s\n",frame,fps,alg.isFault());
		} else {
			Se3_F64 found = alg.getCameraToWorld().concat(previousWorldToLeftFound,null);
			Se3_F64 expected = data.getLeftToWorld().concat(previousWorldToLeft,null);

			Se3_F64 diff = expected.concat(found.invert(null), null);

			double distanceError = diff.getT().norm();
			double distanceTruth = expected.getT().norm();

			double errorFrac = distanceError / distanceTruth;
			double errorAngle = rotationMatrixToRadian(diff.getR());

			if( verbose )
				System.out.printf("%5d fps = %6.2f location error %f error frac %f  angle %6.3f\n", frame,fps,distanceError, errorFrac,errorAngle);
//			System.out.println("  expected "+expected.getT());
//			System.out.println("  found "+found.getT());

			if( (frame%skipFrame) == 0 ) {
				Se3_F64 deltaEstimated = alg.getCameraToWorld().concat(prevSkipEstimated.invert(null),null);
				Se3_F64 deltaTruth = data.getLeftToWorld().concat(prevSkipTruth.invert(null),null);

				Se3_F64 error = deltaEstimated.invert(null).concat(deltaTruth,null);

				totalErrorDistanceSkip += error.getT().norm();
				totalErrorRotationSkip += rotationMatrixToRadian(error.getR());

				prevSkipEstimated.set(alg.getCameraToWorld());
				prevSkipTruth.set(data.getLeftToWorld());
			}

			alg.getCameraToWorld().invert(previousWorldToLeftFound);
			data.getLeftToWorld().invert(previousWorldToLeft);

			numEstimates++;
			totalErrorDistance += distanceError;
			totalErrorRotation += errorAngle;

			integralFoundDistance += found.getT().norm();
			integralFoundRotation += rotationMatrixToRadian(found.getR());
			integralTrueDistance += distanceTruth;
			integralTrueRotation += rotationMatrixToRadian(expected.getR());

			// find difference in absolute location
			Se3_F64 leftToWorld = data.getLeftToWorld().concat(initialWorldToLeft,null);

			diff = leftToWorld.concat(alg.getCameraToWorld().invert(null),null);
			absoluteLocation = diff.getT().norm();
			absoluteRotation = rotationMatrixToRadian(diff.getR());
		}

		frame++;
	}

	protected void computeFinalStatistics() {
		// todo add absolute location and absolute rotation?
		double integralDistance = Math.abs(integralFoundDistance - integralTrueDistance)/ integralTrueDistance;
		double integralRotation = Math.abs(integralFoundRotation - integralTrueRotation)/ integralTrueDistance;
		double averagePerEstDistance = totalErrorDistance/numEstimates;
		double averagePerEstRotation = totalErrorRotation/numEstimates;
		double averagePerDistDistance = totalErrorDistance/integralTrueDistance;
		double averagePerDistRotation = totalErrorRotation/integralTrueDistance;
		double averageSkipDistance = totalErrorDistanceSkip/integralTrueDistance;
		double averageSkipRotation = totalErrorRotationSkip/integralTrueDistance;

		if( out != null ) {
			out.println("Total Frames = "+frame);
			out.println("Total Faults = "+numFaults);
			out.println("Fraction with no update "+(numSkipUpdate/(double)frame));
			out.printf("Ave per estimate:      distance %9.7f  rotation %11.9f\n",
					averagePerEstDistance, averagePerEstRotation);
			out.printf("Ave per distance:      distance %9.5f%% deg/unit = %5.2e\n",
					100*averagePerDistDistance, UtilAngle.radianToDegree(averagePerDistRotation));
			out.printf("Ave Skip %2d:           distance %9.5f%% deg/unit = %5.2e\n",
					skipFrame,100*averageSkipDistance,UtilAngle.radianToDegree(averageSkipRotation));
			out.printf("Absolute:              location %9.5f  rotation %6.2f degrees\n",
					absoluteLocation, UtilAngle.radianToDegree(absoluteRotation));
			out.printf("Integral per distance: distance %9.5f%% deg/unit = %5.2e\n",
					100*integralDistance,UtilAngle.radianToDegree(integralRotation));
		}

	}

	private double rotationMatrixToRadian(DMatrixRMaj a) {
		double angles[] = ConvertRotation3D_F64.matrixToEuler(a,EulerType.XYZ,null);

		double sum = angles[0]*angles[0] + angles[1]*angles[1] + angles[1]*angles[1];

		return Math.sqrt(sum);
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public double getAverageFPS() {
		return frame/(totalTimeMilli*1e-3);
	}
}
