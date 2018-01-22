package boofcv.metrics.vo;

import boofcv.io.image.UtilImageIO;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Parse for Leuven07 stereo visual odometry dataset
 *
 * @author Peter Abeles
 */
public class ParseLeuven07 {

	// data directory
	private String homeDirectory;

	// camera calibration information
	private DMatrixRMaj leftK = new DMatrixRMaj(3,3);
	private DMatrixRMaj rightK = new DMatrixRMaj(3,3);
	private Se3_F64 leftToWorld = new Se3_F64();
	private Se3_F64 rightToWorld = new Se3_F64();

	// camera images
	private BufferedImage leftImage;
	private BufferedImage rightImage;

	/**
	 * Specifies data location
	 *
	 * @param homeDirectory Location of the dataset
	 */
	public ParseLeuven07( String homeDirectory ) {
		this.homeDirectory = homeDirectory;
	}

	/**
	 * Loads all the information for a particular image in the sequence.
	 *
	 * @param frame Which frame is to be loaded
	 * @return true if the frame exists
	 */
	public boolean loadFrame( int frame ) {
		String imageNameLeft = String.format("undistorted%05d.png",frame*2);
		String imageNameRight = String.format("undistorted%05d.png",frame*2+1);
		leftImage = UtilImageIO.loadImage(homeDirectory+"/left/"+imageNameLeft);
		rightImage = UtilImageIO.loadImage(homeDirectory+"/right/"+imageNameRight);

		if( leftImage == null )
			return false;

		String calibrationLeft = String.format("maps/camera.%05d",frame*2);
		String calibrationRight = String.format("maps/camera.%05d",frame*2+1);

		loadCalibration(homeDirectory + "/left/" + calibrationLeft, leftK, leftToWorld);
		loadCalibration(homeDirectory + "/right/" + calibrationRight, rightK, rightToWorld);

		return true;
	}

	/**
	 * Reads in camera parameters from log file
	 *
	 * @param fileName Which file should be read
	 * @param K intrinsic camera calibration matrix
	 * @param motion location of the camera
	 */
	private void loadCalibration(String fileName, DMatrixRMaj K, Se3_F64 motion) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));

			// calibration matrix
			double line[] = readNumbers(reader);
			K.set(0,0,line[0]); K.set(0,1,line[1]); K.set(0,2,line[2]);
			line = readNumbers(reader);
			K.set(1,0,line[0]); K.set(1,1,line[1]); K.set(1,2,line[2]);
			line = readNumbers(reader);
			K.set(2,0,line[0]); K.set(2,1,line[1]); K.set(2,2,line[2]);

			reader.readLine(); // empty line

			// skip distortion parameters since they are always zero
			readNumbers(reader);

			reader.readLine(); // empty line

			// rotation matrix
			DMatrixRMaj R = motion.getR();
			line = readNumbers(reader);
			R.set(0,0,line[0]); R.set(0,1,line[1]); R.set(0,2,line[2]);
			line = readNumbers(reader);
			R.set(1,0,line[0]); R.set(1,1,line[1]); R.set(1,2,line[2]);
			line = readNumbers(reader);
			R.set(2,0,line[0]); R.set(2,1,line[1]); R.set(2,2,line[2]);

			reader.readLine(); // empty line

			// translation vector
			line = readNumbers(reader);
			motion.getT().set(line[0],line[1],line[2]);

			// convert into meters
			GeometryMath_F64.scale(motion.getT(),0.49603);

			reader.close();

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static double[] readNumbers( BufferedReader reader ) throws IOException {
		String line = reader.readLine();

		String words[] = line.split("\\s") ;
		double ret[] = new double[ words.length ];

		for( int i = 0; i < ret.length; i++ ) {
			ret[i] = Double.parseDouble(words[i]);
		}
		return ret;
	}

	public BufferedImage getLeftImage() {
		return leftImage;
	}

	public DMatrixRMaj getLeftK() {
		return leftK;
	}

	public BufferedImage getRightImage() {
		return rightImage;
	}

	public DMatrixRMaj getRightK() {
		return rightK;
	}

	public Se3_F64 getLeftToWorld() {
		return leftToWorld;
	}

	public Se3_F64 getRightToWorld() {
		return rightToWorld;
	}
}
