package tigase.disteventbus;

import java.util.Set;
import java.util.concurrent.Executor;

import tigase.disteventbus.objbus.DefaultObjectsEventsBus;
import tigase.disteventbus.objbus.Event;
import tigase.disteventbus.objbus.EventListener;
import tigase.disteventbus.objbus.ObjEventHandler;
import tigase.disteventbus.xmlbus.DefaultXMLEventsBus;
import tigase.disteventbus.xmlbus.EventName;
import tigase.xml.Element;

/**
 * Created by bmalkow on 17.12.2015.
 */
public class CombinedEventBus implements EventBus {

	private final DefaultObjectsEventsBus objectsEventsBus = new DefaultObjectsEventsBus();

	private final DefaultXMLEventsBus xmlEventsBus = new DefaultXMLEventsBus();

	@Override
	public void addHandler(String name, String xmlns, tigase.disteventbus.xmlbus.EventHandler handler) {
		xmlEventsBus.addHandler(name, xmlns, handler);
	}

	@Override
	public <H extends ObjEventHandler> void addHandler(Class<? extends Event<H>> type, H handler) {
		objectsEventsBus.addHandler(type, handler);
	}

	@Override
	public <H extends ObjEventHandler> void addHandler(Class<? extends Event<H>> type, Object source, H handler) {
		objectsEventsBus.addHandler(type, source, handler);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(Class<? extends Event<H>> type, EventListener listener) {
		objectsEventsBus.addListener(type, listener);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(Class<? extends Event<H>> type, Object source, EventListener listener) {
		objectsEventsBus.addListener(type, source, listener);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(EventListener listener) {
		objectsEventsBus.addListener(listener);
	}

	public void doFire(String name, String xmlns, Element event) {
		xmlEventsBus.doFire(name, xmlns, event);
	}

	@Override
	public void fire(Element event) {
		xmlEventsBus.fire(event);
	}

	@Override
	public void fire(Event<?> e) {
		objectsEventsBus.fire(e);
	}

	@Override
	public void fire(Event<?> e, Object source) {
		objectsEventsBus.fire(e, source);
	}

	public Set<EventName> getAllListenedEvents() {
		return xmlEventsBus.getAllListenedEvents();
	}

	public Executor getExecutor() {
		return xmlEventsBus.getExecutor();
	}

	public void setExecutor(Executor executor) {
		xmlEventsBus.setExecutor(executor);
	}

	public boolean hasHandlers(String name, String xmlns) {
		return xmlEventsBus.hasHandlers(name, xmlns);
	}

	@Override
	public void remove(Class<? extends Event<?>> type, ObjEventHandler handler) {
		objectsEventsBus.remove(type, handler);
	}

	@Override
	public void remove(Class<? extends Event<?>> type, Object source, ObjEventHandler handler) {
		objectsEventsBus.remove(type, source, handler);
	}

	@Override
	public void remove(ObjEventHandler handler) {
		objectsEventsBus.remove(handler);
	}

	@Override
	public void removeHandler(String name, String xmlns, tigase.disteventbus.xmlbus.EventHandler handler) {
		xmlEventsBus.removeHandler(name, xmlns, handler);
	}

	public void setThreadPool(int pool) {
		xmlEventsBus.setThreadPool(pool);
	}
}
