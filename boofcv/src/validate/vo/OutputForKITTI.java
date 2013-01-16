package validate.vo;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribeMulti;
import boofcv.abst.feature.detdesc.DetectDescribeMultiFusion;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.interest.DetectorInterestPointMulti;
import boofcv.abst.feature.detect.interest.GeneralToInterestMulti;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.se.Se3_F64;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
* @author Peter Abeles
*/
public class OutputForKITTI {

	public static <T extends ImageSingleBand>
	void computeOdometry( SequenceStereoImages data,
						  StereoVisualOdometry<T> alg ,
						  Class<T> imageType ,
						  PrintStream output ) {

		T inputLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		T inputRight = GeneralizedImageOps.createSingleBand(imageType,1,1);

		alg.setCalibration(data.getCalibration());
		int totalFrames = 0;

		long before = System.currentTimeMillis();

		data.next();
		inputLeft.reshape(data.getLeft().getWidth(),data.getLeft().getHeight());
		inputRight.reshape(data.getRight().getWidth(),data.getRight().getHeight());

		do {
			ConvertBufferedImage.convertFrom(data.getLeft(), inputLeft);
			ConvertBufferedImage.convertFrom(data.getRight(), inputRight);

			if( !alg.process(inputLeft,inputRight) ) {
				throw new RuntimeException("Updated failed!??!");
			} else {
				Se3_F64 found = alg.getLeftToWorld();

				for( int row = 0; row < 3; row++ ) {
					for( int i = 0; i < 3; i++ )
						output.printf("%15e ", found.getR().get(row, i));
					output.printf("%15e", found.getT().getIndex(row));
					if( row != 2 )
						output.print(" ");
				}
				output.println();
			}
			System.out.println("Processed "+totalFrames++);
		} while( data.next() );
		long after = System.currentTimeMillis();
		System.out.print("Frame Average: "+(after-before)/(totalFrames*1000.0));
	}

	public static void main( String args[] ) throws FileNotFoundException {

		Class imageType = ImageFloat32.class;
		Class derivType = ImageFloat32.class;

		for( int dataSet = 0; dataSet < 11; dataSet++ ) {
//			GeneralFeatureIntensity intensity =
//					FactoryIntensityPoint.hessian(HessianBlobIntensity.Type.DETERMINANT, imageType);
			GeneralFeatureIntensity intensity =
					FactoryIntensityPoint.shiTomasi(2, false, imageType);
			NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(new ConfigExtract(4, 400, 0, true, false, true));
			GeneralFeatureDetector general = new GeneralFeatureDetector(intensity,nonmax);
			DetectorInterestPointMulti detector = new GeneralToInterestMulti(general,1,imageType,derivType);
//			DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(16,512,-1,4,true,imageType);
//			DescribeRegionPoint describe = FactoryDescribeRegionPoint.pixelNCC(11,11,imageType);
			DescribeRegionPoint describe = FactoryDescribeRegionPoint.surfFast(null, imageType);
			DetectDescribeMulti detDescMulti =  new DetectDescribeMultiFusion(detector,null,describe);

			StereoVisualOdometry alg = FactoryVisualOdometry.stereoQuadPnP(1.5, 0.1 , Double.MAX_VALUE, 5000, 50, detDescMulti, imageType);

			String dataID = String.format("%02d",dataSet);

			SequenceStereoImages data = new WrapParseKITTI("../data/KITTI",dataID);
			PrintStream output = new PrintStream(new FileOutputStream(dataID+".txt"));

			computeOdometry(data,alg,imageType,output);

			output.close();
		}
	}
}
