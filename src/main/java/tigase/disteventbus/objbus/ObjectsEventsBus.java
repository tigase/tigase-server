package tigase.disteventbus.objbus;

/**
 * Dispatches events to all registered handlers and listeners.
 */
public interface ObjectsEventsBus {

	/**
	 * Adds handler to receive given type of events.
	 *
	 * @param type
	 *            type of event.
	 * @param handler
	 *            event handler
	 */
	<H extends ObjEventHandler> void addHandler(Class<? extends Event<H>> type, H handler);

	/**
	 * Adds handler to receive given type of events from specified source.
	 * 
	 * @param type
	 *            type of event.
	 * @param source
	 *            source of event.
	 * @param handler
	 *            event handler.
	 */
	<H extends ObjEventHandler> void addHandler(Class<? extends Event<H>> type, Object source, H handler);

	/**
	 * Adds listener to receive given type of events.
	 * 
	 * @param type
	 *            type of event.
	 * @param listener
	 *            event listener.
	 */
	<H extends ObjEventHandler> void addListener(Class<? extends Event<H>> type, EventListener listener);

	/**
	 * Adds listener to receive given type of events from specified source.
	 * 
	 * @param type
	 *            type of event.
	 * @param source
	 *            source of event.
	 * @param listener
	 *            event listener.
	 */
	<H extends ObjEventHandler> void addListener(Class<? extends Event<H>> type, Object source, EventListener listener);

	/**
	 * Adds listener to receive all types events.
	 * 
	 * @param listener
	 *            event listener.
	 */
	<H extends ObjEventHandler> void addListener(EventListener listener);

	/**
	 * Fires event.
	 * 
	 * @param e
	 *            event to fire
	 */
	void fire(Event<?> e);

	/**
	 * Fires event.
	 * 
	 * @param e
	 *            event to fire.
	 * @param source
	 *            source of event.
	 */
	void fire(Event<?> e, Object source);

	/**
	 * Removes listener or handler of given type.
	 * 
	 * @param type
	 *            type of event.
	 * @param handler
	 *            handler or listener to remove from EventBus.
	 */
	void remove(Class<? extends Event<?>> type, ObjEventHandler handler);

	/**
	 * Removes listener or handler of given type added registered to receive
	 * event from specified source.
	 * 
	 * @param type
	 *            type of event.
	 * @param source
	 *            source of event.
	 * @param handler
	 *            handler or listener to remove from EventBus.
	 */
	void remove(Class<? extends Event<?>> type, Object source, ObjEventHandler handler);

	/**
	 * Removed listener or handler.
	 * 
	 * @param handler
	 *            handler or listener to remove from EventBus.
	 */
	void remove(ObjEventHandler handler);

}
