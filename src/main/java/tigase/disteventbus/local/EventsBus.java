package tigase.disteventbus.local;

/**
 * Dispatches events to all registered handler.
 */
public interface EventsBus {

	/**
	 * Adds handler to receive given type of events.
	 * 
	 * @param type
	 *            type of event.
	 * @param handler
	 *            event handler.
	 */
	void addHandler(Class<? extends Event> type, EventHandler handler);

	/**
	 * Adds handler to receive all types events.
	 * 
	 * @param handler
	 *            event handler.
	 */
	void addHandler(EventHandler handler);

	/**
	 * Fires event.
	 * 
	 * @param e
	 *            event to fire
	 */
	void fire(Event e);

	/**
	 * Register all methods annotated with {@link HandleEvent @HandleEvent} as
	 * events handlers to EventBus.
	 * 
	 * @param consumer
	 *            events consumer object.
	 * @throws RegistrationException
	 *             if it is impossible to register all handlers method.
	 */
	void registerAll(Object consumer) throws RegistrationException;

	/**
	 * Removes handler of given type.
	 *
	 * @param type
	 *            type of event.
	 * @param handler
	 *            handler to remove from EventBus.
	 */
	void remove(Class<? extends Event> type, EventHandler handler);

	/**
	 * Removed handler.
	 *
	 * @param handler
	 *            handler to remove from EventBus.
	 */
	void remove(EventHandler handler);

	/**
	 * Unregister all methods annotated with {@link HandleEvent @HandleEvent} as
	 * events handlers from EventBus.
	 *
	 * @param consumer
	 *            events consumer object.
	 */
	void unregisterAll(Object consumer);

}
