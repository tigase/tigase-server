package tigase.eventbus;

import tigase.xml.Element;

public interface EventBus {

	/**
	 * Adds listener of event to EventBus.
	 * 
	 * @param eventClass
	 *            class of expected event.
	 * @param listener
	 *            listener.
	 * @param <T>
	 *            class of event.
	 */
	<T> void addListener(Class<T> eventClass, EventListener<T> listener);

	/**
	 * Adds listener of event to EventBus. If event matching to given
	 * packageName and eventName will be fired as Object (not Element), then
	 * event will be converted to XML.
	 * 
	 * @param packageName
	 *            package of event to listen.
	 * @param eventName
	 *            name of event to listen. May be <code>null</code>, then
	 *            listener is listening for all events with specific package
	 *            name.
	 * @param listener
	 *            listener.
	 */
	void addListener(String packageName, String eventName, EventListener<Element> listener);

	/**
	 * Fires event.
	 * 
	 * @param event
	 *            event to fire.
	 */
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

	/**
	 * Removes listener from Eventbus.
	 * 
	 * @param listener
	 *            listener to remove.
	 * @param <T>
	 */
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
