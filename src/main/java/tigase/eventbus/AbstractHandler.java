package tigase.eventbus;

/**
 * Created by bmalkow on 18.01.2016.
 */
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

	public enum Type {
		object,
		element,
		asIs
	}

}
