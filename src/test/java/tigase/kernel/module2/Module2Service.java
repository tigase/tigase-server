package tigase.kernel.module2;

import tigase.kernel.Bean1;
import tigase.kernel.beans.Inject;

public class Module2Service {

	@Inject(nullAllowed = false)
	private Bean1 bean1;

	public Bean1 getBean1() {
		return bean1;
	}

	public void setBean1(Bean1 bean1) {
		this.bean1 = bean1;
	}

}
