package tigase.eventbus;

import java.util.*;
import java.util.concurrent.Executor;

import tigase.xml.Element;

public class EventBusImplementation implements EventBus {

	private final EventsRegistrar registrar = new EventsRegistrar();
	private final EventsNameMap<AbstractHandler> listeners = new EventsNameMap<>();
	private final Serializer serializer = new Serializer();
	private final ReflectEventListenerHandlerFactory reflectEventListenerFactory = new ReflectEventListenerHandlerFactory();
	private boolean acceptOnlyRegisteredEvents = false;
	private Executor executor = new Executor() {
		@Override
		public void execute(Runnable command) {
			command.run();
		}
	};

	public void addHandler(AbstractHandler listenerHandler) {
		listeners.put(listenerHandler.getPackageName(), listenerHandler.getEventName(), listenerHandler);
	}

	public <T> void addListener(Class<T> eventClass, EventListener<T> listener) {
		final String packageName = eventClass.getPackage().getName();
		final String eventName = eventClass.getSimpleName();

		AbstractListenerHandler handler = new ObjectEventsListenerHandler(packageName, eventName, listener);
		addHandler(handler);
		fireListenerAddedEvent(packageName, eventName);
	}

	public <T> void addListener(Class<T> eventClass, EventSourceListener<T> listener) {
		final String packageName = eventClass.getPackage().getName();
		final String eventName = eventClass.getSimpleName();

		AbstractListenerHandler handler = new ObjectEventsSourceListenerHandler(packageName, eventName, listener);

		addHandler(handler);
		fireListenerAddedEvent(packageName, eventName);
	}

	public void addListener(String packageName, String eventName, EventListener<Element> listener) {
		AbstractListenerHandler handler = new ElementListenerHandler(packageName, eventName, listener);
		addHandler(handler);
		fireListenerAddedEvent(packageName, eventName);
	}

	public void addListener(String packageName, String eventName, EventSourceListener<Element> listener) {
		AbstractListenerHandler handler = new ElementSourceListenerHandler(packageName, eventName, listener);
		addHandler(handler);
		fireListenerAddedEvent(packageName, eventName);
	}

	private void checkIfEventIsRegistered(final String eventName) {
		if (this.acceptOnlyRegisteredEvents && !registrar.isRegistered(eventName)) {
			throw new EventBusException("Event " + eventName + " is not registered.");
		}
	}

