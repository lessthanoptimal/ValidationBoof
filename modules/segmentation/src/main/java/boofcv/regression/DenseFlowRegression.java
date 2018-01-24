package boofcv.regression;

import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.metrics.flow.BenchmarkMiddleburyFlow;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DenseFlowRegression extends BaseRegression implements ImageRegression {

	public static final String path = "data/denseflow/";

	public DenseFlowRegression() {
		super(BoofRegressionConstants.TYPE_SEGMENTATION);
	}

	@Override
	public void process( ImageDataType type ) {

		List<Info> all = new ArrayList<Info>();

		Class bandType = ImageDataType.typeToSingleClass(type);

		all.add(new Info("KLT",FactoryDenseOpticalFlow.flowKlt(null, 6, bandType, null)));
		all.add(new Info("Region",FactoryDenseOpticalFlow.region(null, bandType)));
		all.add(new Info("HornSchunck",FactoryDenseOpticalFlow.hornSchunck(null, bandType)));
		all.add(new Info("HornSchunckPyr",FactoryDenseOpticalFlow.hornSchunckPyramid(null, bandType)));
		all.add(new Info("Brox",FactoryDenseOpticalFlow.broxWarping(null, bandType)));

		for( Info i : all ) {
			System.out.println("Regression "+i.name);
			try {
				PrintStream out = new PrintStream(new File(directory,"ACC_DenseFlow"+i.name+".txt"));
				BoofRegressionConstants.printGenerator(out, getClass());
				BenchmarkMiddleburyFlow benchmark = new BenchmarkMiddleburyFlow(path,i.detdesc,out);
				benchmark.evaluate();
				out.flush();
			} catch( RuntimeException e ) {
				errorLog.print(e);
			} catch (IOException e) {
				errorLog.print(e);
			}
		}
	}

	public static class Info {
		public String name;
		public DenseOpticalFlow detdesc;

		public Info(String name, DenseOpticalFlow detdesc) {
			this.name = name;
			this.detdesc = detdesc;
		}
	}

	public static void main(String[] args) throws IOException {

		DenseFlowRegression app = new DenseFlowRegression();

		app.setOutputDirectory(BoofRegressionConstants.CURRENT_DIRECTORY+"/"+ImageDataType.F32+"/");
		app.process(ImageDataType.F32);
	}
}
