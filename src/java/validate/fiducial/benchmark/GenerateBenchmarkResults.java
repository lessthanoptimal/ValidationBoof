package validate.fiducial.benchmark;

import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.ImageUInt8;
import validate.FactoryObject;
import validate.fiducial.EstimateImageFiducialToCamera;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class GenerateBenchmarkResults {

	FactoryObject factoryDetector;

	public GenerateBenchmarkResults(FactoryObject factoryDectector ) {
		this.factoryDetector = factoryDectector;
	}

	public void process( File benchmarkDir , String outputDir ) throws IOException {

		if( !new File(outputDir).exists() ) {
			new File(outputDir).mkdirs();
		}

		PrintStream out = new PrintStream(new File(outputDir,"libToStandard.txt"));
		out.println("1 0 0 0");
		out.println("0 1 0 0");
		out.println("0 0 1 0");
		out.close();

		for( File f : benchmarkDir.listFiles() ) {
			if( f.isDirectory() ) {
				String scenarioName = f.getName();

				File scenarioOutput = new File(outputDir,scenarioName);

				if( !scenarioOutput.exists() ) {
					scenarioOutput.mkdirs();
				}

				EstimateImageFiducialToCamera estimator = new EstimateImageFiducialToCamera(factoryDetector);

				estimator.setOutputDirectory(scenarioOutput);
				estimator.initialize(benchmarkDir);
				estimator.process(scenarioName);
			}
		}
	}

	public static void main(String[] args) throws IOException {

		FactoryObject factory = new FactoryObject() {
			@Override
			public Object newInstance() {
				return FactoryFiducial.squareImageRobust(new ConfigFiducialImage(1), 20, ImageUInt8.class);
			}
		};

		GenerateBenchmarkResults app = new GenerateBenchmarkResults(factory);

		app.process(new File("data/fiducials/image"),"./output");
	}
}
