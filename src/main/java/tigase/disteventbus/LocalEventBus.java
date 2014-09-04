package tigase.disteventbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.disteventbus.component.NodeNameUtil;
import tigase.xml.Element;

public class LocalEventBus implements EventBus {

	public static class EventName {

		private final String name;

		private final String xmlns;

		EventName(String name, String xmlns) {
			super();
			this.name = name;
			this.xmlns = xmlns;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EventName other = (EventName) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (xmlns == null) {
				if (other.xmlns != null)
					return false;
			} else if (!xmlns.equals(other.xmlns))
				return false;
			return true;
		}

		public String getName() {
			return name;
		}

		public String getXmlns() {
			return xmlns;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((xmlns == null) ? 0 : xmlns.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return NodeNameUtil.createNodeName(name, xmlns);
		}

	}

	public static interface LocalEventBusListener {

		void onAddHandler(final String name, final String xmlns, final EventHandler handler);

		void onFire(final String name, final String xmlns, final Element event);

		void onRemoveHandler(final String name, final String xmlns, final EventHandler handler);

	}

	private final static String NULL_NAME = new String(new byte[] { 0 });

	/**
	 * Map<XMLNS, Map<Name, Handler>>
	 */
	private final Map<String, Map<String, List<EventHandler>>> handlers = createMainHandlersMap();

	private final List<LocalEventBusListener> internalListeners = new ArrayList<LocalEventBusListener>();

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private boolean throwingExceptionOn = true;

	/**
	 * Registers handler to receive events with given name and namespace.
	 * 
	 * @param name
	 *            name of events. Can be {@code null}, then handler will be
	 *            received all events with given {@code xmlns}.
	 * @param xmlns
	 *            namespace of events.
	 * @param handler
	 *            event handler.
	 */
	@Override
	public void addHandler(final String name, final String xmlns, final EventHandler handler) {
		final String eventName = name == null ? NULL_NAME : name;
		// TODO tutaj zarejestrować subskrypcję w zdalnych busach

		Map<String, List<EventHandler>> namesHandlers = handlers.get(xmlns);
		if (namesHandlers == null) {
			namesHandlers = createNamesHandlerMap();
			handlers.put(xmlns, namesHandlers);
		}

		List<EventHandler> handlersList = namesHandlers.get(eventName);
		if (handlersList == null) {
			handlersList = createHandlersList();
			namesHandlers.put(eventName, handlersList);
		}

		handlersList.add(handler);

		fireOnAddHandler(name, xmlns, handler);
	}

	public void addListener(LocalEventBusListener listener) {
		internalListeners.add(listener);
	}

	protected List<EventHandler> createHandlersList() {
		return new ArrayList<EventHandler>();
	}

	protected Map<String, Map<String, List<EventHandler>>> createMainHandlersMap() {
		return new ConcurrentHashMap<String, Map<String, List<EventHandler>>>();
	}

	protected Map<String, List<EventHandler>> createNamesHandlerMap() {
		return new ConcurrentHashMap<String, List<EventHandler>>();
	}

	public void doFire(final String name, final String xmlns, final Element event) {
		if (event == null) {
			throw new NullPointerException("Cannot fire null event");
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
		HashSet<EventName> result = new HashSet<LocalEventBus.EventName>();
		Iterator<Entry<String, Map<String, List<EventHandler>>>> xmlnsIt = handlers.entrySet().iterator();

		while (xmlnsIt.hasNext()) {
			Entry<String, Map<String, List<EventHandler>>> e = xmlnsIt.next();
			final String xmlns = e.getKey();

			Iterator<String> namesIt = e.getValue().keySet().iterator();
			while (namesIt.hasNext()) {
				String n = namesIt.next();
				result.add(new EventName(n == NULL_NAME ? null : n, xmlns));
			}
		}

		return result;
	}

	protected Collection<EventHandler> getHandlersList(final String name, final String xmlns) {
		final String eventName = name == null ? NULL_NAME : name;

		Map<String, List<EventHandler>> namesHandlers = handlers.get(xmlns);
		if (namesHandlers == null)
			return Collections.emptyList();

		List<EventHandler> handlersList = namesHandlers.get(eventName);
		if (handlersList == null)
			return Collections.emptyList();

		return Collections.unmodifiableCollection(handlersList);
	}

	@Override
	public void removeHandler(final String name, final String xmlns, final EventHandler handler) {
		final String eventName = name == null ? NULL_NAME : name;

		Map<String, List<EventHandler>> namesHandlers = handlers.get(xmlns);
		if (namesHandlers == null)
			return;

		List<EventHandler> handlersList = namesHandlers.get(eventName);
		if (handlersList == null)
			return;

		handlersList.remove(handler);

		if (handlersList.isEmpty()) {
			namesHandlers.remove(eventName);
		}

		if (namesHandlers.isEmpty()) {
			handlers.remove(xmlns);
			// TODO tutaj wyrejestrować subskrypcję w zdalnych busach
		}

		fireOnRemoveHandler(name, xmlns, handler);

	}

	public void removeListener(LocalEventBusListener listener) {
		internalListeners.remove(listener);
	}

}
