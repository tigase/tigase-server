package tigase.kernel.beans;

import tigase.kernel.KernelException;

public interface BeanFactory<T> {

	T createInstance() throws KernelException;

}
