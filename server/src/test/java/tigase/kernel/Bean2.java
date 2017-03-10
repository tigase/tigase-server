package tigase.kernel;

import tigase.kernel.beans.Inject;

public class Bean2 {

	@Inject
	private Bean3 bean3;

	@Inject(bean = "bean4_1", nullAllowed = true)
	private Bean4 bean4;

	public Bean3 getBean3() {
		return bean3;
	}

	public Bean4 getBean4() {
		return bean4;
	}

	public void setBean3(Bean3 bean3) {
		this.bean3 = bean3;
	}

	public void setBean4(Bean4 bean4) {
		this.bean4 = bean4;
	}

}
