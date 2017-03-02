package tigase.monitor.tasks;

import tigase.eventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.xml.Element;

import java.util.Date;

@Bean(name = "sample-task", active = true)
public class SampleTask extends AbstractConfigurableTimerTask implements Initializable {

	@Inject
	private EventBus eventBus;
	private String message = "<->";

	@Override
	protected void enable() {
		super.enable();

		Element event = new Element("tigase.monitor.tasks.SampleTaskEnabled");
		event.addChild(new Element("timestamp", "" + (new Date())));
		event.addChild(new Element("message", this.message));
		this.message = "<->";
		eventBus.fire(event);

		setEnabled(false);
	}

	@Override
	public Form getCurrentConfiguration() {
		Form x = super.getCurrentConfiguration();
		x.addField(Field.fieldTextSingle("message", "", "Event message"));
		return x;
	}

	@Override
	public void initialize() {
		eventBus.registerEvent("tigase.monitor.tasks.SampleTaskEnabled", "Sample task", false);
	}

	@Override
	protected void run() {
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field m = form.get("message");
		if (m == null) {
			this.message = "<not found>";
		} else {
			this.message = m.getValue();
		}

		super.setNewConfiguration(form);
	}
}
