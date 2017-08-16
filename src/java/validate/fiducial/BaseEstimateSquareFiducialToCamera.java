package validate.fiducial;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import validate.DataSetDoesNotExist;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Estimates the location of fiducials in the input images.  Results are saved to the specified output directory.
 * The detector should be configured such that the fiducial is of size 1.  THe actual size will be read later on
 * and the translation adjusted.
 *
 * @author Peter Abeles
 */
public abstract class BaseEstimateSquareFiducialToCamera<T extends ImageBase<T>> {

	File baseDirectory;
	File outputDirectory = new File(".");

	public abstract FiducialDetector<T> createDetector( File datasetDir );

	public void initialize( File baseDirectory ) {
		this.baseDirectory = baseDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public void process( File dataSetDir ) throws IOException {

		if( !dataSetDir.exists() ) {
			throw new DataSetDoesNotExist("The data set directory doesn't exist. "+dataSetDir.getPath());
		}

		FiducialDetector<T> detector = createDetector(dataSetDir);
		FiducialCommon.Library library = FiducialCommon.parseScenario(new File(dataSetDir, "library.txt"));

		List<String> files = UtilIO.directoryList(dataSetDir.getAbsolutePath(), "png");
		if( files.size() == 0 ) {
			files = UtilIO.directoryList(dataSetDir.getAbsolutePath(), "jpg");
		}
		if( files.size() == 0 ) {
			throw new IllegalArgumentException("No images found.  paths correct?");
		}
		T image = detector.getInputType().createImage(1,1);

		CameraPinholeRadial intrinsic = FiducialCommon.parseIntrinsic(new File(dataSetDir,"intrinsic.txt"));
		detector.setLensDistortion(new LensDistortionRadialTangential(intrinsic));
		for( String path : files ) {
			BufferedImage orig = UtilImageIO.loadImage(path);
			image.reshape(orig.getWidth(),orig.getHeight());
			ConvertBufferedImage.convertFrom(orig,image,true);

			detector.detect(image);
//			System.out.println("processing "+path+"  found "+detector.totalFound());

//			if( detector.totalFound() == 0 )
//				System.out.println("no detections in "+path);

			File f = new File(path);
			String inputName = f.getName();
			File outFile = new File(outputDirectory,inputName.substring(0,inputName.length()-3)+"csv");
			PrintStream out = new PrintStream(outFile);
			out.println("# Detected fiducials inside of "+inputName);
			out.println("# 4 lines for each detection. line 1 = detected fiducial.  lines 2-4 = rigid body transform, row major");
			Se3_F64 fiducialToSensor = new Se3_F64();
			for (int i = 0; i < detector.totalFound(); i++) {
				long which = detector.getId(i);
				double fiducialWidth = library != null ? library.getWidth(which) : 1;
				detector.getFiducialToCamera(i,fiducialToSensor);


				DMatrixRMaj R = fiducialToSensor.getR();
				Vector3D_F64 T = fiducialToSensor.getT();

				// adjust translation for actual fiducial size
				T.x *= fiducialWidth;
				T.y *= fiducialWidth;
				T.z *= fiducialWidth;

				out.println(which);
				out.printf("%.15f %.15f %.15f %.15f\n",R.get(0,0),R.get(0,1),R.get(0,2),T.x);
				out.printf("%.15f %.15f %.15f %.15f\n",R.get(1,0),R.get(1,1),R.get(1,2),T.y);
				out.printf("%.15f %.15f %.15f %.15f\n",R.get(2,0),R.get(2,1),R.get(2,2),T.z);
			}
			out.close();
		}
	}

	protected static File setupOutput() {
		File outputDirectory = new File("tmp");
		if( outputDirectory.exists() ) {
			for( File f : outputDirectory.listFiles() ) {
				if( !f.delete() ) {
					throw new RuntimeException("Couldn't delete a file in tmp. "+f.getName());
				}
			}
		} else {
			outputDirectory.mkdirs();
		}
		return outputDirectory;
	}

}
