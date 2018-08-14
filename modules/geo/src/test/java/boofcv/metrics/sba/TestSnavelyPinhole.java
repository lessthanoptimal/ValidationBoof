package boofcv.metrics.sba;

/**
 * @author Peter Abeles
 */
public class TestSnavelyPinhole  extends GenericChecksBundleAdjustmentCamera
{
    public TestSnavelyPinhole() {
        super(new SnavelyPinhole());
        setParameters(new double[][]{{300,0,0},{300,1e-1,1e-2}});
    }
}