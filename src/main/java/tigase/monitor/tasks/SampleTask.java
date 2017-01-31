package tigase.monitor.tasks;

import java.util.Date;

import tigase.disteventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.monitor.MonitorComponent;
import tigase.xml.Element;

@Bean(name = "sample-task")
public class SampleTask extends AbstractConfigurableTimerTask {

	@Inject
	private EventBus eventBus;
	private String message = "<->";

	@Override
	protected void enable() {
		super.enable();

		Element event = new Element("SampleTaskEnabled", new String[] { "xmlns" },
				new String[] { MonitorComponent.EVENTS_XMLNS });
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
