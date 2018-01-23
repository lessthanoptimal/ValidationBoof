package boofcv.metrics.threshold;

import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateTextThresholdDIBCO {
    public static String directory = "data/DIBCO/2009/";

    List<GrayF32> input = new ArrayList<GrayF32>();
    List<GrayU8> truth = new ArrayList<GrayU8>();

    List<Alg> algorithms = new ArrayList<Alg>();

    long TP,FP,TN,FN;
    double totalTP,totalFP,totalTN,totalFN;

    PrintStream out;

    public void addAlgorithm( ThresholdText alg , String name ) {
        algorithms.add( new Alg(alg,name));
    }

    public void evaluate() {
        input.clear();
        truth.clear();
        // load all the data
        for (int i = 1; i <= 5 ; i++) {
            load(String.format("H%02d.bmp",i),String.format("H%02d_truth.bmp",i));
        }
        for (int i = 1; i <= 5 ; i++) {
            load(String.format("P%02d.bmp",i),String.format("P%02d_truth.bmp",i));
        }

        GrayU8 found = new GrayU8(1,1);
        for( Alg alg : algorithms ) {

            totalTP=totalFP=totalTN=totalFN=0;

            for (int i = 0; i < input.size(); i++) {

                GrayF32 in = input.get(i);
                GrayU8 expected = truth.get(i);
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

            System.out.printf("%25s F = %7f P = %7f R = %7f\n",alg.name,F,precision,recall);
            if( out != null)
                out.printf("%25s F = %7f P = %7f R = %7f\n",alg.name,F,precision,recall);
        }

        if( out != null) {
            out.close();
            out = null;
        }
    }

    protected void load( String fileIn , String fileTruth ) {
        input.add(UtilImageIO.loadImage(directory+fileIn,GrayF32.class));
        GrayF32 img = UtilImageIO.loadImage(directory+fileTruth,GrayF32.class);
        truth.add(ThresholdImageOps.threshold(img, null, 100, true));
    }

    public void setOutputResults(PrintStream outputStream) {
        this.out = outputStream;
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

        app.addAlgorithm(FactoryThresholdAlgs.globalMean(),"mean");
        app.addAlgorithm(FactoryThresholdAlgs.globalOtsu(),"otsu");
        app.addAlgorithm(FactoryThresholdAlgs.globalEntropy(),"entropy");
        app.addAlgorithm(FactoryThresholdAlgs.localSquare(),"local square");
        app.addAlgorithm(FactoryThresholdAlgs.localGaussian(),"local gaussian");
        app.addAlgorithm(FactoryThresholdAlgs.localSauvola(),"Sauvola");
        app.addAlgorithm(FactoryThresholdAlgs.localBlockMinMax(),"Block Min-Max");

        app.evaluate();
    }
}
