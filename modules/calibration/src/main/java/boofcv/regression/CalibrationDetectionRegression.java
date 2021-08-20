package boofcv.regression;

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.common.*;
import boofcv.common.misc.PointFileCodec;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDataType;
import com.google.common.base.Strings;
import georegression.struct.point.Point2D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import static boofcv.common.parsing.ParseCalibrationConfigFiles.parseGridDimen3;
import static boofcv.common.parsing.ParseCalibrationConfigFiles.parseGridDimen4;

/**
 * @author Peter Abeles
 */
public class CalibrationDetectionRegression extends BaseRegression implements ImageRegression  {

	List<String> chessDirectories = new ArrayList<>();
	List<String> squareDirectories = new ArrayList<>();
	List<String> circleHexDirectories = new ArrayList<>();
	List<String> circleRegirectories = new ArrayList<>();

	List<DetectorInfo> chessDetectors = new ArrayList<>();
	List<DetectorInfo> squareDetectors = new ArrayList<>();
	List<DetectorInfo> circleHexDetctors = new ArrayList<>();
	List<DetectorInfo> circleRegDetectors = new ArrayList<>();

	RuntimeSummary outputRuntime;

	public CalibrationDetectionRegression() {
		super(BoofRegressionConstants.TYPE_CALIBRATION);

		chessDirectories.add("data/calibration_stereo/Bumblebee2_Chess");
		chessDirectories.add("data/calibration_mono/chessboard/border");
		chessDirectories.add("data/calibration_mono/chessboard/caltech_edges");
		chessDirectories.add("data/calibration_mono/chessboard/challenge");
		chessDirectories.add("data/calibration_mono/chessboard/close");
		chessDirectories.add("data/calibration_mono/chessboard/deltille_fisheye");
		chessDirectories.add("data/calibration_mono/chessboard/distance_angle");
		chessDirectories.add("data/calibration_mono/chessboard/distance_straight");
		chessDirectories.add("data/calibration_mono/chessboard/distant");
		chessDirectories.add("data/calibration_mono/chessboard/focus");
		chessDirectories.add("data/calibration_mono/chessboard/focus_large");
		chessDirectories.add("data/calibration_mono/chessboard/gaus_fisheye");
		chessDirectories.add("data/calibration_mono/chessboard/gaus_perfect");
		chessDirectories.add("data/calibration_mono/chessboard/hard");
		chessDirectories.add("data/calibration_mono/chessboard/large");
		chessDirectories.add("data/calibration_mono/chessboard/large_shadow");
		chessDirectories.add("data/calibration_mono/chessboard/motion_blur");
		chessDirectories.add("data/calibration_mono/chessboard/ocam_fisheye190");
		chessDirectories.add("data/calibration_mono/chessboard/ocam_kaidan_omni");
		chessDirectories.add("data/calibration_mono/chessboard/ocam_ladybug");
		chessDirectories.add("data/calibration_mono/chessboard/ocam_mini_omni");
		chessDirectories.add("data/calibration_mono/chessboard/ocam_omni");
		chessDirectories.add("data/calibration_mono/chessboard/perfect");
		chessDirectories.add("data/calibration_mono/chessboard/ricoh_theta_5");
		chessDirectories.add("data/calibration_mono/chessboard/ricoh_theta_v");
		chessDirectories.add("data/calibration_mono/chessboard/rotation_flat");
		chessDirectories.add("data/calibration_mono/chessboard/rotation_vertical");
		chessDirectories.add("data/calibration_mono/chessboard/shadow");
		chessDirectories.add("data/calibration_mono/chessboard/sloppy13x10");
		chessDirectories.add("data/calibration_mono/chessboard/Sony_DSC-HX5V");
		chessDirectories.add("data/calibration_mono/chessboard/stefano_2012");

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

		addDetector("ChessBinary", new CreateChessboardBinary(), CalibrationPatterns.CHESSBOARD);
		addDetector("ChessXCorner", new CreateChessboardXCorner(), CalibrationPatterns.CHESSBOARD);
		addDetector("Square", new CreateSquareGrid(), CalibrationPatterns.SQUARE_GRID);
		addDetector("CircleHexagonal", new CreateCircleHexagonal(), CalibrationPatterns.CIRCLE_HEXAGONAL);
		addDetector("CircleRegular", new CreateCircleRegular(), CalibrationPatterns.CIRCLE_GRID);
	}

	public void addDetector( String name , CreateCalibration detector , CalibrationPatterns type) {
		switch( type ) {
			case CHESSBOARD:chessDetectors.add(new DetectorInfo(name,detector));break;
			case SQUARE_GRID:squareDetectors.add(new DetectorInfo(name,detector));break;
			case CIRCLE_HEXAGONAL: circleHexDetctors.add(new DetectorInfo(name,detector));break;
			case CIRCLE_GRID:circleRegDetectors.add(new DetectorInfo(name,detector));break;
		}
	}

