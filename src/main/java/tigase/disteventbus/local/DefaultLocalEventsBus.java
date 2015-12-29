package tigase.disteventbus.local;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic implementation of {@link EventsBus}.
 */
public class DefaultLocalEventsBus implements EventsBus {

	private final static Class<? extends Event> NULL_TYPE = N.class;
	protected final Map<Class<? extends Event>, List<EventHandler>> handlers;
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	private final ReflectEventListenerFactory reflectEventListenerFactory = new ReflectEventListenerFactory();
	protected boolean throwingExceptionOn = true;
	private Executor executor;

	public DefaultLocalEventsBus() {
		setThreadPool(4);
		this.handlers = createMainHandlersMap();
	}

	@Override
	public void addHandler(Class<? extends Event> type, EventHandler listener) {
		doAdd(type, listener);
	}

	@Override
	public void addHandler(EventHandler listener) {
		doAdd(null, listener);
	}

	protected List<EventHandler> createHandlersArray() {
		return new ArrayList<EventHandler>();
	}

	protected Map<Class<? extends Event>, List<EventHandler>> createMainHandlersMap() {
		return new HashMap<Class<? extends Event>, List<EventHandler>>();
	}

	protected void doAdd(Class<? extends Event> type, EventHandler handler) {
		synchronized (this.handlers) {

			List<EventHandler> lst = handlers.get(type == null ? NULL_TYPE : type);
			if (lst == null) {
				lst = createHandlersArray();
				handlers.put(type == null ? NULL_TYPE : type, lst);
			}
			lst.add(handler);
		}

	}

	@SuppressWarnings("unchecked")
	protected void doFire(Event event) {
		if (event == null) {
			throw new NullPointerException("Cannot fire null event");
		}

		final ArrayList<EventHandler> handlers = new ArrayList<EventHandler>();
		synchronized (this.handlers) {
			handlers.addAll(getHandlersList(event.getClass()));
			handlers.addAll(getHandlersList(null));
		}
		doFireThreadPerHandler(event, handlers);
	}

	protected void doFire(Event event, ArrayList<EventHandler> handlers) {
		final Set<Throwable> causes = new HashSet<Throwable>();

		for (EventHandler eventHandler : handlers) {
			try {
				eventHandler.onEvent(event);
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

	protected void doFireThreadPerHandler(Event event, ArrayList<EventHandler> handlers) {
		for (EventHandler eventHandler : handlers) {
			Runnable task = () -> {
				try {
					eventHandler.onEvent(event);
				} catch (Throwable e) {
					if (log.isLoggable(Level.WARNING))
						log.log(Level.WARNING, "", e);
				}
			};
			executor.execute(task);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void fire(Event event) {
		doFire(event);
	}

	public Executor getExecutor() {
		return executor;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	protected Collection<EventHandler> getHandlersList(Class<? extends Event> type) {
		final List<EventHandler> lst = handlers.get(type == null ? NULL_TYPE : type);
		if (lst != null) {
			return lst;
		} else
			return Collections.emptyList();
	}

	public boolean isThrowingExceptionOn() {
		return throwingExceptionOn;
	}

	public void setThrowingExceptionOn(boolean throwingExceptionOn) {
		this.throwingExceptionOn = throwingExceptionOn;
	}

	@Override
	public void registerAll(Object consumer) {
		Collection<ReflectEventHandler> listeners = this.reflectEventListenerFactory.create(consumer);
		for (ReflectEventHandler l : listeners) {
			addHandler(l.getEventType(), l);
		}
	}

	@Override
	public void remove(Class<? extends Event> type, EventHandler handler) {
		synchronized (this.handlers) {
			List<EventHandler> lst = handlers.get(type == null ? NULL_TYPE : type);
			if (lst != null) {
				lst.remove(handler);
				if (lst.isEmpty()) {
					handlers.remove(type == null ? NULL_TYPE : type);
				}
			}
		}
	}

	@Override
	public void remove(EventHandler handler) {
		synchronized (this.handlers) {
			Iterator<Entry<Class<? extends Event>, List<EventHandler>>> iterator = this.handlers.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Class<? extends Event>, List<EventHandler>> entry = iterator.next();
				if (entry != null) {
					entry.getValue().remove(handler);
					if (entry.getValue().isEmpty())
						iterator.remove();
				}
			}
		}
	}

	public void setThreadPool(int pool) {
		this.executor = Executors.newFixedThreadPool(pool);
	}

	@Override
	public void unregisterAll(Object consumer) {
		Collection<ReflectEventHandler> listeners = this.reflectEventListenerFactory.create(consumer);
		for (ReflectEventHandler l : listeners) {
			remove(l.getEventType(), l);
		}
	}

	private final static class N implements Event {

	}

}
