package tigase.disteventbus;

public class DistEventBusException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DistEventBusException() {
		super();
	}

	public DistEventBusException(String message) {
		super(message);
	}

	public DistEventBusException(String message, Throwable cause) {
		super(message, cause);
	}

	public DistEventBusException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DistEventBusException(Throwable cause) {
		super(cause);
	}

}
