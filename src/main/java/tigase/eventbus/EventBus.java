package tigase.eventbus;

public interface EventBus {

	<T> void addListener(Class<T> eventClass, EventListener<T> listener);

	void fire(Object event);

	/**
	 * Register all methods annotated with {@link HandleEvent @HandleEvent} as
	 * events handlers to EventBus.
	 *
	 * @param eventConsumer
	 *            events consumer object.
	 * @throws RegistrationException
	 *             if it is impossible to register all handlers method.
	 */
	void registerAll(Object eventConsumer);

	<T> void removeListener(EventListener<T> listener);

	/**
	 * Unregister all methods annotated with {@link HandleEvent @HandleEvent} as
	 * events handlers from EventBus.
	 *
	 * @param eventConsumer
	 *            events consumer object.
	 */
	void unregisterAll(Object eventConsumer);

}
