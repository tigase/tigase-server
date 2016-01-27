package tigase.eventbus;

import java.lang.reflect.Method;

public class ReflectEventSourceListenerHandler extends ReflectEventListenerHandler {

	public ReflectEventSourceListenerHandler(HandleEvent.Type filter, String packageName, String eventName,
			Object consumerObject, Method handlerMethod) {
		super(filter, packageName, eventName, consumerObject, handlerMethod);
	}

	@Override
	public void dispatch(final Object event, final Object source, boolean remotelyGeneratedEvent) {
		if (remotelyGeneratedEvent && filter == HandleEvent.Type.local
				|| !remotelyGeneratedEvent && filter == HandleEvent.Type.remote)
			return;
		try {
			handlerMethod.invoke(consumerObject, event, source);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
