package tigase.kernel;

import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

public class Bean5 implements UnregisterAware {

	@ConfigField(desc = "One field with value")
	private Long value = 15l;

	@Override
	public void beforeUnregister() {
		System.out.println("Destroying Bean5 class");
	}

	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
	}

}
