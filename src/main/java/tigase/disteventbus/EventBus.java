package tigase.disteventbus;

import tigase.xml.Element;

public interface EventBus {

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

	public void fire(final Element event);

	public void removeHandler(final String name, final String xmlns, final EventHandler handler);

}
