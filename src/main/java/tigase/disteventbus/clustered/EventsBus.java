package tigase.disteventbus.clustered;

import tigase.xml.Element;

public interface EventsBus {

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
	void addHandler(final String name, final String xmlns, final EventHandler handler);

	/**
	 * Fire distributed event.
	 * 
	 * @param event
	 *            event to publish.
	 */
	void fire(final Element event);

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
	void removeHandler(final String name, final String xmlns, final EventHandler handler);

}
