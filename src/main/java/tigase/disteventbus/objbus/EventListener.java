package tigase.disteventbus.objbus;

/**
 * Interface for listeners.<br/>
 * Listener is special viariant of handler. Instead of invoking method
 * {@linkplain Event#dispatch(ObjEventHandler) dispatch()},
 * {@linkplain EventListener#onEvent(Event) onEvent()} will be invoked.
 * 
 */
public interface EventListener extends ObjEventHandler {

	/**
	 * Method called when event is fired.
	 * 
	 * @param event
	 *            fired event.
	 */
	void onEvent(Event<? extends ObjEventHandler> event);

}
