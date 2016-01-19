package tigase.eventbus;

import java.lang.reflect.Method;

public class ReflectEventListenerHandler extends AbstractHandler {

	private final Object consumerObject;

	private final Method handlerMethod;

	private final HandleEvent.Type filter;

	public ReflectEventListenerHandler(HandleEvent.Type filter, final String packageName, final String eventName,
			Object consumerObject, Method handlerMethod) {
		super(packageName, eventName);
		this.filter = filter;
		this.consumerObject = consumerObject;
		this.handlerMethod = handlerMethod;
	}

	@Override
	public void dispatch(final Object event, final Object source, boolean remotelyGeneratedEvent) {
		if (remotelyGeneratedEvent && filter == HandleEvent.Type.local
				|| !remotelyGeneratedEvent && filter == HandleEvent.Type.remote)
			return;
		try {
			handlerMethod.invoke(consumerObject, event);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;

		ReflectEventListenerHandler that = (ReflectEventListenerHandler) o;

		if (!consumerObject.equals(that.consumerObject))
			return false;
		return handlerMethod.equals(that.handlerMethod);

	}

	@Override
	public Type getRequiredEventType() {
		return Type.object;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + consumerObject.hashCode();
		result = 31 * result + handlerMethod.hashCode();
		return result;
	}
}
