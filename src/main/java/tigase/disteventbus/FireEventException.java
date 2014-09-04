package tigase.disteventbus;

import java.util.Collection;

public class FireEventException extends DistEventBusException {

	private static final long serialVersionUID = 1L;

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

	private final Collection<Throwable> causes;

	public FireEventException(Collection<Throwable> causes) {
		super(createMessage(causes), createThrowable(causes));
		this.causes = causes;
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
