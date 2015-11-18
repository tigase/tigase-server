package tigase.disteventbus.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventHandler;
import tigase.xml.Element;

public class LocalEventBus implements EventBus {

	public static final String EVENTBUS_INTERNAL_EVENTS_XMLNS = "tigase:eventbus:internal:events:0";
	public static final String HANDLER_ADDED_EVENT_NAME = "HandlerAdded";
	public static final String HANDLER_REMOVED_EVENT_NAME = "HandlerRemoved";
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	private final EventsNameMap<EventHandler> handlers;
	private Executor executor;

	public LocalEventBus() {
		setThreadPool(4);
		handlers = createHandlersMap();
	}

	public Executor getExecutor() {
		return executor;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void addHandler(final String name, final String xmlns, final EventHandler handler) {
		if (name != null && xmlns == null)
			throw new RuntimeException(
					"Illegal handler registration. If name is specified, then xmlns must also be specified.");
		this.handlers.put(name, xmlns, handler);
		fireOnAddHandler(name, xmlns, handler);
	}

	protected EventsNameMap<EventHandler> createHandlersMap() {
		return new EventsNameMap<>();
	}

	public void doFire(final String name, final String xmlns, final Element event) {
		if (event == null) {
			throw new NullPointerException("Cannot fire null event");
		}

		final ArrayList<EventHandler> handlers = new ArrayList<>();
		synchronized (this.handlers) {
			handlers.addAll(getHandlersList(name, xmlns));
			handlers.addAll(getHandlersList(null, xmlns));
			handlers.addAll(getHandlersList(null, null));
		}
		doFireThreadPerHandler(name, xmlns, event, handlers);
	}

	private void doFireThreadPerHandler(final String name, final String xmlns, final Element event,
			ArrayList<EventHandler> handlersList) {
		for (EventHandler eventHandler : handlersList) {
			Runnable task = () -> {
				try {
					eventHandler.onEvent(name, xmlns, event);
				} catch (Throwable e) {
					if (log.isLoggable(Level.WARNING))
						log.log(Level.WARNING, "Problem during handling event name=" + name + ", xmlns=" + xmlns
								+ " in handler " + eventHandler, e);
				}
			};
			executor.execute(task);
		}
	}

	private void doFireThreadPerEvent(final String name, final String xmlns, final Element event,
			ArrayList<EventHandler> handlersList) {
		Runnable task = () -> {
			for (EventHandler eventHandler : handlersList) {
				try {
					eventHandler.onEvent(name, xmlns, event);
				} catch (Throwable e) {
					if (log.isLoggable(Level.WARNING))
						log.log(Level.WARNING, "Problem during handling event name=" + name + ", xmlns=" + xmlns
								+ " in handler " + eventHandler, e);
				}
			}
		};
		executor.execute(task);
	}

	@Override
	public void fire(final Element event) {
		final String name = event.getName();
		final String xmlns = event.getXMLNS();

		doFire(name, xmlns, event);
	}

	private void fireOnAddHandler(final String name, final String xmlns, final EventHandler handler) {
		Element event = new Element(HANDLER_ADDED_EVENT_NAME);
		event.setAttribute("local", "true");
		event.setXMLNS(EVENTBUS_INTERNAL_EVENTS_XMLNS);

		event.addChild(new Element("name", name));
		event.addChild(new Element("xmlns", xmlns));

		fire(event);
	}

	private void fireOnRemoveHandler(final String name, final String xmlns, final EventHandler handler) {
		Element event = new Element(HANDLER_REMOVED_EVENT_NAME);
		event.setAttribute("local", "true");
		event.setXMLNS(EVENTBUS_INTERNAL_EVENTS_XMLNS);

		event.addChild(new Element("name", name));
		event.addChild(new Element("xmlns", xmlns));

		fire(event);
	}

	public Set<EventName> getAllListenedEvents() {
		return handlers.getAllListenedEvents();
	}

	protected Collection<EventHandler> getHandlersList(final String name, final String xmlns) {
		return handlers.get(name, xmlns);
	}

	public boolean hasHandlers(String name, String xmlns) {
		return handlers.hasData(name, xmlns);
	}

	@Override
	public void removeHandler(final String name, final String xmlns, final EventHandler handler) {
		handlers.delete(name, xmlns, handler);
		fireOnRemoveHandler(name, xmlns, handler);
	}

	public void setThreadPool(int pool) {
		this.executor = Executors.newFixedThreadPool(pool);
	}

}
