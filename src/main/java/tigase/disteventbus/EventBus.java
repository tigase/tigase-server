package tigase.disteventbus;

import tigase.disteventbus.local.Event;
import tigase.disteventbus.local.HandleEvent;
import tigase.disteventbus.local.RegistrationException;
import tigase.xml.Element;

/**
 * Main interface of EventBus.
 */
public interface EventBus extends tigase.disteventbus.clustered.EventsBus, tigase.disteventbus.local.EventsBus {

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
	void addHandler(final String name, final String xmlns, final tigase.disteventbus.clustered.EventHandler handler);

	/**
	 * Adds handler to receive given type of events.
	 *
	 * @param type
	 *            type of event.
	 * @param handler
	 *            event handler.
	 */
	void addHandler(Class<? extends Event> type, tigase.disteventbus.local.EventHandler handler);

	/**
	 * Adds handler to receive all types events.
	 *
	 * @param handler
	 *            event handler.
	 */
	void addHandler(tigase.disteventbus.local.EventHandler handler);

	/**
	 * Fire distributed event.
	 *
	 * @param event
	 *            event to publish.
	 */
	void fire(final Element event);

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
	void remove(Class<? extends Event> type, tigase.disteventbus.local.EventHandler handler);

	/**
	 * Removed handler.
	 *
	 * @param handler
	 *            handler to remove from EventBus.
	 */
	void remove(tigase.disteventbus.local.EventHandler handler);

	/**
	 * Removes handler for specific event.
	 *
	 * @param name
	 *            name of event. Can be {@code null}.
	 * @param xmlns
	 *            namespace of events.
	 * @param handler
	 *            event handler.
	 */
	void removeHandler(final String name, final String xmlns, final tigase.disteventbus.clustered.EventHandler handler);

	/**
	 * Unregister all methods annotated with
	 * {@link tigase.disteventbus.clustered.EventHandler @HandleEvent} as events
	 * handlers from EventBus.
	 *
	 * @param consumer
	 *            events consumer object.
	 */
	void unregisterAll(Object consumer);
}
