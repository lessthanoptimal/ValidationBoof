package boofcv.common;

/**
 * If this exception is thrown then the regression will be skipped and no error reported
 *
 * @author Peter Abeles
 */
public class ImageTypeNotSupportedException extends RuntimeException {
    public ImageTypeNotSupportedException() {
    }

    public ImageTypeNotSupportedException(String message) {
        super(message);
    }
}
