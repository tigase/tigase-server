package tigase.eventbus;

public abstract class AbstractHandler {

	private final String packageName;
	private final String eventName;

	public AbstractHandler(String packageName, String eventName) {
		this.packageName = packageName;
		this.eventName = eventName;
	}

	public abstract void dispatch(Object event, Object source, boolean remotelyGeneratedEvent);

	public String getEventName() {
		return eventName;
	}

	public String getPackageName() {
		return packageName;
	}

	public abstract Type getRequiredEventType();

	/**
	 * Defines what type of event is expected by Handler.
	 */
	public enum Type {
		/**
		 * Only non-XML events. XML events will be ignored.
		 */
		object,
		/**
		 * Only XML events. Non-XML events will be converted to XML.
		 */
		element,
		/**
		 * As is. Without conversion.
		 */
		asIs
	}

}