	@Override
	public void process(ImageDataType type) throws IOException {

		if( type != ImageDataType.F32 ) {
			throw new ImageTypeNotSupportedException("Only supports floating point images");
		}

		outputRuntime = new RuntimeSummary();
		outputRuntime.out = new PrintStream(new File(directoryRuntime, "RUN_CalibrationDetection.txt"));
		BoofRegressionConstants.printGenerator(outputRuntime.out, getClass());
		outputRuntime.out.println("# All times are in milliseconds");
		outputRuntime.out.println();

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

		outputRuntime.printSummaryResults();
		outputRuntime.out.close();
	}

	private void evaluate( DetectorInfo d , List<String> directories ) throws FileNotFoundException {
		outputRuntime.out.println(d.name);
		outputRuntime.printUnitsRow(false);

		PrintStream output = new PrintStream(new File(directoryMetrics,"ACC_DetectCalib"+d.name+".txt"));
		BoofRegressionConstants.printGenerator(output,getClass());
		output.println("# (file name) (truth error 50%) (truth error 95%) (truth error 100%)");

		// Sort it to ensure the same order
		directories.sort(String.CASE_INSENSITIVE_ORDER);

		DogArray_F64 summaryTimeMS = new DogArray_F64();

		Map<String,OverallMetrics> dirMetrics = new HashMap<>();
		try {
			OverallMetrics overallMetrics = new OverallMetrics();

			for (String dir : directories) {
				File[] filesArray = new File(dir).listFiles();
				if (filesArray != null) {
					File descFile = new File(dir, "description.txt");
					DetectSingleFiducialCalibration detector = d.creator.create(descFile);

					List<File> files = Arrays.asList(filesArray);
					Collections.sort(files);

					OverallMetrics directoryMetrics = new OverallMetrics();
					evaluate(detector, d.name, directoryMetrics, output, files);
					overallMetrics.add(directoryMetrics);
					dirMetrics.put(dir,directoryMetrics);
					if( directoryMetrics.total == 0 ) {
						errorLog.println("Failed to find images in dir");
					}
					summaryTimeMS.addAll(directoryMetrics.processingTimeMS);
					outputRuntime.printStatsRow(new File(dir).getName(),directoryMetrics.processingTimeMS);
				} else {
					errorLog.println("No files found in " + dir);
				}
			}
			outputRuntime.out.println();
			outputRuntime.saveSummary(d.name,summaryTimeMS);

			output.println("\n"+ Strings.repeat("-",80)+"\n");

			// Print out summary results for individual directories
			for( String dir : directories ) {
				OverallMetrics m = dirMetrics.get(dir);
				if( m == null ) {
					errorLog.println("No metrics found for " + dir);
					continue;
				}
				String name = new File(dir).getName();

				printSummary(output, m, name);
			}

			// Summarize Everything all together
			output.println("\n"+ Strings.repeat("-",80)+"\n");
			printSummary(output, overallMetrics, "Summary");

		} catch ( Exception e ) {
			e.printStackTrace();
			e.printStackTrace(errorLog);
		} finally {
			output.close();
		}
	}

	private void printSummary(PrintStream output, OverallMetrics m, String name) {
		m.errors.sort();
		double error50,error95,error100;
		int success = 0;
		if( m.errors.size > 0 ) {
			error50 = m.errors.getFraction(0.5);
			error95 = m.errors.getFraction(0.95);
			error100 = m.errors.getFraction(1.00);
			success = m.total - m.failed;
		} else {
			error50 = Double.NaN;
			error95 = Double.NaN;
			error100 = Double.NaN;
		}

		output.printf("%-35s Errors{ 50%% %6.3f , 95%% %6.3f , MAX %6.3f }, Success : %2d / %2d\n",
				name,error50,error95,error100,success,m.total);
	}

