package validate.threshold;

import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateTextThresholdDIBCO {
	public static String directory = "data/DIBCO/2009/";

	List<ImageFloat32> input = new ArrayList<ImageFloat32>();
	List<ImageUInt8> truth = new ArrayList<ImageUInt8>();

	List<Alg> algorithms = new ArrayList<Alg>();

	long TP,FP,TN,FN;
	double totalTP,totalFP,totalTN,totalFN;

	PrintStream out;

	public void setOutputDirectory( String directory ) {
		try {
			out = new PrintStream(directory+"/text_threshold.txt");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public EvaluateTextThresholdDIBCO() {
		// load all the data
		for (int i = 1; i <= 5 ; i++) {
			load(String.format("H%02d.bmp",i),String.format("H%02d_truth.bmp",i));
		}
		for (int i = 1; i <= 5 ; i++) {
			load(String.format("P%02d.bmp",i),String.format("P%02d_truth.bmp",i));
		}
	}

	public void addAlgorithm( ThresholdText alg , String name ) {
		algorithms.add( new Alg(alg,name));
	}

	public void evaluate() {
		ImageUInt8 found = new ImageUInt8(1,1);
		for( Alg alg : algorithms ) {

			totalTP=totalFP=totalTN=totalFN=0;

			for (int i = 0; i < input.size(); i++) {

				ImageFloat32 in = input.get(i);
				ImageUInt8 expected = truth.get(i);
				found.reshape(in.width,in.height);

				alg.alg.process(in.clone(), found);

//				ShowImages.showWindow(VisualizeBinaryData.renderBinary(expected,null),"expected");
//				ShowImages.showWindow(VisualizeBinaryData.renderBinary(found,null),"found");
//				break;

				TP=FP=TN=FN=0;

				for (int y = 0; y < in.height; y++) {
					for (int x = 0; x < in.width; x++) {
						int e = expected.get(x,y);
						int f = found.get(x,y);

						if( e == 0 ) {
							if( f == 0 ) {
								TN++;
							} else {
								FP++;
							}
						} else {
							if( f == 0 ) {
								FN++;
							} else {
								TP++;
							}
						}
					}
				}

				// count each image equally when computing the overall score
				// otherwise the number of pixels will determine the weight
				double N = in.width*in.height;
				totalTP += TP/N;
				totalFP += FP/N;
				totalTN += TN/N;
				totalFN += FN/N;
			}

			totalTP /= input.size();
			totalFP /= input.size();
			totalTN /= input.size();
			totalFN /= input.size();

			double precision = totalTP/(totalTP+totalFP);
			double recall = totalTP/(totalTP+totalFN);
			double F = 2.0*(precision*recall)/(precision+recall);

			System.out.println(alg.name+" F = "+F+" P = "+precision+" R = "+recall);
			if( out != null)
				out.println(alg.name+" F = "+F+" P = "+precision+" R = "+recall);
		}

		if( out != null) {
			out.close();
			out = null;
		}
	}

	protected void load( String fileIn , String fileTruth ) {
		input.add(UtilImageIO.loadImage(directory+fileIn,ImageFloat32.class));
		ImageFloat32 img = UtilImageIO.loadImage(directory+fileTruth,ImageFloat32.class);
		truth.add(ThresholdImageOps.threshold(img, null, 100, true));
	}

	public static class Alg {
		ThresholdText alg;
		String name;

		public Alg(ThresholdText alg, String name) {
			this.alg = alg;
			this.name = name;
		}
	}

	public static void main(String[] args) {
		EvaluateTextThresholdDIBCO app = new EvaluateTextThresholdDIBCO();

		app.addAlgorithm(FactoryThresholdAlgs.mean(),"mean");
		app.addAlgorithm(FactoryThresholdAlgs.otsu(),"otsu");
		app.addAlgorithm(FactoryThresholdAlgs.entropy(),"entropy");
		app.addAlgorithm(FactoryThresholdAlgs.localSquare(),"local square");
		app.addAlgorithm(FactoryThresholdAlgs.localGaussian(),"local gaussian");
		app.addAlgorithm(FactoryThresholdAlgs.adaptiveSauvola(),"Sauvola");

		app.evaluate();
	}

}