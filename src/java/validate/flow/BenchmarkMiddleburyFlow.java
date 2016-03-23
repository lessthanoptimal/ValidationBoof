package validate.flow;

import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import org.ddogleg.struct.GrowQueue_F64;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * @author Peter Abeles
 */
public class BenchmarkMiddleburyFlow<T extends ImageBase> {

	String dataSets[] = new String[]{"Dimetrodon","Grove2","Grove3","Hydrangea","RubberWhale",
			"Urban2","Urban3","Venus"};


	String dataDirectory;

	DenseOpticalFlow<T> algorithm;

	PrintStream out;

	public BenchmarkMiddleburyFlow( String dataDirectory ,
									DenseOpticalFlow<T> algorithm ,
									PrintStream out ) {
		this.dataDirectory = dataDirectory;
		this.out = out;
		this.algorithm = algorithm;

	}

	public void evaluate() throws IOException {

		out.println("# dataset totalValid (error 50%) (error 90%) (error 95%)");

		GrowQueue_F64 errors = new GrowQueue_F64();

		for( String which : dataSets ) {
			String nameTruth = dataDirectory+"/other-gt-flow/"+which+"/flow10.flo";

			String imageName0 = dataDirectory+"/other-data-gray/"+which+"/frame10.png";
			String imageName1 = dataDirectory+"/other-data-gray/"+which+"/frame11.png";

			BufferedImage image0 = UtilImageIO.loadImage(imageName0);
			BufferedImage image1 = UtilImageIO.loadImage(imageName1);

			ImageFlow flowTruth = ParseMiddleburyFlow.parse(nameTruth);

			T input0 = algorithm.getInputType().createImage(flowTruth.width, flowTruth.height);
			T input1 = algorithm.getInputType().createImage(flowTruth.width, flowTruth.height);
			ConvertBufferedImage.convertFrom(image0,input0,true);
			ConvertBufferedImage.convertFrom(image1,input1,true);

			ImageFlow flowFound = new ImageFlow(flowTruth.width, flowTruth.height);

			algorithm.process(input0,input1,flowFound);

			// score the results
			errors.reset();
			for (int y = 0; y < input0.height; y++) {
				for (int x = 0; x < input0.width; x++) {
					ImageFlow.D f = flowFound.get(x,y);
					ImageFlow.D t = flowTruth.get(x,y);

					if( f.isValid() && t.isValid() ) {
						float dx = f.x - t.x;
						float dy = f.y - t.y;

						errors.add( Math.sqrt(dx*dx + dy*dy));
					}
				}
			}

			Arrays.sort(errors.data,0,errors.size);

			double error50 = errors.get( errors.size/2 );
			double error90 = errors.get( (int)(errors.size*0.9) );
			double error95 = errors.get( (int)(errors.size*0.95) );

			out.printf("%20s %6d %7.2f %7.2f %7.2f\n",which,errors.size,error50,error90,error95);
			if( out != System.out ) {
				System.out.printf("%20s %6d %7.2f %7.2f %7.2f\n",which,errors.size,error50,error90,error95);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		DenseOpticalFlow<GrayF32> denseFlow =
				FactoryDenseOpticalFlow.flowKlt(null, 6, GrayF32.class, null);
//				FactoryDenseOpticalFlow.region(null, GrayF32.class);
//				FactoryDenseOpticalFlow.hornSchunck(20, 1000, GrayF32.class);
//				FactoryDenseOpticalFlow.hornSchunckPyramid(null,GrayF32.class);
//				FactoryDenseOpticalFlow.broxWarping(null, GrayF32.class);

		BenchmarkMiddleburyFlow benchmark = new BenchmarkMiddleburyFlow("data/denseflow",denseFlow,System.out);

		benchmark.evaluate();
	}
}
