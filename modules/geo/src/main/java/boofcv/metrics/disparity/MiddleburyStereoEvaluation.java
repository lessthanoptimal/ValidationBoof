package boofcv.metrics.disparity;

import boofcv.abst.disparity.StereoDisparity;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class MiddleburyStereoEvaluation<T extends ImageGray<T>> {

	public static <T extends ImageGray<T>>
	List<Score> evaluateAll(String path , StereoDisparity<T,GrayF32> stereo , float badThresh ) {
		File[] _tests = new File(path).listFiles();
		if( _tests == null )
			throw new RuntimeException("No children in directory?");

		// Put into alphabetical order to make the printed results consistent across systems
		List<File> tests = Arrays.asList(_tests);
		tests.sort(Comparator.comparing(File::getName));

		T left = stereo.getInputType().createImage(1,1);
		T right = stereo.getInputType().createImage(1,1);
		GrayF32 gtDisp = new GrayF32(1,1);
		GrayU8 mask = new GrayU8(1,1);

		List<Score> results = new ArrayList<>();

		boolean first = true;

		for( File f : tests ) {
			if( f.isFile() )
				continue;
			if( !new File(f,"calib.txt").exists() )
				continue;

			loadTraining(f,left,right,gtDisp,mask);

			// Process the image. if it was fast to run then run it again multiple times to give the JVM time
			// to optimize and provide more accurate results. If it's not fast then it doesn't make much
			// of a difference.
			double runtimeMS=Double.POSITIVE_INFINITY;
			for (int i = 0; i < 3; i++) {
				long time0 = System.nanoTime();
				stereo.process(left,right);
				long time1 = System.nanoTime();
				runtimeMS = (time1-time0)*1e-6;

				if( runtimeMS > 1_000.0 || !first) {
					break;
				}
			}
			first = false;

			GrayF32 foundDisparity = stereo.getDisparity();
			float invalid = stereo.getInvalidValue();
			final int N = foundDisparity.totalPixels();
			for (int i = 0; i < N; i++) {
				if( foundDisparity.data[i] >= invalid ) {
					foundDisparity.data[i] = Float.POSITIVE_INFINITY;
				}
			}
			// TODO handle invalid pixels. Fill in with NN?
			Score found = evaluate(foundDisparity,gtDisp,mask,badThresh,99999,false);
			found.name = f.getName();
			found.runtimeMS = runtimeMS;
			results.add(found);
		}
		return results;
	}


	public static <T extends ImageGray<T>>
	void loadTraining(File directory , T left , T right , GrayF32 gtDisp , GrayU8 mask ) {
		left.setTo(UtilImageIO.loadImage(new File(directory,"im0.png"),true, left.getImageType()));
		right.setTo(UtilImageIO.loadImage(new File(directory,"im1.png"),true, right.getImageType()));
		mask.setTo(UtilImageIO.loadImage(new File(directory,"mask0nocc.png"),true, ImageType.SB_U8));
		CodecPFM.read(new File(directory,"disp0GT.pfm"),gtDisp);
	}

	public static Score evaluate(GrayF32 disp, GrayF32 gtDisp , GrayU8 mask , float badThresh, int maxDisp , boolean roundDisp )
	{
		int scale = gtDisp.width/disp.width;
		if( scale != 1 && scale != 2 && scale != 4 )
			throw new RuntimeException("Unexpected scale "+scale);

		boolean useMask = mask.width > 0 && mask.height > 0;
		if( useMask && (mask.width!=gtDisp.width || mask.height!=gtDisp.height))
			throw new RuntimeException("Ground Truth and Mask must be the same shape");

		int n =0;
		int bad = 0;
		int invalid = 0;
		float serr = 0;
		for (int y = 0; y < gtDisp.height; y++) {
			for (int x = 0; x < gtDisp.width; x++) {
				float gt = gtDisp.get(x,y);
				if( gt == Float.POSITIVE_INFINITY )
					continue;
				float d = scale * disp.get(x/scale,y/scale);
				boolean valid = (d != Float.POSITIVE_INFINITY);
				if( valid ) {
					float maxd = scale*maxDisp;
					d = Math.max(0,Math.min(maxd,d));
				}
				if( valid && roundDisp )
					d = Math.round(d);
				float err = Math.abs(d-gt);
				if( useMask && mask.get(x,y) != 255 ){

				} else {
					n++;
					if( valid ) {
						serr += err;
						if( err > badThresh ) {
							bad++;
						}
					} else {
						invalid++;
					}
				}
			}
		}

		Score score = new Score();
		score.badPercent = 100.0f*bad/n;
		score.invalidPercent = 100.0f*invalid/n;
		score.totalBadPercent = 100.0f*(bad+invalid)/n;
		score.aveError = serr / (n-invalid);
		score.usedPercent = n/(float)gtDisp.totalPixels();

		return score;
	}

	public static class Score {
		public String name;
		public double runtimeMS;
		public float badPercent;
		public float invalidPercent;
		public float totalBadPercent;
		public float aveError;
		public float usedPercent; // percent of pixels used to evaluate
	}
}
