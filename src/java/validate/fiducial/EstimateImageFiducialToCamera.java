package validate.fiducial;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import validate.FactoryObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Estimates the location of fiducials in the input images.  Results are saved to the specified output directory.
 * The detector should be configured such that the fiducial is of size 1.  THe actual size will be read later on
 * and the translation adjusted.
 *
 * @author Peter Abeles
 */
public class EstimateImageFiducialToCamera<T extends ImageSingleBand> extends BaseEstimateSquareFiducialToCamera<T> {

	FactoryObject<SquareImage_to_FiducialDetector<T>> factory;

	public EstimateImageFiducialToCamera(FactoryObject<SquareImage_to_FiducialDetector<T>> factory) {
		this.factory = factory;
	}

	@Override
	public FiducialDetector<T> createDetector(File datasetDirectory) {
		FiducialCommon.Library library = FiducialCommon.parseScenario(new File(datasetDirectory, "library.txt"));

		SquareImage_to_FiducialDetector<T> detectorImage = factory.newInstance();

		List<String> loaded = new ArrayList<String>();
		for( String name : library.getAllNames() ) {
			if( loaded.contains(name))
				continue;
			else
				loaded.add(name);
			File f = new File(baseDirectory,name);
			BufferedImage image = UtilImageIO.loadImage(f.getAbsolutePath());
			if( image == null )
				throw new RuntimeException("Can't load "+name);
			T input = (T)detectorImage.getInputType().createImage(image.getWidth(),image.getHeight());
			ConvertBufferedImage.convertFrom(image,input,true);

			detectorImage.addTarget(input, 100);
		}

		return detectorImage;
	}

	public static void main(String[] args) throws IOException {

		File outputDirectory = setupOutput();

		FactoryObject factory = new FactoryObject() {
			@Override
			public Object newInstance() {
				return FactoryFiducial.squareImageRobust(new ConfigFiducialImage(1), 20, ImageUInt8.class);
			}
		};

		EstimateImageFiducialToCamera app = new EstimateImageFiducialToCamera(factory);
		app.initialize(new File("data/fiducials/image"));
		app.setOutputDirectory(outputDirectory);

		app.process("static_front_close");
	}

}
