package tigase.monitor;

import tigase.form.Form;

public interface ConfigurableTask {

	Form getCurrentConfiguration();

	void setNewConfiguration(Form form);

}
