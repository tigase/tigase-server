package tigase.kernel;

import tigase.TestLogger;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Bean5 implements UnregisterAware {

	private static final Logger log = TestLogger.getLogger(Bean5.class);

	@ConfigField(desc = "One field with value")
	private Long value = 15l;

	@Override
	public void beforeUnregister() {
		log.log(Level.FINE, "Destroying Bean5 class");
	}

	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
	}

}
