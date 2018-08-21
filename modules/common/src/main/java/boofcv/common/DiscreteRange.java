package boofcv.common;

/**
 * @author Peter Abeles
 */
public class DiscreteRange {
    public double min,max;
    public int count;

    public DiscreteRange(double min, double max, int count) {
        this.min = min;
        this.max = max;
        this.count = count;
    }

    public DiscreteRange() {
    }
}
