package validate;

/**
 * @author Peter Abeles
 */
public class DataSetDoesNotExist extends RuntimeException {
	public DataSetDoesNotExist(String message) {
		super(message);
	}
}
