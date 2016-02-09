package tigase.eventbus;

public class EventBusException extends RuntimeException {
	public EventBusException() {
	}

	public EventBusException(String message) {
		super(message);
	}

	public EventBusException(String message, Throwable cause) {
		super(message, cause);
	}

	public EventBusException(Throwable cause) {
		super(cause);
	}

	public EventBusException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
