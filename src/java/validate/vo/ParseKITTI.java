package validate.vo;

import boofcv.io.image.UtilImageIO;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ParseKITTI {
	String directoryImages;

	// camera projection matrices
	private DMatrixRMaj leftP = new DMatrixRMaj(3,4);
	private DMatrixRMaj rightP = new DMatrixRMaj(3,4);

	// left camera pose information
	private List<Se3_F64> poseLeft = new ArrayList<Se3_F64>();

	// camera images
	private BufferedImage leftImage;
	private BufferedImage rightImage;
	private Se3_F64 leftToWorld;

	public ParseKITTI(String directoryImages) {
		this.directoryImages = directoryImages;
	}

	public void loadCalibration( String fileName ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));

			double line[] = readLine(reader,1);
			System.arraycopy(line,0,leftP.data,0,line.length);
			line = readLine(reader,1);
			System.arraycopy(line,0,rightP.data,0,line.length);

			reader.close();

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void loadTruth( String fileName ) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));

			while( true ) {
				double line[] = readLine(reader,0);
				if( line == null )
					break;

				Se3_F64 pose = new Se3_F64();
				pose.getR().set(0,0,line[0]);
				pose.getR().set(0,1,line[1]);
				pose.getR().set(0,2,line[2]);
				pose.T.x = line[3];
				pose.getR().set(1,0,line[4]);
				pose.getR().set(1,1,line[5]);
				pose.getR().set(1,2,line[6]);
				pose.T.y = line[7];
				pose.getR().set(2,0,line[8]);
				pose.getR().set(2,1,line[9]);
				pose.getR().set(2,2,line[10]);
				pose.T.z = line[11];


				poseLeft.add(pose);
			}

		} catch (FileNotFoundException e) {
			// not all datasets have ground truth
//			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean loadFrame( int frameNumber ) {
		String imageName = String.format("%06d.png",frameNumber);

		leftImage = UtilImageIO.loadImage(directoryImages+"/image_0/"+imageName);
		if( leftImage == null )
			return false;
		rightImage = UtilImageIO.loadImage(directoryImages+"/image_1/"+imageName);


		if( poseLeft.size() > frameNumber ) {
			leftToWorld = poseLeft.get(frameNumber);
		}

		return true;
	}

	public static double[] readLine( BufferedReader reader , int skip ) throws IOException {
		String line = reader.readLine();
		if( line == null )
			return null;

		String words[] = line.split("\\s") ;
		double ret[] = new double[ words.length-skip ];

		for( int i = 0; i < ret.length; i++ ) {
			ret[i] = Double.parseDouble(words[i+skip]);
		}
		return ret;
	}

	public DMatrixRMaj getLeftProjection() {
		return leftP;
	}

	public DMatrixRMaj getRightProjection() {
		return rightP;
	}

	public BufferedImage getLeftImage() {
		return leftImage;
	}

	public BufferedImage getRightImage() {
		return rightImage;
	}

	public Se3_F64 getLeftToWorld() {
		return leftToWorld;
	}
}
