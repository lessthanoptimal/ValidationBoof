package boofcv.metrics;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.common.FactoryObject;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Estimates the location of fiducials in the input images. Results are saved to the specified output directory.
 * The detector should be configured such that the fiducial is of size 1. THe actual size will be read later on
 * and the translation adjusted.
 *
 * @author Peter Abeles
 */
public class EstimateImageFiducialToCamera<T extends ImageGray<T>> extends BaseEstimateSquareFiducialToCamera<T> {

	FactoryObject<SquareImage_to_FiducialDetector<T>> factory;

	public EstimateImageFiducialToCamera(FactoryObject<SquareImage_to_FiducialDetector<T>> factory) {
		this.factory = factory;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDirectory) {
		FiducialCommon.Library library = FiducialCommon.parseScenario(new File(datasetDirectory, "library.txt"));

		SquareImage_to_FiducialDetector<T> detectorImage = factory.newInstance();

		for( int i = 0; i < library.getAllNames().size(); i++ ) {
			String name = library.getAllNames().get(i);
			File f = new File(baseDirectory,name);
			BufferedImage image = UtilImageIO.loadImage(f.getAbsolutePath());
			if( image == null )
				throw new RuntimeException("Can't load "+name);
			T input = (T)detectorImage.getInputType().createImage(image.getWidth(),image.getHeight());
			ConvertBufferedImage.convertFrom(image,input,true);

			detectorImage.addPatternImage(input, 100, 1.0);
		}

		return detectorImage;
	}

	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		FactoryObject factory = new FactoryObject() {
			@Override
			public void configure(File file) {}

			@Override
			public Object newInstance() {
				return FactoryFiducial.squareImage(
						new ConfigFiducialImage(),
						ConfigThreshold.local(ThresholdType.LOCAL_MEAN,20), GrayU8.class);
			}
		};

		EstimateImageFiducialToCamera app = new EstimateImageFiducialToCamera(factory);
		app.initialize(new File("data/fiducials/image"));
		app.setOutputDirectory(outputDirectory);

//		app.process("static_front_close");
	}

}
