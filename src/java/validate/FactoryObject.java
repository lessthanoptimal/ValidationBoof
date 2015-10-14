package validate;

import java.io.File;

/**
 * @author Peter Abeles
 */
public interface FactoryObject<T> {

	public void configure( File file );

	T newInstance();
}
