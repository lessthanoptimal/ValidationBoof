package regression;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.ConfigSquareGrid;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageFloat32;
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

	List<String> chessDirectories = new ArrayList<String>();
	List<String> squareDirectories = new ArrayList<String>();

	List<DetectorInfo> chessDetectors = new ArrayList<DetectorInfo>();
	List<DetectorInfo> squareDetectors = new ArrayList<DetectorInfo>();


	public CalibrationDetectionRegression() {

		chessDirectories.add("data/calib/stereo/Bumblebee2_Chess");
		chessDirectories.add("data/calib/mono/Sony_DSC-HX5V_Chess");

		squareDirectories.add("data/calib/stereo/Bumblebee2_Square");
		squareDirectories.add("data/calib/mono/Sony_DSC-HX5V_Square");

		addDetector("DetectCalibChess", FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(5, 7,30)), true);
		addDetector("DetectCalibSquare", FactoryPlanarCalibrationTarget.detectorSquareGrid(new ConfigSquareGrid(5, 7,30,30)), false);
	}

	public void addDetector( String name , PlanarCalibrationDetector detector , boolean chess ) {
		if( chess) {
			chessDetectors.add(new DetectorInfo(name,detector));
		} else {
			squareDetectors.add(new DetectorInfo(name,detector));
		}
	}

	@Override
	public void process(ImageDataType type) throws IOException {

		if( type != ImageDataType.F32 ) {
			throw new RuntimeException("Only supports floating point images");
		}

		for( DetectorInfo d : chessDetectors ) {
			evaluate(d,chessDirectories);
		}

		for( DetectorInfo d : squareDetectors) {
			evaluate(d, squareDirectories);
		}

	}

	private void evaluate( DetectorInfo d , List<String> directories ) throws FileNotFoundException {
		PrintStream output = new PrintStream(new File(directory,d.name+".txt"));
		output.println("# (file name) (truth error 50%) (truth error 95%)");

		GrowQueue_F64 allErrors = new GrowQueue_F64();
		for( String dir : directories) {
			List<File> files = Arrays.asList(new File(dir).listFiles());

			Collections.sort(files);
			evaluate(d.detector,d.name,allErrors,output, files);
		}
		output.println();

		Arrays.sort(allErrors.data,0,allErrors.size);
		double error50 = allErrors.data[(int)(allErrors.size*0.5)];
		double error95 = allErrors.data[(int)(allErrors.size*0.95)];
		output.println("Summary: 50% = "+error50+"  95% = "+error95);

		output.close();
	}

	private void evaluate(PlanarCalibrationDetector detector, String detectorName, GrowQueue_F64 allErrors,
						  PrintStream output, List<File> files) {
		for( File f : files ) {
			if( !f.getName().endsWith("jpg") )
				continue;

			String dataSetName = f.getPath();
			String path = f.getAbsolutePath();
			String pathTruth = path.substring(0,path.length()-3) + "txt";

			ImageFloat32 image = UtilImageIO.loadImage(f.getAbsolutePath(), ImageFloat32.class);

			try {
				List<Point2D_F64> groundTruth = PointFileCodec.load(pathTruth);

				if( detector.process(image) ) {
					double errors[] = new double[ groundTruth.size() ];

					List<Point2D_F64> found = detector.getDetectedPoints();
					if( found.size() != groundTruth.size() ) {
						errorLog.println(dataSetName+" different sizes. "+found.size()+" "+groundTruth.size());
					} else {
						for (int i = 0; i < found.size(); i++) {
							errors[i] = distanceFromClosest(found.get(i),groundTruth);
							allErrors.add(errors[i]);
						}

						Arrays.sort(errors);

						double e50 = errors[errors.length/2];
						double e95 = errors[(int)((errors.length-1)*0.95)];

						output.println(dataSetName+" "+e50+" "+e95);
					}
				} else {
					output.println(detectorName+" "+dataSetName+" failed");
				}
			} catch( Exception e ) {
				errorLog.println(detectorName+" "+dataSetName+" no ground truth");
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

	private class DetectorInfo
	{
		String name;
		PlanarCalibrationDetector detector;

		public DetectorInfo(String name, PlanarCalibrationDetector detector) {
			this.name = name;
			this.detector = detector;
		}
	}

	public static void main(String[] args) throws IOException {
		CalibrationDetectionRegression app = new CalibrationDetectionRegression();


//		app.addDetector("Default_Chess",FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(5,7)),true);
//		app.addDetector("Default_Square",FactoryPlanarCalibrationTarget.detectorSquareGrid(new ConfigSquareGrid(5, 7)),false);

		app.setOutputDirectory("./");
		app.process(ImageDataType.F32);
	}
}
