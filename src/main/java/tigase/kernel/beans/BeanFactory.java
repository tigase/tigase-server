package tigase.kernel.beans;

import tigase.kernel.KernelException;

/**
 * Interface to create factories of beans.<br/>
 * Factory is responsible to create instance of bean and inject all
 * dependencies!
 * 
 * @param <T>
 *            type of created bean.
 */
public interface BeanFactory<T> {

	/**
	 * Create instance of bean.<br/>
	 * Remember, that dependencies will not be injected to this bean. Factory
	 * must do that!
	 * 
	 * @return instancje of bean.
	 * @throws KernelException
	 *             when something goes wrong.
	 */
	T createInstance() throws KernelException;

}
