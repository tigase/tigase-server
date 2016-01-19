package tigase.eventbus;

public class ObjectEventsSourceListenerHandler extends AbstractListenerHandler<EventSourceListener> {

	public ObjectEventsSourceListenerHandler(final String packageName, final String eventName, EventSourceListener listener) {
		super(packageName, eventName, listener);
	}

	@Override
	public void dispatch(Object event, Object source, boolean remotelyGeneratedEvent) {
		listener.onEvent(event, source);
	}

	@Override
	public Type getRequiredEventType() {
		return Type.object;
	}
}
