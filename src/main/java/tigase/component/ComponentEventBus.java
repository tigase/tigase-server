package tigase.component;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.EventHandler;
import tigase.kernel.beans.Inject;
import tigase.server.BasicComponent;
import tigase.xml.Element;

public class ComponentEventBus implements EventBus {

	@Inject(nullAllowed = false)
	private BasicComponent component;

	private final EventBus eventBus = EventBusFactory.getInstance();

	@Override
	public void addHandler(String name, String xmlns, EventHandler handler) {
		eventBus.addHandler(name, xmlns, handler);
	}

	@Override
	public void fire(Element event) {
		event.setAttribute("eventSource", component.getComponentId().toString());
		event.setAttribute("eventTimestamp", Long.toString(System.currentTimeMillis()));

		eventBus.fire(event);
	}

	@Override
	public void removeHandler(String name, String xmlns, EventHandler handler) {
		eventBus.removeHandler(name, xmlns, handler);
	}

}
