package tigase.kernel;

import tigase.kernel.beans.BeanFactory;
import tigase.kernel.beans.Inject;

public class Bean5Factory implements BeanFactory<Bean5> {

	@Inject
	private Bean1 bean;

	@Override
	public Bean5 createInstance() throws KernelException {
		return new Bean5();
	}

	public Bean1 getBean() {
		return bean;
	}

	public void setBean(Bean1 bean) {
		this.bean = bean;
	}

}
