package boofcv.metrics.disparity;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Reading and writing PMF files used with Middlebury stereo tests
 *
 * @author Peter Abeles
 */
public class CodecPFM {
	public static void write(GrayF32 disparity , OutputStream writer ) throws IOException {
		PrintStream out = new PrintStream(writer);

		// Java is big endian so the scale factor is positive
		double scalefactor = 1.0; // WTF is this?
		out.printf("Pf\n%d %d\n%f\n",disparity.width,disparity.height,scalefactor);

		for (int y = disparity.height-1; y >= 0; y--) {
			int idx = disparity.startIndex + y*disparity.stride;
			for (int x = 0; x < disparity.width; x++) {
				float f = disparity.data[idx++];
				if( f == 0.0f )
					f = Float.POSITIVE_INFINITY;
				writer.write(Float.floatToRawIntBits(f));
			}
		}

		writer.close();
	}

	public static GrayF32 read( File file , GrayF32 disparity) {
		try {
			return read(new FileInputStream(file),disparity);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static GrayF32 read(InputStream input , GrayF32 disparity) throws IOException {
		StringBuffer buffer = new StringBuffer();
		String line;
		line = readLine(input,buffer);
		if( !line.equals("Pf"))
			throw new IOException("File does not start with Pf");

		if( disparity == null )
			disparity = new GrayF32(1,1);

		String[] words = nextWords(readLine(input,buffer));
		if( words.length != 2 )
			throw new IOException("Expected 2 words for width and height");
		disparity.reshape(Integer.parseInt(words[0]),Integer.parseInt(words[1]));
		double scaleFactor = Double.parseDouble(readLine(input,buffer));
		boolean bigEndian = scaleFactor > 0;

//		System.out.println("image size "+disparity.width+" "+disparity.height+" scale = "+scaleFactor);

		final byte[] row = new byte[disparity.width*4];
		for (int y = disparity.height-1; y >= 0; y--) {
			int idx = disparity.startIndex + y*disparity.stride;
			int amount = input.read(row);
			if( amount != row.length ) {
				throw new IOException("Didn't read as much as it should have! " + amount + " vs " + row.length);
			}

			int i = 0;
			if( bigEndian ) {
				while (i < row.length) {
					int raw = ((row[i++] & 0xFF) << 24) | ((row[i++] & 0xFF) << 16) | ((row[i++] & 0xFF) << 8) | (row[i++] & 0xFF);
					disparity.data[idx++] = Float.intBitsToFloat(raw);
				}
			} else {
				while (i < row.length) {
					int raw = (row[i++] & 0xFF) | ((row[i++] & 0xFF) << 8) | ((row[i++] & 0xFF) << 16) | ((row[i++] & 0xFF)<<24);
					disparity.data[idx++] = Float.intBitsToFloat(raw);
				}
			}
		}

		input.close();

		return disparity;
	}

	private static String readLine( InputStream input, StringBuffer buffer ) throws IOException {
		buffer.setLength(0);
		while( true ) {
			int v = input.read();
			if( v == -1 || v == '\n' )
				return buffer.toString();
			buffer.append((char)v);
		}
	}

	private static String[] nextWords( String line ) throws IOException {
		if( line.length() == 0 )
			throw new IOException("empty string");
		return line.split(" ");
	}

	public static void main(String[] args) throws IOException {
		File f = BoofSwingUtil.fileChooser("CodecPFM",null,true,".",null);
		if( f == null )
			return;
		GrayF32 disparity = read(new FileInputStream(f),null);
		float m = -Float.MAX_VALUE;
		for (int i = 0; i < disparity.totalPixels(); i++) {
			float v = disparity.data[i];
			if( Float.isInfinite(v))
				continue;
			m = Math.max(m,v);
		}
		System.out.println("Max = "+m);
		BufferedImage colorized = VisualizeImageData.disparity(disparity,null,(int)m+1,0);
		ShowImages.showWindow(colorized,"Disparity",true);
	}
}
