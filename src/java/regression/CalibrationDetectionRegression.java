package regression;

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigCircleRegularGrid;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDataType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.GrowQueue_F64;
import validate.misc.PointFileCodec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CalibrationDetectionRegression extends BaseTextFileRegression{

	List<String> chessDirectories = new ArrayList<>();
	List<String> squareDirectories = new ArrayList<>();
	List<String> circleAsymDirectories = new ArrayList<>();
	List<String> circleRegirectories = new ArrayList<>();

	List<DetectorInfo> chessDetectors = new ArrayList<DetectorInfo>();
	List<DetectorInfo> squareDetectors = new ArrayList<DetectorInfo>();
	List<DetectorInfo> circleAsymDetctors = new ArrayList<DetectorInfo>();
	List<DetectorInfo> circleRegDetectors = new ArrayList<DetectorInfo>();

	public CalibrationDetectionRegression() {

		chessDirectories.add("data/calib/stereo/Bumblebee2_Chess");
		chessDirectories.add("data/calib/mono/chessboard/Sony_DSC-HX5V");
		chessDirectories.add("data/calib/mono/chessboard/large");
		chessDirectories.add("data/calib/mono/chessboard/distant");
		chessDirectories.add("data/calib/mono/chessboard/hard");
		chessDirectories.add("data/calib/mono/chessboard/border");

		squareDirectories.add("data/calib/stereo/Bumblebee2_Square");
		squareDirectories.add("data/calib/mono/square_grid/Sony_DSC-HX5V");
		squareDirectories.add("data/calib/mono/square_grid/large");
		squareDirectories.add("data/calib/mono/square_grid/distant");

		circleAsymDirectories.add("data/calib/mono/circle_asymmetric/Sony_DSC-HX5V");
		circleAsymDirectories.add("data/calib/mono/circle_asymmetric/large");
		circleAsymDirectories.add("data/calib/mono/circle_asymmetric/distant");

		circleRegirectories.add("data/calib/mono/circle_regular/distant");
		circleRegirectories.add("data/calib/mono/circle_regular/large");
		circleRegirectories.add("data/calib/mono/circle_regular/fisheye");

//		addDetector("DetectCalibChess",
//				FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5,30)),
//				CalibrationPatterns.CHESSBOARD);
//		addDetector("DetectCalibSquare",
//				FactoryFiducialCalibration.squareGrid(new ConfigSquareGrid(4, 3,30,30)),
//				CalibrationPatterns.SQUARE_GRID);
//		addDetector("DetectCalibCircleAsymmetric",
//				FactoryFiducialCalibration.circleAsymmGrid(new ConfigCircleAsymmetricGrid(8, 5,1,6)),
//				CalibrationPatterns.CIRCLE_ASYMMETRIC_GRID);

		addDetector("DetectCalibCircleRegular",
				FactoryFiducialCalibration.circleRegularGrid(new ConfigCircleRegularGrid(4, 3,4,6)),
				CalibrationPatterns.CIRCLE_GRID);
	}

	public void addDetector( String name , DetectorFiducialCalibration detector , CalibrationPatterns type) {
		switch( type ) {
			case CHESSBOARD:chessDetectors.add(new DetectorInfo(name,detector));break;
			case SQUARE_GRID:squareDetectors.add(new DetectorInfo(name,detector));break;
			case CIRCLE_ASYMMETRIC_GRID:circleAsymDetctors.add(new DetectorInfo(name,detector));break;
			case CIRCLE_GRID:circleRegDetectors.add(new DetectorInfo(name,detector));break;
		}
	}

	@Override
	public void process(ImageDataType type) throws IOException {

		if( type != ImageDataType.F32 ) {
			throw new IOException("Only supports floating point images");
		}

		for( DetectorInfo d : chessDetectors ) {
			evaluate(d,chessDirectories);
		}

		for( DetectorInfo d : squareDetectors) {
			evaluate(d, squareDirectories);
		}

		for( DetectorInfo d : circleAsymDetctors) {
			evaluate(d, circleAsymDirectories);
		}

		for( DetectorInfo d : circleRegDetectors) {
			evaluate(d, circleRegirectories);
		}

	}

	private void evaluate( DetectorInfo d , List<String> directories ) throws FileNotFoundException {
		PrintStream output = new PrintStream(new File(directory,d.name+".txt"));
		output.println("# (file name) (truth error 50%) (truth error 95%)");

		OverallMetrics overallMetrics = new OverallMetrics();
		for( String dir : directories) {
			List<File> files = Arrays.asList(new File(dir).listFiles());

			Collections.sort(files);
			evaluate(d.detector,d.name,overallMetrics,output, files);
		}
		output.println();

		Arrays.sort(overallMetrics.errors.data,0,overallMetrics.errors.size);
		double error50 = overallMetrics.errors.data[(int)(overallMetrics.errors.size*0.5)];
		double error95 = overallMetrics.errors.data[(int)(overallMetrics.errors.size*0.95)];
		double percentSuccess = (overallMetrics.total-overallMetrics.failed)/(double)overallMetrics.total;
		output.println("Summary: 50% = "+error50+"  95% = "+error95+"   success %"+(100.0*percentSuccess));

		output.close();
	}

	private void evaluate(DetectorFiducialCalibration detector, String detectorName,
						  OverallMetrics metrics, PrintStream output, List<File> files) {
		for( File f : files ) {
			if( !f.getName().endsWith("jpg") )
				continue;

			String dataSetName = f.getPath();
			String path = f.getAbsolutePath();
			String pathTruth = path.substring(0,path.length()-3) + "txt";

			GrayF32 image = UtilImageIO.loadImage(f.getAbsolutePath(), GrayF32.class);

			List<Point2D_F64> groundTruth;
			try {
				groundTruth = PointFileCodec.load(pathTruth);
			} catch( Exception e ) {
				errorLog.println(detectorName+" "+dataSetName+" no ground truth. "+e.getMessage());
				continue;
			}

			metrics.total++;

			try {
				if( detector.process(image) ) {
					double errors[] = new double[ groundTruth.size() ];

					CalibrationObservation found = detector.getDetectedPoints();
					if( found.size() != groundTruth.size() ) {
						errorLog.println(dataSetName+" different sizes. "+found.size()+" "+groundTruth.size());
					} else {
						for (int i = 0; i < found.size(); i++) {
							errors[i] = distanceFromClosest(found.points.get(i),groundTruth);
							metrics.errors.add(errors[i]);
						}

						Arrays.sort(errors);

						double e50 = errors[errors.length/2];
						double e95 = errors[(int)((errors.length-1)*0.95)];

						output.println(dataSetName+" "+e50+" "+e95);
					}
				} else {
					output.println(dataSetName+" failed");
					metrics.failed++;
				}
			} catch( Exception e ) {
				errorLog.println(detectorName+" "+dataSetName+" detector threw exception. "+e.getMessage());
				metrics.failed++;
			}
		}
	}

	private double distanceFromClosest( Point2D_F64 target , List<Point2D_F64> points ) {

		double best = Double.MAX_VALUE;

		for( Point2D_F64 p : points ) {
			double d = target.distance(p);
			if( d < best ) {
				best = d;
			}
		}

		return best;
	}

	private class OverallMetrics {
		GrowQueue_F64 errors = new GrowQueue_F64();
		int total;
		int failed;
	}

	private class DetectorInfo
	{
		String name;
		DetectorFiducialCalibration detector;

		public DetectorInfo(String name, DetectorFiducialCalibration detector) {
			this.name = name;
			this.detector = detector;
		}
	}

	public static void main(String[] args) throws IOException {
		CalibrationDetectionRegression app = new CalibrationDetectionRegression();


//		app.addDetector("Default_Chess",FactoryCalibrationTarget.detectorChessboard(new ConfigChessboard(5,7)),true);
//		app.addDetector("Default_Square",FactoryCalibrationTarget.detectorSquareGrid(new ConfigSquareGrid(5, 7)),false);

		app.setOutputDirectory("./");
		app.process(ImageDataType.F32);
	}
}
