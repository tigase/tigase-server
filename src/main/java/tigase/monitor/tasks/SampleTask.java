package tigase.monitor.tasks;

import java.util.Date;

import tigase.disteventbus.EventBus;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.monitor.MonitorComponent;
import tigase.xml.Element;

@Bean(name = "sample-task")
public class SampleTask extends AbstractConfigurableTimerTask {

	@Inject
	private EventBus eventBus;

	@Override
	protected void enable() {
		super.enable();

		Element event = new Element("SampleTaskEnabled", new String[] { "xmlns" },
				new String[] { MonitorComponent.EVENTS_XMLNS });
		event.addChild(new Element("timestamp", "" + (new Date())));
		event.addChild(new Element("message", "Hello kitty!"));
		eventBus.fire(event);
	}

	@Override
	protected void run() {
		System.out.println("Sample task!");
	}
}
