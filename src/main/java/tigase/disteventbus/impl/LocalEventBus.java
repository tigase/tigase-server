package tigase.disteventbus.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventHandler;
import tigase.disteventbus.FireEventException;
import tigase.xml.Element;

public class LocalEventBus implements EventBus {

	public static interface LocalEventBusListener {

		void onAddHandler(final String name, final String xmlns, final EventHandler handler);

		void onFire(final String name, final String xmlns, final Element event);

		void onRemoveHandler(final String name, final String xmlns, final EventHandler handler);

	}

	private final EventsNameMap<EventHandler> handlers;

	private final Collection<LocalEventBusListener> internalListeners = new HashSet<LocalEventBusListener>();

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private boolean throwingExceptionOn = true;

	public LocalEventBus() {
		handlers = createHandlersMap();
	}

	@Override
	public void addHandler(final String name, final String xmlns, final EventHandler handler) {
		this.handlers.put(name, xmlns, handler);
		fireOnAddHandler(name, xmlns, handler);
	}

	public void addListener(LocalEventBusListener listener) {
		if (!internalListeners.contains(listener)) {
			internalListeners.add(listener);
		}
	}

	protected EventsNameMap<EventHandler> createHandlersMap() {
		return new EventsNameMap<EventHandler>();
	}

	public void doFire(final String name, final String xmlns, final Element event) {
		if (event == null) {
			throw new NullPointerException("Cannot fire null event");
		}

		if (xmlns == null) {
			throw new NullPointerException("Cannot fire event with null XMLNS");
		}

		final ArrayList<EventHandler> handlers = new ArrayList<EventHandler>();
		synchronized (this.handlers) {
			handlers.addAll(getHandlersList(name, xmlns));
			handlers.addAll(getHandlersList(null, xmlns));
		}
		doFire(name, xmlns, event, handlers);

	}

	public void doFire(final String name, final String xmlns, final Element event, ArrayList<EventHandler> handlersList) {
		final Set<Throwable> causes = new HashSet<Throwable>();

		for (EventHandler eventHandler : handlersList) {
			try {
				eventHandler.onEvent(name, xmlns, event);
			} catch (Throwable e) {
				if (log.isLoggable(Level.WARNING))
					log.log(Level.WARNING, "", e);
				causes.add(e);
			}

		}

		if (!causes.isEmpty()) {
			if (throwingExceptionOn)
				throw new FireEventException(causes);
		}

	}

	@Override
	public void fire(final Element event) {
		final String name = event.getName();
		final String xmlns = event.getXMLNS();

		try {
			doFire(name, xmlns, event);
		} finally {
			fireOnFire(name, xmlns, event);
		}
	}

	private void fireOnAddHandler(final String name, final String xmlns, final EventHandler handler) {
		for (LocalEventBusListener listener : internalListeners) {
			listener.onAddHandler(name, xmlns, handler);
		}
	}

	private void fireOnFire(final String name, final String xmlns, final Element event) {
		for (LocalEventBusListener listener : internalListeners) {
			listener.onFire(name, xmlns, event);
		}
	}

	private void fireOnRemoveHandler(final String name, final String xmlns, final EventHandler handler) {
		for (LocalEventBusListener listener : internalListeners) {
			listener.onRemoveHandler(name, xmlns, handler);
		}
	}

	public Set<EventName> getAllListenedEvents() {
		Set<EventName> result = handlers.getAllListenedEvents();
		return result;
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

	public void removeListener(LocalEventBusListener listener) {
		internalListeners.remove(listener);
	}

}
