package tigase.component;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.objbus.Event;
import tigase.disteventbus.objbus.EventListener;
import tigase.disteventbus.objbus.ObjEventHandler;
import tigase.disteventbus.xmlbus.EventHandler;
import tigase.kernel.beans.Inject;
import tigase.server.BasicComponent;
import tigase.xml.Element;

public class ComponentEventBus implements EventBus {

	private final EventBus eventBus = EventBusFactory.getInstance();
	@Inject(nullAllowed = false)
	private BasicComponent component;

	@Override
	public void addHandler(String name, String xmlns, EventHandler handler) {
		eventBus.addHandler(name, xmlns, handler);
	}

	@Override
	public <H extends ObjEventHandler> void addHandler(Class<? extends Event<H>> type, H handler) {
		eventBus.addHandler(type, handler);
	}

	@Override
	public <H extends ObjEventHandler> void addHandler(Class<? extends Event<H>> type, Object source, H handler) {
		eventBus.addHandler(type, source, handler);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(Class<? extends Event<H>> type, EventListener listener) {
		eventBus.addListener(type, listener);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(Class<? extends Event<H>> type, Object source, EventListener listener) {
		eventBus.addListener(type, source, listener);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(EventListener listener) {
		eventBus.addListener(listener);
	}

	@Override
	public void fire(Element event) {
		event.setAttribute("eventSource", component.getComponentId().toString());
		event.setAttribute("eventTimestamp", Long.toString(System.currentTimeMillis()));

		eventBus.fire(event);
	}

	@Override
	public void fire(Event<?> e) {
		eventBus.fire(e);
	}

	@Override
	public void fire(Event<?> e, Object source) {
		eventBus.fire(e, source);
	}

	@Override
	public void remove(Class<? extends Event<?>> type, ObjEventHandler handler) {
		eventBus.remove(type, handler);
	}

	@Override
	public void remove(Class<? extends Event<?>> type, Object source, ObjEventHandler handler) {
		eventBus.remove(type, source, handler);
	}

	@Override
	public void remove(ObjEventHandler handler) {
		eventBus.remove(handler);
	}

	@Override
	public void removeHandler(String name, String xmlns, EventHandler handler) {
		eventBus.removeHandler(name, xmlns, handler);
	}

}
