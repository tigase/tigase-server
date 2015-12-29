package tigase.disteventbus.local;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by bmalkow on 30.12.2015.
 */
public class ReflectEventListenerFactory {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	public Collection<ReflectEventHandler> create(final Object consumer) throws RegistrationException {
		ArrayList<ReflectEventHandler> result = new ArrayList<>();

		Method[] methods = consumer.getClass().getMethods();
		for (Method method : methods) {
			HandleEvent annotation = method.getAnnotation(HandleEvent.class);

			if (annotation == null)
				continue;

			if (method.getParameterCount() != 1) {
				throw new RegistrationException("Handler method must have exactly one parameter!");
			}

			final Class type = method.getParameters()[0].getType();

			if (!Event.class.isAssignableFrom(type)) {
				throw new RegistrationException("Handler method parameter must extends Event class.");
			}

			ReflectEventHandler listener = new ReflectEventHandler(consumer, method);
			result.add(listener);
		}

		return result;
	}

}
