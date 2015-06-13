package validate.features.corner;

import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import validate.misc.PointFileCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates files which list all the detected features.  Basically creates ground truth for changes
 * to the algortihms
 *
 * @author Peter Abeles
 */
public class GenerateCornerFeatureFiles {

	public static String ImagePath = "data/outdoors_gray.png";

	public static String outputDir = "data/corner_regression";
	List<ImageDataType> imageTypes = new ArrayList<ImageDataType>();

	public GenerateCornerFeatureFiles() {

		imageTypes.add( ImageDataType.U8);
		imageTypes.add( ImageDataType.F32);
	}

	public void generateAll() {
		for( ImageDataType type : imageTypes ) {
			generateAll( ImageDataType.typeToSingleClass(type));
		}
	}

	public void generateAll( Class imageType ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		List<AlgInfo> detectors = createAlgorithms(imageType,derivType);

		AnyImageDerivative anyDeriv = GImageDerivativeOps.createDerivatives(
				imageType, FactoryImageGenerator.create(derivType));

		for (int i = 0; i < detectors.size(); i++) {
			ImageSingleBand input = UtilImageIO.loadImage(ImagePath,imageType);
			anyDeriv.setInput(input);
			ImageSingleBand derivX = anyDeriv.getDerivative(true);
			ImageSingleBand derivY = anyDeriv.getDerivative(false);
			ImageSingleBand derivXX = anyDeriv.getDerivative(true, true);
			ImageSingleBand derivYY = anyDeriv.getDerivative(false, false);
			ImageSingleBand derivXY = anyDeriv.getDerivative(true, false);

			AlgInfo info = detectors.get(i);
			info.detector.process(input, derivX, derivY, derivXX, derivYY, derivXY);

			List<Point2D_F64> points = new ArrayList<Point2D_F64>();
			if( info.detector.isDetectMaximums()) {
				QueueCorner corners = info.detector.getMaximums();
				for (int j = 0; j < corners.size; j++) {
					Point2D_I16 c = corners.get(j);
					points.add( new Point2D_F64(c.x,c.y));
				}
			}
			if( info.detector.isDetectMinimums() ) {
				QueueCorner corners = info.detector.getMinimums();
				for (int j = 0; j < corners.size; j++) {
					Point2D_I16 c = corners.get(j);
					points.add( new Point2D_F64(c.x,c.y));
				}
			}
			String fileName = info.name+"_"+(ImageDataType.classToType(imageType))+".txt";
			PointFileCodec.save(outputDir+"/"+fileName,"Detected Corner Points",points);
		}
	}



	public static List<AlgInfo> createAlgorithms( Class inputType, Class derivType ) {

		List<AlgInfo> out = new ArrayList<AlgInfo>();

		ConfigGeneralDetector confDector = new ConfigGeneralDetector(200,2,0.1f);
		out.add( new AlgInfo("FAST",FactoryDetectPoint.createFast(null,confDector,inputType)) );
		out.add( new AlgInfo("ShiTomasi",FactoryDetectPoint.createShiTomasi(confDector,false,derivType)) );
		out.add( new AlgInfo("ShiTomasiW",FactoryDetectPoint.createShiTomasi(confDector, true, derivType)) );
		out.add( new AlgInfo("Harris",FactoryDetectPoint.createHarris(confDector, false, derivType)) );
		out.add( new AlgInfo("HarrisW",FactoryDetectPoint.createHarris(confDector,true,derivType)) );
		out.add( new AlgInfo("KitRos",FactoryDetectPoint.createKitRos(confDector, derivType)) );
		out.add( new AlgInfo("Median",FactoryDetectPoint.createMedian(confDector,inputType)) );

		GeneralFeatureIntensity intensityHessianDet =
				FactoryIntensityPoint.hessian(HessianBlobIntensity.Type.DETERMINANT,derivType);
		GeneralFeatureIntensity intensityHessianTrace =
				FactoryIntensityPoint.hessian(HessianBlobIntensity.Type.TRACE,derivType);
		GeneralFeatureIntensity intensityLaplacian =
				FactoryIntensityPoint.laplacian();

		out.add( new AlgInfo("HessianDet",FactoryDetectPoint.createGeneral(intensityHessianDet,confDector)) );
		out.add( new AlgInfo("HessianTrace",FactoryDetectPoint.createGeneral(intensityHessianTrace,confDector)) );
		out.add( new AlgInfo("Laplacian",FactoryDetectPoint.createGeneral(intensityLaplacian,confDector)) );

		return out;
	}

	public static class AlgInfo {
		public GeneralFeatureDetector detector;
		public String name;

		public AlgInfo(String name, GeneralFeatureDetector detector) {
			this.detector = detector;
			this.name = name;
		}
	}

	public static void main(String[] args) {
		GenerateCornerFeatureFiles generator = new GenerateCornerFeatureFiles();

		generator.generateAll();
	}
}
