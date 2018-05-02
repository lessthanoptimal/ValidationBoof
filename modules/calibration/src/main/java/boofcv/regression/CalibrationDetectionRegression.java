package boofcv.regression;

import boofcv.abst.fiducial.calib.*;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.common.misc.PointFileCodec;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDataType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.GrowQueue_F64;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static boofcv.parsing.ParseCalibrationConfigFiles.parseCircleHexagonalConfig;

/**
 * @author Peter Abeles
 */
public class CalibrationDetectionRegression extends BaseRegression implements ImageRegression  {

	List<String> chessDirectories = new ArrayList<>();
	List<String> squareDirectories = new ArrayList<>();
	List<String> circleHexDirectories = new ArrayList<>();
	List<String> circleRegirectories = new ArrayList<>();

	List<DetectorInfo> chessDetectors = new ArrayList<DetectorInfo>();
	List<DetectorInfo> squareDetectors = new ArrayList<DetectorInfo>();
	List<DetectorInfo> circleHexDetctors = new ArrayList<DetectorInfo>();
	List<DetectorInfo> circleRegDetectors = new ArrayList<DetectorInfo>();

	public CalibrationDetectionRegression() {
		super(BoofRegressionConstants.TYPE_CALIBRATION);

		chessDirectories.add("data/calibration_stereo/Bumblebee2_Chess");
		chessDirectories.add("data/calibration_mono/chessboard/Sony_DSC-HX5V");
		chessDirectories.add("data/calibration_mono/chessboard/large");
		chessDirectories.add("data/calibration_mono/chessboard/distant");
		chessDirectories.add("data/calibration_mono/chessboard/hard");
		chessDirectories.add("data/calibration_mono/chessboard/border");
		chessDirectories.add("data/calibration_mono/chessboard/fisheye");
		chessDirectories.add("data/calibration_mono/chessboard/close");

		squareDirectories.add("data/calibration_stereo/Bumblebee2_Square");
		squareDirectories.add("data/calibration_mono/square_grid/Sony_DSC-HX5V");
		squareDirectories.add("data/calibration_mono/square_grid/large");
		squareDirectories.add("data/calibration_mono/square_grid/distant");
		squareDirectories.add("data/calibration_mono/square_grid/fisheye");

		circleHexDirectories.add("data/calibration_mono/circle_hexagonal/calib_5x6");
		circleHexDirectories.add("data/calibration_mono/circle_hexagonal/calib_24x28");
		circleHexDirectories.add("data/calibration_mono/circle_hexagonal/fisheye_5x6");
		circleHexDirectories.add("data/calibration_mono/circle_hexagonal/large_24x28");

		circleRegirectories.add("data/calibration_mono/circle_regular/distant");
		circleRegirectories.add("data/calibration_mono/circle_regular/large");
		circleRegirectories.add("data/calibration_mono/circle_regular/fisheye");

		addDetector("DetectCalibChess",
				new CreateChessboard(),
				CalibrationPatterns.CHESSBOARD);
		addDetector("DetectCalibSquare",
				new CreateSquareGrid(),
				CalibrationPatterns.SQUARE_GRID);
		addDetector("DetectCalibCircleHexagonal",
				new CreateCircleHexagonal(),
				CalibrationPatterns.CIRCLE_HEXAGONAL);
		addDetector("DetectCalibCircleRegular",
				new CreateCircleRegular(),
				CalibrationPatterns.CIRCLE_GRID);
	}

	public void addDetector( String name , CreateCalibration detector , CalibrationPatterns type) {
		switch( type ) {
			case CHESSBOARD:chessDetectors.add(new DetectorInfo(name,detector));break;
			case SQUARE_GRID:squareDetectors.add(new DetectorInfo(name,detector));break;
			case CIRCLE_HEXAGONAL:
				circleHexDetctors.add(new DetectorInfo(name,detector));break;
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

		for( DetectorInfo d : circleHexDetctors) {
			evaluate(d, circleHexDirectories);
		}

		for( DetectorInfo d : circleRegDetectors) {
			evaluate(d, circleRegirectories);
		}

	}

	private void evaluate( DetectorInfo d , List<String> directories ) throws FileNotFoundException {
		PrintStream output = new PrintStream(new File(directory,"ACC_"+d.name+".txt"));
		BoofRegressionConstants.printGenerator(output,getClass());
		output.println("# (file name) (truth error 50%) (truth error 95%)");

		OverallMetrics overallMetrics = new OverallMetrics();
		for( String dir : directories) {
			File filesArray[] = new File(dir).listFiles();
			if( filesArray != null ) {
				File descFile = new File(dir,"description.txt");
				DetectorFiducialCalibration detector = d.creator.create(descFile);

				List<File> files = Arrays.asList(filesArray);
				Collections.sort(files);
				evaluate(detector,d.name,overallMetrics,output, files);
			} else {
				errorLog.println("No files found in "+dir);
			}
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
		CreateCalibration creator;

		public DetectorInfo(String name, CreateCalibration creator) {
			this.name = name;
			this.creator = creator;
		}
	}

	interface CreateCalibration {
		DetectorFiducialCalibration create( File file );
	}

	static class CreateChessboard implements CreateCalibration {
		@Override
		public DetectorFiducialCalibration create(File file) {
			ConfigChessboard config;
			if( !file.exists() )
				config = new ConfigChessboard(7, 5,30);
			else {
				throw new RuntimeException("Implement");
			}
			return FactoryFiducialCalibration.chessboard(config);
		}
	}

	static class CreateSquareGrid implements CreateCalibration {
		@Override
		public DetectorFiducialCalibration create(File file) {
			ConfigSquareGrid config;
			if( !file.exists() )
				config = new ConfigSquareGrid(4, 3,30,30);
			else {
				throw new RuntimeException("Implement");
			}
			return FactoryFiducialCalibration.squareGrid(config);
		}
	}

	static class CreateCircleRegular implements CreateCalibration {
		@Override
		public DetectorFiducialCalibration create(File file) {
			ConfigCircleRegularGrid config;
			if( !file.exists() )
				config = new ConfigCircleRegularGrid(4, 3,4,6);
			else {
				throw new RuntimeException("Implement");
			}
			return FactoryFiducialCalibration.circleRegularGrid(config);
		}
	}

	static class CreateCircleHexagonal implements CreateCalibration {
		@Override
		public DetectorFiducialCalibration create(File file) {
			ConfigCircleHexagonalGrid config;
			if( !file.exists() )
				config = new ConfigCircleHexagonalGrid(8, 5,2,6);
			else {
				config = parseCircleHexagonalConfig(file);
			}
			return FactoryFiducialCalibration.circleHexagonalGrid(config);
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
