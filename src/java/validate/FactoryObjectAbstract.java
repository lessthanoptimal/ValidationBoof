package validate;

import java.io.File;

/**
 * @author Peter Abeles
 */
public abstract class FactoryObjectAbstract<T> implements FactoryObject<T> {
	@Override
	public void configure(File file) {}

}
