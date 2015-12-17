package tigase.disteventbus.objbus;

import java.util.Collection;

/**
 * Exception collects all exceptions throwed by handlers or listeners during
 * firing event. This exception is usually throwed after calling all listeners
 * and handlers.
 */
public class EventBusException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final Collection<Throwable> causes;

	public EventBusException(Collection<Throwable> causes) {
		super(createMessage(causes), createThrowable(causes));
		this.causes = causes;
	}

	protected static String createMessage(Collection<Throwable> causes) {
		if (causes.isEmpty()) {
			return null;
		}

		StringBuilder b = new StringBuilder();

		int c = causes.size();
		if (c == 1) {
			b.append("Exception caught: ");
		} else {
			b.append(c).append(" exceptions caught: ");
		}

		boolean first = true;
		for (Throwable t : causes) {
			if (first) {
				first = false;
			} else {
				b.append("; ");
			}
			b.append(t.getMessage());
		}

		return b.toString();
	}

	protected static Throwable createThrowable(Collection<Throwable> causes) {
		if (causes.isEmpty()) {
			return null;
		}
		return causes.iterator().next();
	}

	/**
	 * Returns collection of all Exceptions throwed by handlers or listeners.
	 * 
	 * @return collection of Exceptions.
	 */
	public Collection<Throwable> getCauses() {
		return causes;
	}

}
