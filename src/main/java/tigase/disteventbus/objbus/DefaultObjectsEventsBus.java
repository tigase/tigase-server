package tigase.disteventbus.objbus;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic implementation of {@link ObjectsEventsBus}.
 */
public class DefaultObjectsEventsBus implements ObjectsEventsBus {

	private final static Object NULL_SOURCE = new Object();
	private final static Class<? extends Event<?>> NULL_TYPE = N.class;
	protected final Map<Object, Map<Class<? extends Event<?>>, List<ObjEventHandler>>> handlers;
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	protected boolean throwingExceptionOn = true;

	public DefaultObjectsEventsBus() {
		this.handlers = createMainHandlersMap();
	}

	@Override
	public <H extends ObjEventHandler> void addHandler(Class<? extends Event<H>> type, H handler) {
		doAdd(type, null, handler);
	}

	@Override
	public <H extends ObjEventHandler> void addHandler(Class<? extends Event<H>> type, Object source, H handler) {
		doAdd(type, source, handler);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(Class<? extends Event<H>> type, EventListener listener) {
		doAdd(type, null, listener);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(Class<? extends Event<H>> type, Object source, EventListener listener) {
		doAdd(type, source, listener);
	}

	@Override
	public <H extends ObjEventHandler> void addListener(EventListener listener) {
		doAdd(null, null, listener);
	}

	protected List<ObjEventHandler> createHandlersArray() {
		return new ArrayList<ObjEventHandler>();
	}

	protected Map<Object, Map<Class<? extends Event<?>>, List<ObjEventHandler>>> createMainHandlersMap() {
		return new HashMap<Object, Map<Class<? extends Event<?>>, List<ObjEventHandler>>>();
	}

	protected Map<Class<? extends Event<?>>, List<ObjEventHandler>> createTypeHandlersMap() {
		return new HashMap<Class<? extends Event<?>>, List<ObjEventHandler>>();
	}

	protected void doAdd(Class<? extends Event<?>> type, Object source, ObjEventHandler handler) {
		synchronized (this.handlers) {
			Map<Class<? extends Event<?>>, List<ObjEventHandler>> hdlrs = getHandlersBySource(source);
			if (hdlrs == null) {
				hdlrs = createTypeHandlersMap();
				handlers.put(source == null ? NULL_SOURCE : source, hdlrs);
			}

			List<ObjEventHandler> lst = hdlrs.get(type == null ? NULL_TYPE : type);
			if (lst == null) {
				lst = createHandlersArray();
				hdlrs.put(type == null ? NULL_TYPE : type, lst);
			}
			lst.add(handler);
		}

	}

	@SuppressWarnings("unchecked")
	protected void doFire(Event<ObjEventHandler> event, Object source) {
		if (event == null) {
			throw new NullPointerException("Cannot fire null event");
		}

		setEventSource(event, source);
		final ArrayList<ObjEventHandler> handlers = new ArrayList<ObjEventHandler>();
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

	protected void doFire(Event<ObjEventHandler> event, Object source, ArrayList<ObjEventHandler> handlers) {
		final Set<Throwable> causes = new HashSet<Throwable>();

		for (ObjEventHandler eventHandler : handlers) {
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
		doFire((Event<ObjEventHandler>) event, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void fire(Event<?> event, Object source) {
		doFire((Event<ObjEventHandler>) event, source);
	}

	private Map<Class<? extends Event<?>>, List<ObjEventHandler>> getHandlersBySource(Object source) {
		return handlers.get(source == null ? NULL_SOURCE : source);
	}

	protected Collection<ObjEventHandler> getHandlersList(Class<? extends Event<?>> type, Object source) {
		final Map<Class<? extends Event<?>>, List<ObjEventHandler>> hdlrs = getHandlersBySource(source);
		if (hdlrs == null) {
			return Collections.emptyList();
		} else {
			final List<ObjEventHandler> lst = hdlrs.get(type == null ? NULL_TYPE : type);
			if (lst != null) {
				return lst;
			} else
				return Collections.emptyList();
		}
	}

	public boolean isThrowingExceptionOn() {
		return throwingExceptionOn;
	}

	public void setThrowingExceptionOn(boolean throwingExceptionOn) {
		this.throwingExceptionOn = throwingExceptionOn;
	}

	@Override
	public void remove(Class<? extends Event<?>> type, ObjEventHandler handler) {
		remove(type, null, handler);
	}

	@Override
	public void remove(Class<? extends Event<?>> type, Object source, ObjEventHandler handler) {
		synchronized (this.handlers) {
			final Map<Class<? extends Event<?>>, List<ObjEventHandler>> hdlrs = getHandlersBySource(source);
			if (hdlrs != null) {
				List<ObjEventHandler> lst = hdlrs.get(type == null ? NULL_TYPE : type);
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
	public void remove(ObjEventHandler handler) {
		synchronized (this.handlers) {
			Iterator<Entry<Object, Map<Class<? extends Event<?>>, List<ObjEventHandler>>>> l = this.handlers.entrySet().iterator();
			while (l.hasNext()) {
				Map<Class<? extends Event<?>>, List<ObjEventHandler>> eventHandlers = l.next().getValue();
				Iterator<Entry<Class<? extends Event<?>>, List<ObjEventHandler>>> iterator = eventHandlers.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Class<? extends Event<?>>, List<ObjEventHandler>> entry = iterator.next();
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

	/**
	 * Puts event source to event.
	 */
	protected void setEventSource(Event<ObjEventHandler> event, Object source) {
		event.setSource(source);
	}

	private final static class N extends Event<ObjEventHandler> {

		@Override
		protected void dispatch(ObjEventHandler handler) throws Exception {
		}
	}

}
