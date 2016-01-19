package tigase.eventbus;

public class ObjectEventsListenerHandler extends AbstractListenerHandler<EventListener> {

	public ObjectEventsListenerHandler(final String packageName, final String eventName, EventListener listener) {
		super(packageName, eventName, listener);
	}

	@Override
	public void dispatch(Object event, Object source, boolean remotelyGeneratedEvent) {
		listener.onEvent(event);
	}

	@Override
	public Type getRequiredEventType() {
		return Type.object;
	}
}
