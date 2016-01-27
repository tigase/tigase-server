package tigase.eventbus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

public class ReflectEventListenerHandlerFactory {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	public Collection<AbstractHandler> create(final Object consumer) throws RegistrationException {
		ArrayList<AbstractHandler> result = new ArrayList<>();

		Method[] methods = consumer.getClass().getMethods();
		for (Method method : methods) {
			final HandleEvent annotation = method.getAnnotation(HandleEvent.class);

			if (annotation == null)
				continue;

			if (method.getParameterCount() < 1) {
				throw new RegistrationException("Handler method must have parameter to receive event!");
			}

			final Class eventType = method.getParameters()[0].getType();
			final String packageName = eventType.getPackage().getName();
			final String eventName = eventType.getSimpleName();

			ReflectEventListenerHandler handler;
			if (method.getParameterCount() == 1) {
				handler = new ReflectEventListenerHandler(annotation.filter(), packageName, eventName, consumer, method);
			} else if (method.getParameterCount() == 2) {
				final Class sourPar = method.getParameters()[1].getType();
				if (!sourPar.equals(Object.class))
					throw new RegistrationException("Second parameter (event source) must be Object type.");
				handler = new ReflectEventSourceListenerHandler(annotation.filter(), packageName, eventName, consumer, method);
			} else
				throw new RegistrationException("Handler method must have exactly one parameter!");

			result.add(handler);
		}

		return result;
	}

}
