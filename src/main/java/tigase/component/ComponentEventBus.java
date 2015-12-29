package tigase.component;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.local.Event;
import tigase.disteventbus.local.EventHandler;
import tigase.disteventbus.local.RegistrationException;
import tigase.kernel.beans.Inject;
import tigase.server.BasicComponent;
import tigase.xml.Element;

public class ComponentEventBus implements EventBus {

	private final EventBus eventBus = EventBusFactory.getInstance();
	@Inject(nullAllowed = false)
	private BasicComponent component;

	@Override
	public void addHandler(String name, String xmlns, tigase.disteventbus.clustered.EventHandler handler) {
		eventBus.addHandler(name, xmlns, handler);
	}

	@Override
	public void addHandler(Class<? extends Event> type, EventHandler handler) {
		eventBus.addHandler(type, handler);
	}

	@Override
	public void addHandler(EventHandler handler) {
		eventBus.addHandler(handler);
	}

	@Override
	public void fire(Element event) {
		event.setAttribute("eventSource", component.getComponentId().toString());
		event.setAttribute("eventTimestamp", Long.toString(System.currentTimeMillis()));

		eventBus.fire(event);
	}

	@Override
	public void fire(Event e) {
		eventBus.fire(e);
	}

	@Override
	public void registerAll(Object consumer) throws RegistrationException {
		eventBus.registerAll(consumer);
	}

	@Override
	public void remove(Class<? extends Event> type, EventHandler handler) {
		eventBus.remove(type, handler);
	}

	@Override
	public void remove(EventHandler handler) {
		eventBus.remove(handler);
	}

	@Override
	public void removeHandler(String name, String xmlns, tigase.disteventbus.clustered.EventHandler handler) {
		eventBus.removeHandler(name, xmlns, handler);
	}

	@Override
	public void unregisterAll(Object consumer) {
		eventBus.unregisterAll(consumer);
	}

}
