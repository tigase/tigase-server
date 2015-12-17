package tigase.disteventbus.xmlbus;

import tigase.xml.Element;

public interface XMLEventsBus {

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
	public void addHandler(final String name, final String xmlns, final EventHandler handler);

	/**
	 * Fire distributed event.
	 * 
	 * @param event
	 *            event to publish.
	 */
	public void fire(final Element event);

	public void removeHandler(final String name, final String xmlns, final EventHandler handler);

}
