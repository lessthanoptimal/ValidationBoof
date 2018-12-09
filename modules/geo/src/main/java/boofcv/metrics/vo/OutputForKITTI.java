package boofcv.metrics.vo;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.se.Se3_F64;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
* @author Peter Abeles
*/
public class OutputForKITTI {

	public static <T extends ImageGray<T>>
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
			ConvertBufferedImage.convertFrom(data.getLeft(), inputLeft,true);
			ConvertBufferedImage.convertFrom(data.getRight(), inputRight,true);

			if( !alg.process(inputLeft,inputRight) ) {
				throw new RuntimeException("Updated failed!??!");
			} else {
				Se3_F64 found = alg.getCameraToWorld();

				for( int row = 0; row < 3; row++ ) {
					for( int i = 0; i < 3; i++ )
						output.printf("%15e ", found.getR().get(row, i));
					output.printf("%15e", found.getT().getIdx(row));
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

		Class imageType = GrayF32.class;
		Class derivType = GrayF32.class;

		for( int dataSet = 0; dataSet < 11; dataSet++ ) {
			PkltConfig configKlt = new PkltConfig();
			configKlt.pyramidScaling = new int[]{1, 2, 4, 8};
			configKlt.templateRadius = 3;

			PointTrackerTwoPass tracker =
					FactoryPointTrackerTwoPass.klt(configKlt, new ConfigGeneralDetector(600, 3, 1),
							imageType, derivType);

			// TODO add stereo NCC error to handle
			StereoDisparitySparse<GrayF32> disparity =
					FactoryStereoDisparity.regionSparseWta(10, 120, 2, 2, 30, 0.1, true, imageType);

			StereoVisualOdometry alg = FactoryVisualOdometry.stereoDepth(1.5,120, 2,200,50,false,disparity, tracker,imageType);

			String dataID = String.format("%02d",dataSet);

			SequenceStereoImages data = new WrapParseKITTI("data/KITTI",dataID);
			PrintStream output = new PrintStream(new FileOutputStream(dataID+".txt"));

			computeOdometry(data,alg,imageType,output);

			output.close();
		}
	}
}
