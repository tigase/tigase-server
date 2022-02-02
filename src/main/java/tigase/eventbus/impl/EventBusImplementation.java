/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.eventbus.impl;

import tigase.annotations.TigaseDeprecated;
import tigase.eventbus.*;
import tigase.xml.Element;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventBusImplementation
		implements EventBus {

	private static final Logger log = Logger.getLogger(EventBusImplementation.class.getName());
	private final EventsNameMap<AbstractHandler> listeners = new EventsNameMap<>();
	private final ReflectEventListenerHandlerFactory reflectEventListenerFactory = new ReflectEventListenerHandlerFactory();
	private final ReflectEventRoutedTransientFillerFactory reflectEventRoutedTransientFillerFactory = new ReflectEventRoutedTransientFillerFactory();
	private final ReflectEventRoutingSelectorFactory reflectEventRoutingSelectorFactory = new ReflectEventRoutingSelectorFactory();
	private final EventsRegistrar registrar = new EventsRegistrar();
	private final Map<Class<?>, Set<EventRoutedTransientFiller>> routedTransientFillers = new ConcurrentHashMap<>();
	private final Map<Class<?>, EventRoutingSelector> routingSelectors = new ConcurrentHashMap<>();
	private final Serializer serializer = new EventBusSerializer();
	private boolean acceptOnlyRegisteredEvents = false;
	private Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public EventBusImplementation() {
		this.scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				fire(new TickMinuteEvent(System.currentTimeMillis()));
			}
		}, 1, 1, TimeUnit.MINUTES);
	}

	public void addHandler(AbstractHandler listenerHandler) {
		listeners.put(listenerHandler.getPackageName(), listenerHandler.getEventName(), listenerHandler);
	}

	public <T> void addListener(Class<T> eventClass, tigase.eventbus.EventListener<T> listener) {
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

	@Deprecated
	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Class based events should be used")
	public void addListener(String packageName, String eventName, tigase.eventbus.EventListener<Element> listener) {
		AbstractListenerHandler handler = new ElementListenerHandler(packageName, eventName, listener);
		addHandler(handler);
		fireListenerAddedEvent(packageName, eventName);
	}

	@Deprecated
	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Class based events should be used")
	public void addListener(String packageName, String eventName, EventSourceListener<Element> listener) {
		AbstractListenerHandler handler = new ElementSourceListenerHandler(packageName, eventName, listener);
		addHandler(handler);
		fireListenerAddedEvent(packageName, eventName);
	}

	@Deprecated
	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Class based events should be used")
	public void fire(Object event) {
		fire(event, null, false);
	}

	public void fire(EventBusEvent event) {
		fire(event, null, false);
	}

	public void fire(Object event, Object source) {
		fire(event, source, false);
	}

	public void fire(Object event, Object source, boolean remotelyGeneratedEvent) {
		try {
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
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem on firing event", e);
		}
	}

	public Collection<AbstractHandler> getAllHandlers() {
		return listeners.getAllData();
	}

	public Set<EventName> getAllListenedEvents() {
		return listeners.getAllListenedEvents();
	}

	public Collection<EventRoutedTransientFiller> getEventRoutedTransientFillers(Class<?> eventClass) {
		final HashSet<EventRoutedTransientFiller> result = new HashSet<>();
		Class<?> tmp = eventClass;
		do {
			Collection<EventRoutedTransientFiller> fillers = routedTransientFillers.get(tmp);
			if (fillers != null) {
				result.addAll(fillers);
			}
			tmp = tmp.getSuperclass();
		} while (!tmp.equals(Object.class));

		return result;
	}

	public EventRoutingSelector getEventRoutingSelector(Class<?> eventClass) {
		Class<?> tmp = eventClass;
		EventRoutingSelector handler = null;
		do {
			handler = routingSelectors.get(tmp);
			if (handler != null) {
				break;
			}
			tmp = tmp.getSuperclass();
		} while (!tmp.equals(Object.class));

		return handler;
	}

	public Executor getExecutor() {
		return executor;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public EventsRegistrar getRegistrar() {
		return registrar;
	}

	public Serializer getSerializer() {
		return serializer;
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
		Collection<EventRoutedTransientFiller> fillers = this.reflectEventRoutedTransientFillerFactory.create(consumer);
		for (EventRoutedTransientFiller f : fillers) {
			Set<EventRoutedTransientFiller> eventFillers = routedTransientFillers.computeIfAbsent(f.getEventClass(),
																								  (Class<?> c) -> {
																									  return new CopyOnWriteArraySet<>();
																								  });
			eventFillers.add(f);
		}
		Collection<EventRoutingSelector> selectors = this.reflectEventRoutingSelectorFactory.create(consumer);
		for (EventRoutingSelector s : selectors) {
			routingSelectors.put(s.getEventClass(), s);
		}
	}

	public void registerEvent(String event, String description, boolean privateEvent) {
		registrar.register(event, description, privateEvent);
	}

	public void registerEvent(Class<?> event, String description, boolean privateEvent) {
		registrar.register(event.getName(), description, privateEvent);
	}

	public void removeHandler(AbstractHandler listenerHandler) {
		listeners.delete(listenerHandler);
	}

	public <T> void removeListener(EventSourceListener<T> listener) {
		AbstractListenerHandler handler = new ObjectEventsSourceListenerHandler(null, null, listener);
		removeHandler(handler);
	}

	public <T> void removeListener(tigase.eventbus.EventListener<T> listener) {
		if (listener == null) {
			return;
		}
		AbstractListenerHandler handler = new ObjectEventsListenerHandler(null, null, listener);
		removeHandler(handler);
	}

	public void unregisterAll(Object consumer) {
		Collection<AbstractHandler> listeners = this.reflectEventListenerFactory.create(consumer);
		for (AbstractHandler l : listeners) {
			removeHandler(l);
		}
		Collection<EventRoutingSelector> selectors = this.reflectEventRoutingSelectorFactory.create(consumer);
		for (EventRoutingSelector s : selectors) {
			routingSelectors.remove(s.getEventClass(), s);
		}
		Collection<EventRoutedTransientFiller> fillers = this.reflectEventRoutedTransientFillerFactory.create(consumer);
		for (EventRoutedTransientFiller f : fillers) {
			Set<EventRoutedTransientFiller> eventFillers = routedTransientFillers.get(f.getEventClass());
			if (eventFillers != null) {
				eventFillers.remove(f);
			}
		}
	}

	List<tigase.eventbus.EventListener> getEventListeners(final String packageName, final String eventName) {
		ArrayList<tigase.eventbus.EventListener> result = new ArrayList<>();
		Collection ls = listeners.get(packageName, eventName);
		result.addAll(ls);

		ls = listeners.get(null, eventName);
		result.addAll(ls);

		return result;
	}

	HashSet<AbstractHandler> getListenersForEvent(final Class<?> eventClass) {

		final HashSet<AbstractHandler> result = new HashSet<>();

		// interface-based listeners
		for (Class<?> cls : eventClass.getInterfaces()) {
			fillListenersForEvent(result, cls);
		}

		// class-based listeners
		Class<?> cls = eventClass;
		while (!cls.equals(Object.class)) {
			fillListenersForEvent(result, cls);
			cls = cls.getSuperclass();
		}
		result.addAll(listeners.get(null, null));

		return result;
	}

	@Deprecated
	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Class based events should be used")
	HashSet<AbstractHandler> getListenersForEvent(final String packageName, final String eventName) {
		final HashSet<AbstractHandler> result = new HashSet<>();
		result.addAll(listeners.get(packageName, eventName));
		result.addAll(listeners.get(packageName, null));
		result.addAll(listeners.get(null, null));

		return result;
	}

	protected void doFireThreadPerHandler(final Object event, final Object source, boolean remotelyGeneratedEvent,
										  HashSet<AbstractHandler> handlers) {
		Element eventConverted = null;
		for (AbstractHandler listenerHandler : handlers) {
			Object eventObject;

			if (listenerHandler.getRequiredEventType() == AbstractListenerHandler.Type.asIs) {
				eventObject = event;
			} else if (listenerHandler.getRequiredEventType() == AbstractListenerHandler.Type.element &&
					!(event instanceof Element)) {
				if (eventConverted == null) {
					eventConverted = serializer.serialize(event);
				}
				eventObject = eventConverted;
			} else if (listenerHandler.getRequiredEventType() != AbstractListenerHandler.Type.element &&
					event instanceof Element) {
				continue;
			} else {
				eventObject = event;
			}

			Runnable task = () -> {
				try {
					listenerHandler.dispatch(eventObject, source, remotelyGeneratedEvent);
				} catch (Throwable e) {
					log.log(Level.WARNING,
							"Exception during execution of event: " + event.getClass().getCanonicalName(), e);
				}
			};

			executor.execute(task);
		}
	}

	private void checkIfEventIsRegistered(final String eventName) throws EventBusException {
		if (!registrar.isRegistered(eventName)) {
			if (this.acceptOnlyRegisteredEvents) {
				throw new EventBusException("Event " + eventName + " is not registered.");
			} else {
				log.log(Level.FINEST, "Event " + eventName + " in not registered.");
			}
		}
	}

	private void fillListenersForEvent(HashSet<AbstractHandler> result, Class<?> cls) {
		final String packageName = cls.getPackage().getName();
		final String eventName = cls.getSimpleName();

		result.addAll(listeners.get(packageName, eventName));
		result.addAll(listeners.get(packageName, null));
	}

	private void fireListenerAddedEvent(String packageName, String eventName) {
		ListenerAddedEvent event = new ListenerAddedEvent();
		event.setEventName(eventName);
		event.setPackageName(packageName);

		fire(event);
	}

	public interface InternalEventbusEvent {

	}

	public static class ListenerAddedEvent
			implements InternalEventbusEvent {

		private String eventName;
		private String packageName;

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

	public static class ListenerRemovedEvent
			implements InternalEventbusEvent {

		private String eventName;
		private String packageName;

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