	protected void doFireThreadPerHandler(final Object event, final Object source, boolean remotelyGeneratedEvent,
			HashSet<AbstractHandler> handlers) {
		Element eventConverted = null;
		for (AbstractHandler listenerHandler : handlers) {
			Object eventObject;

			if (listenerHandler.getRequiredEventType() == AbstractListenerHandler.Type.asIs) {
				eventObject = event;
			} else if (listenerHandler.getRequiredEventType() == AbstractListenerHandler.Type.element
					&& !(event instanceof Element)) {
				if (eventConverted == null)
					eventConverted = serializer.serialize(event);
				eventObject = eventConverted;
			} else if (listenerHandler.getRequiredEventType() != AbstractListenerHandler.Type.element
					&& event instanceof Element) {
				continue;
			} else {
				eventObject = event;
			}

			Runnable task = () -> {
				try {
					listenerHandler.dispatch(eventObject, source, remotelyGeneratedEvent);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			};

			executor.execute(task);
		}
	}

	public void fire(Object event) {
		fire(event, null, false);
	}

	public void fire(Object event, Object source) {
		fire(event, source, false);
	}

	public void fire(Object event, Object source, boolean remotelyGeneratedEvent) {
		HashSet<AbstractHandler> listeners;
		if (event instanceof Element) {
			String eventFullName = ((Element) event).getName();
			int i = eventFullName.lastIndexOf(".");
			final String packageName = i >= 0 ? eventFullName.substring(0, i) : "";
			final String eventName = eventFullName.substring(i + 1);
			checkIfEventIsRegistered(eventFullName);
			listeners = getListenersForEvent(packageName, eventName);
		} else {
			checkIfEventIsRegistered(event.getClass().getName());
			listeners = getListenersForEvent(event.getClass());
		}

		doFireThreadPerHandler(event, source, remotelyGeneratedEvent, listeners);

	}

	private void fireListenerAddedEvent(String packageName, String eventName) {
		ListenerAddedEvent event = new ListenerAddedEvent();
		event.setEventName(eventName);
		event.setPackageName(packageName);

		fire(event);
	}

	public Collection<AbstractHandler> getAllHandlers() {
		return listeners.getAllData();
	}

	public Set<EventName> getAllListenedEvents() {
		return listeners.getAllListenedEvents();
	}

	List<EventListener> getEventListeners(final String packageName, final String eventName) {
		ArrayList<EventListener> result = new ArrayList<>();
		Collection ls = listeners.get(packageName, eventName);
		result.addAll(ls);

		ls = listeners.get(null, eventName);
		result.addAll(ls);

		return result;
	}

	public Executor getExecutor() {
		return executor;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	HashSet<AbstractHandler> getListenersForEvent(final Class<?> eventClass) {
		final HashSet<Class<?>> pp = new HashSet<>();

		// interface-based listeners
		pp.addAll(Arrays.asList(eventClass.getInterfaces()));

		Class<?> tmp = eventClass;
		do {
			pp.add(tmp);
			tmp = tmp.getSuperclass();
		} while (!tmp.equals(Object.class));

		final HashSet<AbstractHandler> result = new HashSet<>();

		for (Class<?> cls : pp) {
			final String packageName = cls.getPackage().getName();
			final String eventName = cls.getSimpleName();

			result.addAll(listeners.get(packageName, eventName));
			result.addAll(listeners.get(packageName, null));
		}
		result.addAll(listeners.get(null, null));

		return result;
	}

	HashSet<AbstractHandler> getListenersForEvent(final String packageName, final String eventName) {
		final HashSet<AbstractHandler> result = new HashSet<>();
		result.addAll(listeners.get(packageName, eventName));
		result.addAll(listeners.get(packageName, null));
		result.addAll(listeners.get(null, null));

		return result;
	}

	public EventsRegistrar getRegistrar() {
		return registrar;
	}

	public boolean isAcceptOnlyRegisteredEvents() {
		return acceptOnlyRegisteredEvents;
	}

	public void setAcceptOnlyRegisteredEvents(boolean acceptOnlyRegisteredEvents) {
		this.acceptOnlyRegisteredEvents = acceptOnlyRegisteredEvents;
	}

	public boolean isListened(String eventPackage, String eventName) {
		return listeners.hasData(eventPackage, eventName);
	}

	public void registerAll(Object consumer) {
		Collection<AbstractHandler> listeners = this.reflectEventListenerFactory.create(consumer);
		for (AbstractHandler l : listeners) {
			addHandler(l);
		}
	}

	public void registerEvent(String event, String description, boolean privateEvent) {
		registrar.register(event, description, privateEvent);
	}

	public void registerEvent(Class<?> event, String description, boolean privateEvent) {
		registrar.register(event.getName(), description, privateEvent);
	}

	public <T> void removeListener(EventSourceListener<T> listener) {
		AbstractListenerHandler handler = new ObjectEventsSourceListenerHandler(null, null, listener);
		removeListenerHandler(handler);
	}

	public <T> void removeListener(EventListener<T> listener) {
		AbstractListenerHandler handler = new ObjectEventsListenerHandler(null, null, listener);
		removeListenerHandler(handler);
	}

	public void removeListenerHandler(AbstractHandler listenerHandler) {
		listeners.delete(listenerHandler);
	}

	public void unregisterAll(Object consumer) {
		Collection<AbstractHandler> listeners = this.reflectEventListenerFactory.create(consumer);
		for (AbstractHandler l : listeners) {
			removeListenerHandler(l);
		}
	}

	public interface InternalEventbusEvent {
	}

	public static class ListenerAddedEvent implements InternalEventbusEvent {
		private String packageName;
		private String eventName;

		public String getEventName() {
			return eventName;
		}

		public void setEventName(String eventName) {
			this.eventName = eventName;
		}

		public String getPackageName() {
			return packageName;
		}

		public void setPackageName(String packageName) {
			this.packageName = packageName;
		}
	}

	public static class ListenerRemovedEvent implements InternalEventbusEvent {
		private String packageName;
		private String eventName;

		public String getEventName() {
			return eventName;
		}

		public void setEventName(String eventName) {
			this.eventName = eventName;
		}

		public String getPackageName() {
			return packageName;
		}

		public void setPackageName(String packageName) {
			this.packageName = packageName;
		}
	}
}
