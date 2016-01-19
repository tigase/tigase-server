package tigase.eventbus;

import tigase.xml.Element;

class ElementListenerHandler extends AbstractListenerHandler<EventListener<Element>> {

	public ElementListenerHandler(final String packageName, final String eventName, EventListener<Element> listener) {
		super(packageName, eventName, listener);
	}

	@Override
	public void dispatch(Object event, Object source, boolean remotelyGeneratedEvent) {
		listener.onEvent((Element) event);
	}

	@Override
	public Type getRequiredEventType() {
		return Type.element;
	}
}
