package validate.fiducial.benchmark;

import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.struct.image.GrayU8;
import validate.FactoryObject;
import validate.FactoryObjectAbstract;
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


	public void process( File benchmarkDir , File outputDir ) throws IOException {

		if( !outputDir.exists() ) {
			outputDir.mkdirs();
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
				estimator.process(scenarioOutput);
			}
		}
	}

	public static void main(String[] args) throws IOException {

		FactoryObject factory = new FactoryObjectAbstract() {
			@Override
			public Object newInstance() {
				return FactoryFiducial.squareImage(new ConfigFiducialImage(),
						ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 20),
						GrayU8.class);
			}
		};

		GenerateBenchmarkResults app = new GenerateBenchmarkResults(factory);

//		app.process(new File("data/fiducials/image"),"./output");
	}
}
