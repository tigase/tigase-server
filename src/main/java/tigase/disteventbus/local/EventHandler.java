package tigase.disteventbus.local;

/**
 * Interface for listeners.
 * 
 */
public interface EventHandler<E extends Event> {

	/**
	 * Method called when event is fired.
	 * 
	 * @param event
	 *            fired event.
	 */
	void onEvent(E event);

}
