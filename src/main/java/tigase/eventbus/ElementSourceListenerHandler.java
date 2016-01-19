package tigase.eventbus;

import tigase.xml.Element;

class ElementSourceListenerHandler extends AbstractListenerHandler<EventSourceListener<Element>> {

	public ElementSourceListenerHandler(final String packageName, final String eventName,
			EventSourceListener<Element> listener) {
		super(packageName, eventName, listener);
	}

	@Override
	public void dispatch(Object event, Object source, boolean remotelyGeneratedEvent) {
		listener.onEvent((Element) event, source);
	}

	@Override
	public Type getRequiredEventType() {
		return Type.element;
	}
}