	private void evaluate(DetectSingleFiducialCalibration detector, String detectorName,
						  OverallMetrics metrics, PrintStream output, List<File> files) {
		metrics.processingTimeMS.reset();
		for( File f : files ) {
			String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
			if( !(extension.equals("jpg") || extension.equals("png")) )
				continue;

			String dataSetName = new File(f.getParentFile().getName(),f.getName()).getPath();
			String path = f.getAbsolutePath();
			String pathTruth = path.substring(0,path.length()-3) + "txt";

			GrayF32 image = UtilImageIO.loadImage(f.getAbsolutePath(), GrayF32.class);
			if( image == null ) {
				errorLog.println(detectorName+" failed to load image "+dataSetName);
				output.printf("%-35s  image_error\n",dataSetName);
				metrics.failed++; // mark as a failure so that it will raise a red flag when inspecting metrics
				continue;
			}

			List<Point2D_F64> groundTruth;
			try {
				groundTruth = PointFileCodec.load(pathTruth);
			} catch( Exception e ) {
				errorLog.println(detectorName+" "+dataSetName+" no ground truth. "+e.getMessage());
				continue;
			}
			if( groundTruth == null ) {
				errorLog.println(detectorName + " failed to load point file " + pathTruth);
				output.printf("%-35s  point_file_error\n", dataSetName);
				metrics.failed++; // mark as a failure so that it will raise a red flag when inspecting metrics
				continue;
			}

			metrics.total++;

			try {
				long time0 = System.nanoTime();
				boolean success = detector.process(image);
				long time1 = System.nanoTime();
				metrics.processingTimeMS.add((time1-time0)*1e-6);

				if( success ) {
					double[] errors = new double[ groundTruth.size() ];

					CalibrationObservation found = detector.getDetectedPoints();
					if( found.size() != groundTruth.size() ) {
						errorLog.println(dataSetName+" different point counts. found="+found.size()+" truth="+groundTruth.size());
					} else {
						for (int i = 0; i < found.size(); i++) {
							errors[i] = distanceFromClosest(found.points.get(i).p,groundTruth);
							metrics.errors.add(errors[i]);
						}

						Arrays.sort(errors);

						double e50 = errors[errors.length/2];
						double e95 = errors[(int)((errors.length-1)*0.95)];
						double e100 = errors[errors.length-1];

						output.printf("%-35s %7.4f %7.4f %7.4f\n",dataSetName,e50,e95,e100);
					}
				} else {
					output.printf("%-35s  detection_failed\n",dataSetName);
					metrics.failed++;
				}
			} catch( Exception e ) {
				errorLog.println(detectorName+" "+dataSetName+" detector threw "+e.getClass().getSimpleName()+" message="+e.getMessage());
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

	private static class OverallMetrics {
		final DogArray_F64 errors = new DogArray_F64();
		int total;
		int failed;
		final DogArray_F64 processingTimeMS = new DogArray_F64();

		public void add( OverallMetrics src ) {
			errors.addAll(src.errors);
			this.total += src.total;
			this.failed += src.failed;
		}
	}

	private static class DetectorInfo
	{
		String name;
		CreateCalibration creator;

		public DetectorInfo(String name, CreateCalibration creator) {
			this.name = name;
			this.creator = creator;
		}
	}

	interface CreateCalibration {
		DetectSingleFiducialCalibration create( File file );
	}

	static class CreateChessboardBinary implements CreateCalibration {
		@Override
		public DetectSingleFiducialCalibration create(File file) {
			ConfigGridDimen config;
			if( !file.exists() )
				config = new ConfigGridDimen(7, 5,30);
			else {
				config = parseGridDimen3(file);
			}
			return FactoryFiducialCalibration.chessboardB(null,config);
		}
	}

	static class CreateChessboardXCorner implements CreateCalibration {
		@Override
		public DetectSingleFiducialCalibration create(File file) {
			ConfigGridDimen config;
			if( !file.exists() )
				config = new ConfigGridDimen(7,5,30);
			else {
				config = parseGridDimen3(file);
			}
			return FactoryFiducialCalibration.chessboardX(null,config);
		}
	}

	static class CreateSquareGrid implements CreateCalibration {
		@Override
		public DetectSingleFiducialCalibration create(File file) {
			ConfigGridDimen config;
			if( !file.exists() )
				config = new ConfigGridDimen(4, 3,30,30);
			else {
				config = parseGridDimen4(file);
			}
			return FactoryFiducialCalibration.squareGrid(null,config);
		}
	}

	static class CreateCircleRegular implements CreateCalibration {
		@Override
		public DetectSingleFiducialCalibration create(File file) {
			ConfigGridDimen config;
			if( !file.exists() )
				config = new ConfigGridDimen(4, 3,4,6);
			else {
				config = parseGridDimen4(file);
			}
			return FactoryFiducialCalibration.circleRegularGrid(null,config);
		}
	}

	static class CreateCircleHexagonal implements CreateCalibration {
		@Override
		public DetectSingleFiducialCalibration create(File file) {
			ConfigGridDimen config;
			if( !file.exists() )
				config = new ConfigGridDimen(8, 5,2,6);
			else {
				config = parseGridDimen4(file);
			}
			return FactoryFiducialCalibration.circleHexagonalGrid(null,config);
		}
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{CalibrationDetectionRegression.class.getName(),ImageDataType.F32.toString()});
//		RegressionRunner.main(new String[]{CalibrationDetectionRegression.class.getName(),ImageDataType.U8.toString()});

//		CalibrationDetectionRegression app = new CalibrationDetectionRegression();
//		app.setOutputDirectory("./");
//		app.process(ImageDataType.F32);
	}
}
