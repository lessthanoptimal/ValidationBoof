package validate.trackrect;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class GenerateDetectionsTldData<T extends ImageBase> {

	public static String[]videos = new String[]{"01_david","02_jumping","03_pedestrian1","04_pedestrian2","05_pedestrian3",
			"06_car","07_motocross","08_volkswagen","09_carchase","10_panda"};

	T input;


	public GenerateDetectionsTldData(ImageType<T> type) {
		input = type.createImage(1,1);
	}

	public void evaluate( String dataName , String outputName , TrackerObjectQuad<T> tracker ) {
		System.out.println("Processing "+dataName);

		String path = "data/track_rect/TLD/"+dataName;
		Quadrilateral_F64 initial = new Quadrilateral_F64();
		Rectangle2D_F64 rect = UtilTldData.parseRectangle(path + "/init.txt");
		UtilPolygons2D_F64.convert(rect, initial);
		Quadrilateral_F64 found = new Quadrilateral_F64();
		Rectangle2D_F64 bounding = new Rectangle2D_F64();

		PrintStream out;

		try {
			out = new PrintStream(outputName+"_"+dataName+".txt");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		String imageType = new File(path+"/00001.jpg").exists() ? "jpg" : "png";

		int imageNum = 0;
		while( true ) {
			String imageName = String.format("%s/%05d.%s",path,imageNum+1,imageType);
			BufferedImage image = UtilImageIO.loadImage(imageName);
			if( image == null )
				break;

			input.reshape(image.getWidth(),image.getHeight());
			ConvertBufferedImage.convertFrom(image,input,true);

			boolean detected;

			if( imageNum == 0 ) {
				detected = tracker.initialize(input,initial);
			} else {
				detected = tracker.process(input,found);
			}

			if( !detected ) {
				System.out.print("-");
				out.println("nan,nan,nan,nan");
			} else {
				UtilPolygons2D_F64.bounding(found,bounding);
				System.out.print("+");
				out.printf("%f,%f,%f,%f\n",bounding.p0.x,bounding.p0.y,bounding.p1.x,bounding.p1.y);
			}

			imageNum++;
			if( imageNum % 50 == 0 )
				System.out.println();
		}
		System.out.println();
		out.close();
	}

	public static <Input extends ImageBase>
	void evaluate( String trackerName ,
				   TrackerObjectQuad<Input> tracker , ImageType<Input> imageType ) {

		GenerateDetectionsTldData<Input> generator = new GenerateDetectionsTldData(imageType);

		for( String dataName : videos ) {
			try {
				generator.evaluate(dataName,trackerName,tracker);
			} catch( RuntimeException e ) {
				System.out.println("Skipping video "+dataName);
			}
		}

//		generator.evaluate("01_david",trackerName,tracker);
//		generator.evaluate("02_jumping",trackerName,tracker);
//		generator.evaluate("03_pedestrian1",trackerName,tracker);
//		generator.evaluate("04_pedestrian2",trackerName,tracker);
//		generator.evaluate("05_pedestrian3",trackerName,tracker);
//		generator.evaluate("06_car",trackerName,tracker);
//		generator.evaluate("07_motocross",trackerName,tracker);
//		generator.evaluate("08_volkswagen",trackerName,tracker);
//		generator.evaluate("09_carchase",trackerName,tracker);
//		generator.evaluate("10_panda",trackerName,tracker);
	}

	public static void main(String[] args) {
		FactoryEvaluationTrackerObjectQuad.Info info =
				FactoryEvaluationTrackerObjectQuad.sparseFlow(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.meanShiftLikelihoodHist(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.circulant(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.tld(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.meanShiftComaniciuNoScale(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.meanShiftComaniciuScale(ImageDataType.F32);
//				FactoryEvaluationTrackerObjectQuad.meanShiftLikelihoodHist(ImageDataType.U8);


		evaluate(info.name,info.tracker,info.imageType);

		System.out.println("DONE!");
	}
}
