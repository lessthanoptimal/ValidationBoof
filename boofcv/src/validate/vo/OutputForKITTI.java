package validate.vo;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
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
		SequenceStereoImages data = new WrapParseKITTI("../data/KITTI","00");

		Class imageType = ImageFloat32.class;

//		ImagePointTracker<ImageFloat32> tracker =
//				FactoryPointSequentialTracker.dda_FAST_BRIEF(500, 200, 3, 9, 20, imageType);
//		ImagePointTracker<ImageFloat32> tracker =
//				FactoryPointSequentialTracker.dda_ShiTomasi_BRIEF(500,200,1,1,imageType,null);
//		ImagePointTracker<ImageFloat32> tracker =
//				FactoryPointSequentialTracker.dda_FH_SURF(500,2,200,1,true,imageType);
		ImagePointTracker<ImageFloat32> tracker =
				FactoryPointSequentialTracker.klt(2000, new int[]{1, 2, 4, 8}, 3, 3, 2, imageType, ImageFloat32.class);
//		ImagePointTracker<ImageFloat32> tracker =
//				FactoryPointSequentialTracker.combined_FH_SURF_KLT(500, 200,1,1,3,new int[]{1, 2, 4, 8}, 1000, false,imageType);
//		ImagePointTracker<ImageFloat32> tracker =
//				FactoryPointSequentialTracker.combined_ST_SURF_KLT(500,3,1,3,new int[]{1, 2, 4, 8}, 60, false,imageType,null);

		// TODO add stereo NCC error to handle
		StereoDisparitySparse<ImageFloat32> disparity =
				FactoryStereoDisparity.regionSparseWta(10, 120, 2, 2, 30, 0.1, true, imageType);

		StereoVisualOdometry alg = FactoryVisualOdometry.stereoDepth(120, 2, 1.5, tracker, disparity, 100, imageType);


		PrintStream output = new PrintStream(new FileOutputStream("results00.txt"));

		computeOdometry(data,alg,imageType,output);

		output.close();
	}
}
