package tigase.kernel;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;

@Bean(name = "bean8")
public class Bean8 {

	@Inject(nullAllowed = false)
	private Bean6 bean6;

	public Bean6 getBean6() {
		return bean6;
	}

	public void setBean6(Bean6 bean6) {
		this.bean6 = bean6;
	}

}
