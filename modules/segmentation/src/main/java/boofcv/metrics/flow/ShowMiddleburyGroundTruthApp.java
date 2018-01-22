package boofcv.metrics.flow;

import boofcv.gui.feature.VisualizeOpticalFlow;
import boofcv.gui.image.ShowImages;
import boofcv.struct.flow.ImageFlow;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class ShowMiddleburyGroundTruthApp {
	public static void main(String[] args) throws IOException {

//		String which = "Grove2";
		String which = "Dimetrodon";

		String fileName = "data/denseflow/other-gt-flow/"+which+"/flow10.flo";

		ImageFlow flow = ParseMiddleburyFlow.parse(fileName);

		BufferedImage visualized = new BufferedImage(flow.width,flow.height,BufferedImage.TYPE_INT_BGR);
		VisualizeOpticalFlow.colorized(flow,5,visualized);

		ShowImages.showWindow(visualized,"Truth");
	}
}
