package tigase.kernel;

import tigase.kernel.beans.config.ConfigField;

public class Bean6 implements Special {

	@ConfigField(desc = "Field with string value")
	private String testValue;

	public String getTestValue() {
		return testValue;
	}

}
