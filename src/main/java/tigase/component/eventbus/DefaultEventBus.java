/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.component.eventbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic implementation of {@link EventBus}.
 */
public class DefaultEventBus extends EventBus {

	private final static class N extends Event<EventHandler> {

		@Override
		protected void dispatch(EventHandler handler) throws Exception {
		}
	}

	private final static Object NULL_SOURCE = new Object();

	private final static Class<? extends Event<?>> NULL_TYPE = N.class;

	protected final Map<Object, Map<Class<? extends Event<?>>, List<EventHandler>>> handlers;

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	protected boolean throwingExceptionOn = true;

	public DefaultEventBus() {
		this.handlers = createMainHandlersMap();
	}

	@Override
	public <H extends EventHandler> void addHandler(Class<? extends Event<H>> type, H handler) {
		doAdd(type, null, handler);
	}

	@Override
	public <H extends EventHandler> void addHandler(Class<? extends Event<H>> type, Object source, H handler) {
		doAdd(type, source, handler);
	}

	@Override
	public <H extends EventHandler> void addListener(Class<? extends Event<H>> type, EventListener listener) {
		doAdd(type, null, listener);
	}

	@Override
	public <H extends EventHandler> void addListener(Class<? extends Event<H>> type, Object source, EventListener listener) {
		doAdd(type, source, listener);
	}

	@Override
	public <H extends EventHandler> void addListener(EventListener listener) {
		doAdd(null, null, listener);
	}

	protected List<EventHandler> createHandlersArray() {
		return new ArrayList<EventHandler>();
	}

	protected Map<Object, Map<Class<? extends Event<?>>, List<EventHandler>>> createMainHandlersMap() {
		return new HashMap<Object, Map<Class<? extends Event<?>>, List<EventHandler>>>();
	}

	protected Map<Class<? extends Event<?>>, List<EventHandler>> createTypeHandlersMap() {
		return new HashMap<Class<? extends Event<?>>, List<EventHandler>>();
	}

	protected void doAdd(Class<? extends Event<?>> type, Object source, EventHandler handler) {
		synchronized (this.handlers) {
			Map<Class<? extends Event<?>>, List<EventHandler>> hdlrs = getHandlersBySource(source);
			if (hdlrs == null) {
				hdlrs = createTypeHandlersMap();
				handlers.put(source == null ? NULL_SOURCE : source, hdlrs);
			}

			List<EventHandler> lst = hdlrs.get(type == null ? NULL_TYPE : type);
			if (lst == null) {
				lst = createHandlersArray();
				hdlrs.put(type == null ? NULL_TYPE : type, lst);
			}
			lst.add(handler);
		}

	}

	@SuppressWarnings("unchecked")
	protected void doFire(Event<EventHandler> event, Object source) {
		if (event == null) {
			throw new NullPointerException("Cannot fire null event");
		}

		setEventSource(event, source);
		final ArrayList<EventHandler> handlers = new ArrayList<EventHandler>();
		synchronized (this.handlers) {
			handlers.addAll(getHandlersList((Class<? extends Event<?>>) event.getClass(), source));
			handlers.addAll(getHandlersList(null, source));
			if (source != null) {
				handlers.addAll(getHandlersList((Class<? extends Event<?>>) event.getClass(), null));
				handlers.addAll(getHandlersList(null, null));
			}
		}
		doFire(event, source, handlers);
	}

	protected void doFire(Event<EventHandler> event, Object source, ArrayList<EventHandler> handlers) {
		final Set<Throwable> causes = new HashSet<Throwable>();

		for (EventHandler eventHandler : handlers) {
			try {
				if (eventHandler instanceof EventListener) {
					((EventListener) eventHandler).onEvent(event);
				} else {
					event.dispatch(eventHandler);
				}
			} catch (Throwable e) {
				if (log.isLoggable(Level.WARNING))
					log.log(Level.WARNING, "", e);
				causes.add(e);
			}
		}

		if (!causes.isEmpty()) {
			if (throwingExceptionOn)
				throw new EventBusException(causes);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void fire(Event<?> event) {
		doFire((Event<EventHandler>) event, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void fire(Event<?> event, Object source) {
		doFire((Event<EventHandler>) event, source);
	}

	private Map<Class<? extends Event<?>>, List<EventHandler>> getHandlersBySource(Object source) {
		return handlers.get(source == null ? NULL_SOURCE : source);
	}

	protected Collection<EventHandler> getHandlersList(Class<? extends Event<?>> type, Object source) {
		final Map<Class<? extends Event<?>>, List<EventHandler>> hdlrs = getHandlersBySource(source);
		if (hdlrs == null) {
			return Collections.emptyList();
		} else {
			final List<EventHandler> lst = hdlrs.get(type == null ? NULL_TYPE : type);
			if (lst != null) {
				return lst;
			} else
				return Collections.emptyList();
		}
	}

	public boolean isThrowingExceptionOn() {
		return throwingExceptionOn;
	}

	@Override
	public void remove(Class<? extends Event<?>> type, EventHandler handler) {
		remove(type, null, handler);
	}

	@Override
	public void remove(Class<? extends Event<?>> type, Object source, EventHandler handler) {
		synchronized (this.handlers) {
			final Map<Class<? extends Event<?>>, List<EventHandler>> hdlrs = getHandlersBySource(source);
			if (hdlrs != null) {
				List<EventHandler> lst = hdlrs.get(type == null ? NULL_TYPE : type);
				if (lst != null) {
					lst.remove(handler);
					if (lst.isEmpty()) {
						hdlrs.remove(type == null ? NULL_TYPE : type);
					}
					if (hdlrs.isEmpty()) {
						handlers.remove(source == null ? NULL_SOURCE : source);
					}
				}
			}
		}
	}

	@Override
	public void remove(EventHandler handler) {
		synchronized (this.handlers) {
			Iterator<Entry<Object, Map<Class<? extends Event<?>>, List<EventHandler>>>> l = this.handlers.entrySet().iterator();
			while (l.hasNext()) {
				Map<Class<? extends Event<?>>, List<EventHandler>> eventHandlers = l.next().getValue();
				Iterator<Entry<Class<? extends Event<?>>, List<EventHandler>>> iterator = eventHandlers.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Class<? extends Event<?>>, List<EventHandler>> entry = iterator.next();
					if (entry != null) {
						entry.getValue().remove(handler);
						if (entry.getValue().isEmpty())
							iterator.remove();
					}
				}
				if (eventHandlers.isEmpty())
					l.remove();
			}
		}
	}

	public void setThrowingExceptionOn(boolean throwingExceptionOn) {
		this.throwingExceptionOn = throwingExceptionOn;
	}

}
