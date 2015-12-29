package tigase.disteventbus.local;

import java.lang.reflect.Method;

/**
 * Created by bmalkow on 30.12.2015.
 */
public class ReflectEventHandler implements EventHandler {

	private final Object consumerObject;

	private final Method handlerMethod;

	private final Class<? extends Event> eventType;

	public ReflectEventHandler(Object consumerObject, Method handlerMethod) {
		this.consumerObject = consumerObject;
		this.handlerMethod = handlerMethod;
		this.eventType = (Class<? extends Event>) handlerMethod.getParameters()[0].getType();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ReflectEventHandler that = (ReflectEventHandler) o;

		if (!consumerObject.equals(that.consumerObject))
			return false;
		return handlerMethod.equals(that.handlerMethod);

	}

	public Class<? extends Event> getEventType() {
		return eventType;
	}

	@Override
	public int hashCode() {
		int result = consumerObject.hashCode();
		result = 31 * result + handlerMethod.hashCode();
		return result;
	}

	@Override
	public void onEvent(final Event event) {
		try {
			handlerMethod.invoke(consumerObject, event);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
