package boofcv.metrics.disparity;

import boofcv.abst.disparity.StereoDisparity;
import boofcv.common.RuntimeSummary;
import boofcv.factory.disparity.*;
import boofcv.metrics.disparity.MiddleburyStereoEvaluation.Score;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.ddogleg.struct.DogArray_F64;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static boofcv.metrics.disparity.MiddleburyStereoEvaluation.evaluateAll;
import static org.ddogleg.stats.StatisticsDogArray.mean;

/**
 * Evaluates stereo disparity using ground truth stereo images
 *
 * @author Peter Abeles
 */
public class EvaluateDisparity<T extends ImageGray<T>> {
	public static final String PATH_MIDDLEBURY = "data/disparity/MiddEval3/trainingQ";
	public static final float BAD_THRESH = 2.0f; // If the disparity error is greater than this it is considered to be bad

	public PrintStream out = System.out;
	public PrintStream err = System.err;
	public RuntimeSummary runtime = new RuntimeSummary();

	Class<T> inputType;

	public EvaluateDisparity( Class<T> inputType ) {
		this.inputType = inputType;
	}

	public void process() {
		List<TestSubject<T>> subjects = createAlgorithms(120, inputType);

		runtime.printUnitsRow(true);

		out.println("# Middlebury Results for BAD_THRESH " + BAD_THRESH);
		out.println("# err = average, bp = bad percent, ip = invalid percent, tbp = total bad percent");
		out.println();
		for (TestSubject<T> s : subjects) {
			List<Score> results;
			try {
				results = evaluateAll(PATH_MIDDLEBURY, s.alg, BAD_THRESH);
			} catch (RuntimeException e) {
				e.printStackTrace(err);
				continue;
			}
			DogArray_F64 allError = new DogArray_F64();
			DogArray_F64 allBad = new DogArray_F64();
			DogArray_F64 allInvalid = new DogArray_F64();
			DogArray_F64 allTotalBad = new DogArray_F64();

			DogArray_F64 processingTimeMS = new DogArray_F64();
			out.println(s.name);
			for (Score r : results) {
				out.printf("%20s err=%5.2f bp=%5.1f ip=%5.1f tbp=%5.1f\n", r.name, r.aveError, r.badPercent, r.invalidPercent, r.totalBadPercent);
				allError.add(r.aveError);
				allBad.add(r.badPercent);
				allInvalid.add(r.invalidPercent);
				allTotalBad.add(r.totalBadPercent);
				processingTimeMS.add(r.runtimeMS);
			}
			allError.sort();
			allBad.sort();
			allInvalid.sort();
			allTotalBad.sort();

			out.println();
			out.printf("Summary Mean : err=%5.2f bp=%5.1f ip=%5.1f tbp=%5.1f\n",
					mean(allError), mean(allBad), mean(allInvalid), mean(allTotalBad));
			out.printf("Summary 50%%  : err=%5.2f bp=%5.1f ip=%5.1f tbp=%5.1f\n",
					allError.getFraction(0.5), allBad.getFraction(0.5), allInvalid.getFraction(0.5), allTotalBad.getFraction(0.5));
			out.printf("Summary 95%%  : err=%5.2f bp=%5.1f ip=%5.1f tbp=%5.1f\n",
					allError.getFraction(0.95), allBad.getFraction(0.95), allInvalid.getFraction(0.95), allTotalBad.getFraction(0.95));
			out.println("----------------------------------------------------------------------------");

			runtime.printStatsRow(s.name, processingTimeMS);
		}
	}

	public static <T extends ImageGray<T>> List<TestSubject<T>> createAlgorithms( int range, Class<T> inputType ) {
		List<TestSubject<T>> list = new ArrayList<>();

		list.add(create_BM5(range, 3, DisparityError.SAD, inputType));
		list.add(create_BM5(range, 3, DisparityError.CENSUS, inputType));
		list.add(create_BM5(range, 3, DisparityError.NCC, inputType));

		list.add(create_BM(range, 3, DisparityError.SAD, inputType));
		list.add(create_BM(range, 3, DisparityError.CENSUS, inputType));
		list.add(create_BM(range, 3, DisparityError.NCC, inputType));

		// SGM only supports GrayU8 images
		if (inputType == GrayU8.class) {
			list.add(create_SGM(range, true, DisparitySgmError.ABSOLUTE_DIFFERENCE, inputType));
			list.add(create_SGM(range, true, DisparitySgmError.CENSUS, inputType));
			list.add(create_SGM(range, true, DisparitySgmError.MUTUAL_INFORMATION, inputType));
		}

		return list;
	}

	private static <T extends ImageGray<T>> TestSubject<T> create_BM5(
			int range, int region, DisparityError error, Class<T> inputType ) {
		ConfigDisparityBMBest5 config = new ConfigDisparityBMBest5();
		config.regionRadiusX = config.regionRadiusY = region;
		config.errorType = error;
		config.disparityRange = range;
		if (DisparityError.NCC == error) {
			config.texture = 0.005;
		}

		TestSubject<T> ts = new TestSubject<>();
		ts.alg = FactoryStereoDisparity.blockMatchBest5(config, inputType, GrayF32.class);
		ts.name = "BM5_" + error.name();
		return ts;
	}

	private static <T extends ImageGray<T>> TestSubject<T> create_BM(
			int range, int region, DisparityError error, Class<T> inputType  ) {
		ConfigDisparityBM config = new ConfigDisparityBM();
		config.regionRadiusX = config.regionRadiusY = region;
		config.errorType = error;
		config.disparityRange = range;
		if (DisparityError.NCC == error) {
			config.texture = 0.005;
		}

		TestSubject<T> ts = new TestSubject<>();
		ts.alg = FactoryStereoDisparity.blockMatch(config, inputType, GrayF32.class);
		ts.name = "BM_" + error.name();
		return ts;
	}

	private static <T extends ImageGray<T>> TestSubject<T> create_SGM(
			int range, boolean block, DisparitySgmError error, Class<T> inputType  ) {
		ConfigDisparitySGM config = new ConfigDisparitySGM();
		config.paths = ConfigDisparitySGM.Paths.P16;
		config.useBlocks = block;
		config.errorType = error;
		config.disparityRange = range;

		TestSubject<T> ts = new TestSubject<>();
		ts.alg = FactoryStereoDisparity.sgm(config, inputType, GrayF32.class);
		ts.name = "SGM_" + error.name();
		return ts;
	}

	public static class TestSubject<T extends ImageGray<T>> {
		public String name;
		public StereoDisparity<T, GrayF32> alg;
	}

	public static void main( String[] args ) {
		EvaluateDisparity<GrayU8> app = new EvaluateDisparity<>(GrayU8.class);
		app.process();
	}
}
