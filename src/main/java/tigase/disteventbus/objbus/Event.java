package tigase.disteventbus.objbus;

/**
 * Base event object.
 * 
 * @param <H>
 *            handler type.
 */
public abstract class Event<H extends ObjEventHandler> {

	private Object source;

	protected Event() {
		super();
	}

	/**
	 * Invokes handlers method.
	 * 
	 * @param handler
	 *            handler
	 */
	protected abstract void dispatch(H handler) throws Exception;

	/**
	 * Returns events source.
	 * 
	 * @return events source. May be <code>null</code>.
	 */
	public Object getSource() {
		return source;
	}

	/**
	 * Sets source.
	 * 
	 * @param source
	 *            event source.
	 */
	void setSource(Object source) {
		this.source = source;
	};

}
