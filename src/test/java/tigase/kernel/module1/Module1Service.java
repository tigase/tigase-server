package tigase.kernel.module1;

import tigase.kernel.Bean1;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;

public class Module1Service implements Initializable {

	@Inject(nullAllowed = false)
	private Bean1 bean1;

	@Inject
	private Bean11 bean11;

	public Bean1 getBean1() {
		return bean1;
	}

	public Bean11 getBean11() {
		return bean11;
	}

	@Override
	public void initialize() {
		System.out.println("Service1 STARTED!!! WOW!");
	}

	public void setBean1(Bean1 bean1) {
		System.out.println("????????::::" + bean1);
		this.bean1 = bean1;
	}

	public void setBean11(Bean11 bean11) {
		this.bean11 = bean11;
	}

}
