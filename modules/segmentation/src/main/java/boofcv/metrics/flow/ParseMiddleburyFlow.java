package boofcv.metrics.flow;

import boofcv.struct.image.InterleavedF32;
import com.google.common.io.LittleEndianDataInputStream;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class ParseMiddleburyFlow {

	public static final float TAG_FLOAT = 202021.25f;  // check for this when READING the file
	public static final String TAG_STRING = "PIEH";    // use this when WRITING the file

	public static InterleavedF32 parse( String fileName ) throws IOException {
		LittleEndianDataInputStream stream = new LittleEndianDataInputStream (new FileInputStream(fileName));

		float tag = stream.readFloat();
		int width = stream.readInt();
		int height = stream.readInt();

		if( tag != TAG_FLOAT )
			throw new RuntimeException("Failed float tag test");

		InterleavedF32 flow = new InterleavedF32(width,height,2);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float fx = stream.readFloat();
				float fy = stream.readFloat();

				if( Math.abs(fx) > 1e9f || Math.abs(fy) > 1e9f ) {
					fx = fy = Float.NaN;
				}

				flow.setBand(x,y,0,fx);
				flow.setBand(x,y,1,fy);
			}
		}

		return flow;
	}
}
