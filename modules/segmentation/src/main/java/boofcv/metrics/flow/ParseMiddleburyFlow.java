package boofcv.metrics.flow;

import boofcv.struct.flow.ImageFlow;
import com.google.common.io.LittleEndianDataInputStream;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class ParseMiddleburyFlow {

	public static final float TAG_FLOAT = 202021.25f;  // check for this when READING the file
	public static final String TAG_STRING = "PIEH";    // use this when WRITING the file

	public static ImageFlow parse( String fileName ) throws IOException {
		LittleEndianDataInputStream stream = new LittleEndianDataInputStream (new FileInputStream(fileName));

		float tag = stream.readFloat();
		int width = stream.readInt();
		int height = stream.readInt();

		if( tag != TAG_FLOAT )
			throw new RuntimeException("Failed float tag test");

		ImageFlow flow = new ImageFlow(width,height);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				ImageFlow.D f = flow.get(x,y);

				f.x = stream.readFloat();
				f.y = stream.readFloat();

				if( Math.abs(f.x) > 1e9f || Math.abs(f.y) > 1e9f ) {
					f.x = f.y = Float.NaN;
				}
			}
		}

		return flow;
	}
}
