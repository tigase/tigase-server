package tigase.eventbus;

import java.lang.reflect.Method;

public class ReflectEventSourceListenerHandler extends AbstractListenerHandler {

	private final Object consumerObject;

	private final Method handlerMethod;

	public ReflectEventSourceListenerHandler(final String packageName, final String eventName, Object consumerObject,
			Method handlerMethod) {
		super(packageName, eventName, null);
		this.consumerObject = consumerObject;
		this.handlerMethod = handlerMethod;
	}

	@Override
	public void dispatch(final Object event, final Object source, boolean remotelyGeneratedEvent) {
		try {
			handlerMethod.invoke(consumerObject, event, source);
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

		ReflectEventSourceListenerHandler that = (ReflectEventSourceListenerHandler) o;

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
